package org.openrdf.alibaba.servlet.impl;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.openrdf.alibaba.decor.Content;
import org.openrdf.alibaba.decor.PresentationService;
import org.openrdf.alibaba.decor.UrlResolver;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.exceptions.BadRequestException;
import org.openrdf.alibaba.exceptions.MethodNotAllowedException;
import org.openrdf.alibaba.exceptions.NotAcceptableException;
import org.openrdf.alibaba.exceptions.NotFoundException;
import org.openrdf.alibaba.exceptions.UnsupportedMediaTypeException;
import org.openrdf.alibaba.pov.Intent;
import org.openrdf.alibaba.servlet.PresentationManager;
import org.openrdf.concepts.rdfs.Class;
import org.openrdf.elmo.ElmoManager;
import org.openrdf.elmo.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlibabaServlet extends HttpServlet {
	private static final String RDF_PROTOCOL_HEADER = "X-RdfProtocol";

	private static final String INTENT_PARAMETER = "intent";

	private static final String RESOURCE_PARAMETER = "uri";

	private static final long serialVersionUID = 8748199433111822326L;

	private Logger logger = LoggerFactory.getLogger(AlibabaServlet.class);

	private ServletConfigManagerFactory resources = new ServletConfigManagerFactory();

	private PresentationManagerFactoryImpl presentation = new PresentationManagerFactoryImpl();

	@Override
	public void init(ServletConfig config) throws ServletException {
		logger.debug("init");
		resources.init(config);
		presentation.initialize();
	}

	@Override
	public void destroy() {
		logger.debug("destroy");
		presentation.close();
		resources.close();
	}

	@Override
	public String getServletInfo() {
		return "Ablibaba";
	}

	@Override
	protected long getLastModified(HttpServletRequest req) {
		QName resource = getResource(req);
		logger.debug("getLastModified {}", resource);
		PresentationManager pmgr = presentation.createManager(getLocale(req));
		ElmoManager manager = resources.createElmoManager(getLocale(req));
		manager.getTransaction().begin();
		try {
			PresentationService service = pmgr.findPresentationService();
			return service.getLastModified(manager.find(resource), null);
		} finally {
			pmgr.close();
			manager.getTransaction().commit();
			manager.close();
		}
	}

	/**
	 * Retrieves resources, resource templates, search templates, and search
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
		QName intent = getIntent(req);
		logger.debug("doGet {}", resource);
		HttpResponse response = new HttpResponse(req, resp);
		response.setUrlResolver(createUrlResolver(req, intent));
		PresentationManager pmgr = presentation.createManager(getLocale(req));
		ElmoManager manager = resources.createElmoManager(getLocale(req));
		manager.getTransaction().begin();
		try {
			PresentationService service = pmgr.findPresentationService();
			Intent intention = pmgr.findIntent(intent);
			service.retrieve(manager.find(resource), response, intention);
		} catch (AlibabaException e) {
			handleAlibabaException(e, resp);
		} finally {
			pmgr.close();
			manager.getTransaction().commit();
			manager.close();
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
	 * @throws IOException
	 * @throws NotFoundException
	 */
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		QName resource = getResource(req);
		logger.debug("doDelete {}", resource);
		PresentationManager pmgr = presentation.createManager(getLocale(req));
		ElmoManager manager = resources.createElmoManager(getLocale(req));
		manager.getTransaction().begin();
		try {
			PresentationService service = pmgr.findPresentationService();
			service.remove(manager.find(resource));
			resp.setStatus(204);
		} catch (AlibabaException e) {
			handleAlibabaException(e, resp);
		} finally {
			pmgr.close();
			manager.getTransaction().commit();
			manager.close();
		}
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
		QName intent = getIntent(req);
		logger.debug("doPost {}", resource);
		Content source = new HttpContent(req);
		PresentationManager pmgr = presentation.createManager(getLocale(req));
		ElmoManager manager = resources.createElmoManager(getLocale(req));
		manager.getTransaction().begin();
		try {
			PresentationService service = pmgr.findPresentationService();
			Intent intention = pmgr.findIntent(intent);
			Class ctype = pmgr.findClass(type);
			Entity target = manager.find(resource);
			resource = service.create(target, ctype, source, intention);
			resp.setStatus(201);
			resp.setHeader("Location", getURL(resource, intent, req));
		} catch (AlibabaException e) {
			handleAlibabaException(e, resp);
		} finally {
			pmgr.close();
			manager.getTransaction().commit();
			manager.close();
		}
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
		QName intent = getIntent(req);
		logger.debug("doPut {}", resource);
		Content source = new HttpContent(req);
		PresentationManager pmgr = presentation.createManager(getLocale(req));
		ElmoManager manager = resources.createElmoManager(getLocale(req));
		manager.getTransaction().begin();
		try {
			PresentationService service = pmgr.findPresentationService();
			Intent intention = pmgr.findIntent(intent);
			service.save(manager.find(resource), source, intention);
			resp.setStatus(204);
		} catch (AlibabaException e) {
			handleAlibabaException(e, resp);
		} finally {
			pmgr.close();
			manager.getTransaction().commit();
			manager.close();
		}
	}

	private void handleAlibabaException(AlibabaException ae,
			HttpServletResponse resp) throws IOException {
		resp.sendError(ae.getErrorCode(), ae.getMessage());
		resp.setContentType(ae.getContentType());
		ae.printContent(resp.getWriter());
	}

	private Locale getLocale(HttpServletRequest req) {
		Locale locale = req.getLocale();
		return new Locale(locale.getLanguage());
	}

	private String getPathInfo(HttpServletRequest req) {
		String pathInfo = req.getPathInfo();
		if (pathInfo == null)
			return req.getServletPath();
		return pathInfo;
	}

	private QName getIntent(HttpServletRequest req) {
		if (req.getParameter(INTENT_PARAMETER) != null) {
			return new QName(req.getParameter(INTENT_PARAMETER));
		}
		return getIntention(getPathInfo(req));
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
		if (req.getParameter(RESOURCE_PARAMETER) != null) {
			return new QName(req.getParameter(RESOURCE_PARAMETER));
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

	private UrlResolver createUrlResolver(HttpServletRequest req, QName intent) {
		String header = req.getHeader(RDF_PROTOCOL_HEADER);
		String path = req.getContextPath() + req.getServletPath();
		return new HttpUrlResolver(Boolean.parseBoolean(header), path, intent);
	}
}
