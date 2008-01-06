package org.openrdf.alibaba.servlet.impl;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet access to import or export RDF/XML files into/out of the embeded
 * repository.
 * 
 * @author James Leigh
 * 
 */
public class ImportExportServlet extends HttpServlet {
	private static final String COMMA = "\\s*,\\s*";

	private static final String ACCEPT = "Accept";

	private static final String CONTENT_TYPE = "Content-Type";

	private static final RDFFormat DEFAULT_RDFFORMAT = RDFFormat.RDFXML;

	private static final long serialVersionUID = 8748199433111822326L;

	private Logger logger = LoggerFactory.getLogger(ImportExportServlet.class);

	private ServletConfigManagerFactory resources = new ServletConfigManagerFactory();

	@Override
	public void init(ServletConfig config) throws ServletException {
		logger.debug("init");
		resources.init(config);
	}

	@Override
	public void destroy() {
		logger.debug("destroy");
		resources.close();
	}

	@Override
	public String getServletInfo() {
		return "Ablibaba Import & Export";
	}

	@Override
	protected long getLastModified(HttpServletRequest req) {
		logger.debug("getLastModified");
		return System.currentTimeMillis();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		logger.debug("doGet");
		RDFFormat format = findRdfFormat(req.getHeader(ACCEPT));
		resp.addHeader(CONTENT_TYPE, format.getDefaultMIMEType());
		resp.addHeader("Content-disposition", "attachment; filename=alibaba."
				+ format.getDefaultFileExtension());
		try {
			Repository rep = resources.getRepository();
			RepositoryConnection conn = rep.getConnection();
			try {
				conn.export(Rio.createWriter(format, resp.getWriter()));
			} finally {
				conn.close();
			}
		} catch (RDFHandlerException e) {
			logger.warn(e.toString(), e);
			throw new ServletException(e);
		} catch (RepositoryException e) {
			logger.warn(e.toString(), e);
			throw new ServletException(e);
		}
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		logger.debug("doDelete");
		try {
			Repository rep = resources.getRepository();
			RepositoryConnection conn = rep.getConnection();
			try {
				conn.clear();
			} finally {
				conn.close();
			}
			resp.setStatus(204);
		} catch (RepositoryException e) {
			logger.warn(e.toString(), e);
			throw new ServletException(e);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		logger.debug("doPost");
		RDFFormat format = findRdfFormat(req.getHeader(CONTENT_TYPE));
		try {
			Repository rep = resources.getRepository();
			RepositoryConnection conn = rep.getConnection();
			try {
				conn.add(req.getReader(), "", format);
			} finally {
				conn.close();
			}
			resp.setStatus(204);
		} catch (RDFParseException e) {
			logger.warn(e.toString(), e);
			throw new ServletException(e);
		} catch (RepositoryException e) {
			logger.warn(e.toString(), e);
			throw new ServletException(e);
		}
	}

	private RDFFormat findRdfFormat(String accept) {
		if (accept == null)
			return DEFAULT_RDFFORMAT;
		for (String mimetype : accept.split(COMMA)) {
			if (mimetype.indexOf(';') > 0) {
				mimetype = mimetype.substring(0, mimetype.indexOf(';'));
			}
			RDFFormat type = RDFFormat.forMIMEType(mimetype, null);
			if (type != null)
				return type;
		}
		return DEFAULT_RDFFORMAT;
	}
}
