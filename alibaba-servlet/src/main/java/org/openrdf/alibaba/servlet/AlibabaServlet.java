package org.openrdf.alibaba.servlet;

import info.aduna.platform.Platform;
import info.aduna.platform.PlatformFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.exceptions.BadRequestException;
import org.openrdf.alibaba.exceptions.MethodNotAllowedException;
import org.openrdf.alibaba.exceptions.NotAcceptableException;
import org.openrdf.alibaba.exceptions.NotFoundException;
import org.openrdf.alibaba.exceptions.UnsupportedMediaTypeException;
import org.openrdf.alibaba.servlet.impl.AlibabaStateManager;
import org.openrdf.alibaba.servlet.impl.HttpContent;
import org.openrdf.alibaba.servlet.impl.HttpResponse;
import org.openrdf.alibaba.vocabulary.ALI;
import org.openrdf.elmo.sesame.SesameManagerFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryConfigUtil;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.repository.realiser.StatementRealiserRepository;
import org.openrdf.repository.sail.config.SailRepositoryConfig;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.config.MemoryStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlibabaServlet extends HttpServlet {
	private static final String POVS_PROPERTIES = "META-INF/org.openrdf.alibaba.povs";

	private static final String DECORS_PROPERTIES = "META-INF/org.openrdf.alibaba.decors";

	private static final String ALI_PREFIX = "ali";

	private final Logger logger = LoggerFactory.getLogger(AlibabaServlet.class);

	private static final long serialVersionUID = 8748199433111822326L;

	private StateManager manager;

	private Repository repository;

	public void setStateManager(StateManager manager) {
		this.manager = manager;
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		try {
			String appId = config.getInitParameter("applicationId");
			String dataDir = config.getInitParameter("dataDir");
			String id = config.getInitParameter("repositoryId");
			String initData = config.getInitParameter("initData");
			repository = getRepository(getDataDir(dataDir, appId), id);
			repository = new StatementRealiserRepository(repository);
			initRepository(repository, initData);
			AlibabaStateManager manager = new AlibabaStateManager();
			SesameManagerFactory factory = new SesameManagerFactory(repository);
			manager.setElmoManagerFactory(factory);
			setStateManager(manager);
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	@Override
	public void destroy() {
		try {
			if (repository != null)
				repository.shutDown();
		} catch (RepositoryException e) {
			throw new UndeclaredThrowableException(e);
		}
	}

	@Override
	public String getServletInfo() {
		return "Ablibaba";
	}

	@Override
	protected long getLastModified(HttpServletRequest req) {
		QName resource = getResource(req);
		return manager.getLastModified(resource);
	}

	/**
	 * Retrives resources, resource templates, search templates, and search
	 * results.
	 * 
	 * <pre>
	 *  &gt;&gt; GET $contextPath(/$prefix/$intention)?/$prefix/$resource(\?$bindings)?
	 *  &gt;&gt; Accept : $contentType
	 *  &gt;&gt; Accept-Language : $language
	 *  &lt;&lt; HTTP/1.1 200 OK
	 *  &lt;&lt; Content-Type : $contentType
	 *  &lt;&lt; Content-Language : $language
	 * </pre>
	 * 
	 * @param contentPath
	 *            The matching path prefix for this servlet.
	 * @param intention
	 *            Optional parameter to identify what template the resource
	 *            should be returned in.
	 * @param resource
	 *            The resource to be returned.
	 * @param bindings
	 *            If the resource is an instance of serqo:Query and bindings are
	 *            provided the results of the query are returned (TODO).
	 * @param contentType
	 *            The desired contentType of the result.
	 * @param language
	 *            The desired language of the result.
	 * @throws NotFoundException
	 * @throws NotAcceptableException
	 * @throws BadRequestException
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		QName resource = getResource(req);
		QName intention = getIntention(getPathInfo(req));
		Response response = new HttpResponse(req, resp);
		try {
			manager.retrieve(resource, response, intention);
		} catch (AlibabaException e) {
			throw new ServletException(e);
		}
	}

	/**
	 * Removes a resource of the repository.
	 * 
	 * <pre>
	 *  &gt;&gt; DELETE $contextPath/$prefix/$resource
	 *  &lt;&lt; HTTP/1.1 204 OK
	 * </pre>
	 * 
	 * @param contentPath
	 *            The matching path prefix for this servlet.
	 * @param resource
	 *            The resource to be removed.
	 * @throws NotFoundException
	 */
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException {
		QName resource = getResource(req);
		try {
			manager.remove(resource);
		} catch (AlibabaException e) {
			throw new ServletException(e);
		}
		resp.setStatus(204);
	}

	/**
	 * Creates a new resource or assigns a new type to an existing resource.
	 * 
	 * <pre>
	 *  &gt;&gt; POST $contextPath(/$prefix/$intention)?/$prefix/$type
	 *  &gt;&gt; Content-Type : $contentType
	 *  &gt;&gt; Content-Language : $language
	 *  &gt;&gt; ( Content-Location : $contextPath/$prefix/$resource )?
	 *  &lt;&lt; HTTP/1.1 201 Created
	 *  &lt;&lt; Location : $contextPath/$prefix/$resource
	 * </pre>
	 * 
	 * @param contentPath
	 *            The matching path prefix for this servlet.
	 * @param intention
	 *            Used to identify what template the content is in.
	 * @param type
	 *            The role or type of the resource being created.
	 * @param resource
	 *            The resource uri that is to be created and was created.
	 * @param contentType
	 *            The contentType of the provided resource content.
	 * @param language
	 *            The language of the provided resource content.
	 * @throws MethodNotAllowedException
	 * @throws NotFoundException
	 * @throws UnsupportedMediaTypeException
	 * @throws BadRequestException
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		QName resource = getResource(req.getHeader("Content-Location"));
		QName type = getResource(req);
		QName intention = getIntention(getPathInfo(req));
		Content source = new HttpContent(req);
		try {
			resource = manager.create(resource, type, source, intention);
		} catch (AlibabaException e) {
			throw new ServletException(e);
		}
		resp.setStatus(201);
		resp.setHeader("Location", getURL(resource, intention, req));
	}

	/**
	 * Saves the resource properties back into the repository.
	 * 
	 * <pre>
	 *  &gt;&gt; PUT $contextPath(/$prefix/$intention)?/$prefix/$resource
	 *  &gt;&gt; Content-Type : $contentType
	 *  &gt;&gt; Content-Language : $language
	 *  &lt;&lt; HTTP/1.1 204 OK
	 * </pre>
	 * 
	 * @param contentPath
	 *            The matching path prefix for this servlet.
	 * @param intention
	 *            Used to identify what template the content is in.
	 * @param resource
	 *            The resource uri that is being updated.
	 * @param contentType
	 *            The contentType of the provided resource content.
	 * @param language
	 *            The language of the provided resource content.
	 * @throws MethodNotAllowedException
	 * @throws NotFoundException
	 * @throws UnsupportedMediaTypeException
	 * @throws BadRequestException
	 */
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		QName resource = getResource(req);
		QName intention = getIntention(getPathInfo(req));
		Content source = new HttpContent(req);
		try {
			manager.save(resource, source, intention);
		} catch (AlibabaException e) {
			throw new ServletException(e);
		}
		resp.setStatus(204);
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		try {
			super.service(req, resp);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			PrintWriter writer = resp.getWriter();
			if (e.getCause() instanceof AlibabaException) {
				AlibabaException ae = (AlibabaException) e.getCause();
				resp.sendError(ae.getErrorCode(), ae.getMessage());
				resp.setContentType(ae.getContentType());
				ae.printContent(writer);
			} else {
				resp.sendError(500, e.getMessage());
				resp.setContentType("text/plan");
				e.printStackTrace(writer);
			}
			writer.flush();
		}
	}

	private Repository getRepository(File dataDir, String id)
			throws RepositoryException, RepositoryConfigException {
		logger.info("Using data dir: {}", dataDir);
		assert id != null;
		RepositoryManager manager = new LocalRepositoryManager(dataDir);
		manager.initialize();
		Repository repository = manager.getRepository(id);
		if (repository == null) {
			logger.warn("Creating repository configuration for: {}", id);
			MemoryStoreConfig memConfig = new MemoryStoreConfig();
			SailRepositoryConfig sailConfig = new SailRepositoryConfig(
					memConfig);
			RepositoryConfig config = new RepositoryConfig(id, sailConfig);
			Repository system = manager.getSystemRepository();
			RepositoryConfigUtil.updateRepositoryConfigs(system, config);
			repository = manager.getRepository(id);
		}
		return repository;
	}

	private void initRepository(Repository repository, String initData)
			throws RepositoryException, MalformedURLException, IOException,
			RDFParseException {
		RepositoryConnection conn = repository.getConnection();
		try {
			if (conn.isEmpty()) {
				conn.setNamespace(ALI_PREFIX, ALI.NS);
				ClassLoader cl = Thread.currentThread().getContextClassLoader();
				if (initData != null) {
					for (String file : initData.split(";")) {
						URL url;
						if (new File(file).exists()) {
							url = new File(file).toURL();
						} else {
							url = cl.getResource(file);
						}
						RDFFormat format = RDFFormat.forFileName(url.getFile());
						conn.add(url, "", format);
					}
				}
				loadPropertyKeysAsResource(conn, cl, POVS_PROPERTIES);
				loadPropertyKeysAsResource(conn, cl, DECORS_PROPERTIES);
			}
		} finally {
			conn.close();
		}
	}

	private void loadPropertyKeysAsResource(RepositoryConnection conn,
			ClassLoader cl, String listing) throws IOException,
			RDFParseException, RepositoryException {
		Enumeration<URL> list = cl.getResources(listing);
		while (list.hasMoreElements()) {
			Properties prop = new Properties();
			prop.load(list.nextElement().openStream());
			for (Object res : prop.keySet()) {
				URL url = cl.getResource(res.toString());
				RDFFormat format = RDFFormat.forFileName(url.getFile());
				conn.add(url, "", format);
			}
		}
	}

	private File getDataDir(String dataDir, String appId) {
		if (dataDir == null) {
			assert appId != null;
			Platform platform = PlatformFactory.getPlatform();
			return platform.getApplicationDataDir(appId);
		}
		return new File(dataDir);
	}

	private String getPathInfo(HttpServletRequest req) {
		String pathInfo = req.getPathInfo();
		if (pathInfo == null)
			return req.getServletPath();
		return pathInfo;
	}

	private QName getIntention(String path) {
		if (path == null)
			return null;
		int end = path.lastIndexOf('/');
		end = path.lastIndexOf('/', end - 1);
		int split = path.lastIndexOf('/', end - 1);
		if (split < 0)
			return null;
		int start = path.lastIndexOf('/', split - 1);
		String localPart = path.substring(split + 1, end);
		String prefix = path.substring(start + 1, split);
		return new QName(null, localPart, prefix);
	}

	private QName getResource(HttpServletRequest req) {
		if (req.getParameter("uri") != null) {
			return new QName(req.getParameter("uri"));
		}
		return getResource(getPathInfo(req));
	}

	private QName getResource(String path) {
		if (path == null)
			return null;
		int split = path.lastIndexOf('/');
		int start = path.lastIndexOf('/', split - 1);
		String localPart = path.substring(split + 1);
		String prefix = path.substring(start + 1, split);
		return new QName(null, localPart, prefix);
	}

	private String getURL(QName resource, QName intent, HttpServletRequest req) {
		StringBuffer url = req.getRequestURL();
		url.setLength(url.length() - getPathInfo(req).length());
		if (intent != null) {
			url.append('/').append(intent.getPrefix());
			url.append('/').append(intent.getLocalPart());
		}
		url.append('/').append(resource.getPrefix());
		url.append('/').append(resource.getLocalPart());
		return url.toString();
	}

}
