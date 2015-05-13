package com.blogspot.geekabyte.krwler;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Jetty server used for HTTP integration test
 *
 * @author Dadepo Aderemi.
 */
public class TestServer {

    private final Logger logger = LoggerFactory.getLogger(TestServer.class);
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

    public Handler getMockHandler() {
        Handler handler = new AbstractHandler() {

            public void handle(String target, org.eclipse.jetty.server.Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException, ServletException {
                response.setContentType("text/html;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                baseRequest.setHandled(true);
                String content = getContent(target);
                if (String.valueOf(HttpServletResponse.SC_NOT_FOUND).equals(content)) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
                response.getWriter().println(content);
            }

            private String getContent(String filename) {
                byte[] contentAsBytes = null;
                String pathAsString;
                try {
                    URL pathAsUrl = this.getClass().getResource(filename);
                    if (pathAsUrl == null) {
                        pathAsString = "";
                    } else {
                        pathAsString = pathAsUrl.getPath();
                    }
                    contentAsBytes = Files.readAllBytes(Paths.get(pathAsString));
                } catch (IOException e) {
                    logger.error("Exception while reading {}", filename);
                    return String.valueOf(HttpServletResponse.SC_NOT_FOUND);
                }
                if (contentAsBytes != null) {
                    return new String(contentAsBytes, StandardCharsets.UTF_8);
                }
                return "";
            }
        };
        return handler;
    }

}
