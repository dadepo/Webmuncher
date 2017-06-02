package com.blogspot.geekabyte.webmuncher;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Jetty server used for HTTP integration test
 *
 * @author Dadepo Aderemi.
 */
public class EndlessTestServer {

    private final Logger logger = LoggerFactory.getLogger(EndlessTestServer.class);
    public static final int HTTP_PORT = 50036;

    private Server server;

    public void start() throws Exception {
        server = new Server(HTTP_PORT);
        server.setHandler(getMockHandler());
        server.start();
    }

    public void shutDown() throws Exception {
        server.stop();
    }

    private String getRandomString() {
        return new BigInteger(130, new SecureRandom()).toString(32) + ".html";
    }

    public Handler getMockHandler() {
        Handler handler = new AbstractHandler() {

            public void handle(String target, org.eclipse.jetty.server.Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException, ServletException {
                response.setContentType("text/html;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                baseRequest.setHandled(true);
                String content = getContent(target);

                String page = content.replace("{{X}}", getRandomString()).replace("{{Y}}", getRandomString());
                response.getWriter().println(page);
            }

            private String getContent(String filename) {

                return "<html><head></head>" +
                        "<body>" +
                        "<a href='/{{X}}'></a>" +
                        "<a href='/{{Y}}'></a>" +
                        "</body>" +
                        "</html>";
            }
        };
        return handler;
    }

}
