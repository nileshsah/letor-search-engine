package com.nellex.api;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

/**
 * A light weight web container for extending the search engine as a REST end
 * point using Jetty
 * 
 * @author nellex
 */
public class SearchServer {
	public static void main(String[] args) throws Exception {
		Server server = new Server(7070);
		ServletContextHandler handler = new ServletContextHandler(server, "/search");
		handler.addServlet(SearchServlet.class, "/");
		server.start();
	}
}
