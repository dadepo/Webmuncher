package com.blogspot.geekabyte.krawkraw;

import com.blogspot.geekabyte.krawkraw.interfaces.KrawlerAction;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.runners.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class KrawkrawTest {

    private final Logger logger = LoggerFactory.getLogger(Krawkraw.class);
    private final String host = "http://localhost:" + TestServer.HTTP_PORT;

    Krawkraw krawkrawSUT = new Krawkraw();
    TestServer testServer;

    @Test
    public void test_extractAbsHref() {
        Document doc = createDocument("fivelinks");
        // System under test
        List<String> hrefs = krawkrawSUT.extractAbsHref(doc);
        assertEquals(hrefs.size(), 5);
    }

    @Test
    public void test_extractHref() {
        Document doc = createDocument("fivelinks");
        // System under test
        List<String> hrefs = krawkrawSUT.extractHref(doc);
        assertEquals(hrefs.size(), 5);
    }

    @Test
    public void test_extractAllFromUrl() throws Exception {

        testServer = new TestServer();
        testServer.start();

        KrawlerAction mockAction = mock(KrawlerAction.class);

        krawkrawSUT.setAction(mockAction);
        krawkrawSUT.setBaseUrl("localhost");
        krawkrawSUT.setDelay(0);
        // System under test
        Set<String> hrefs = krawkrawSUT.doKrawl(host + "/mocksite/index.html");

        assertEquals(hrefs.size(), 6);
        verify(mockAction, times(6)).execute(any(FetchedPage.class));

        testServer.shutDown();
    }

    @Test
    public void test_extractAllFromUrl_Asyncronously() throws Exception {

        testServer = new TestServer();
        testServer.start();

        KrawlerAction mockAction = mock(KrawlerAction.class);

        krawkrawSUT.setAction(mockAction);
        krawkrawSUT.setBaseUrl("localhost");
        krawkrawSUT.setDelay(0);
        krawkrawSUT.initializeAsync();
        // System under test
        Future<Set<String>> futureHrefs = krawkrawSUT.doKrawlAsync(host + "/mocksite/index.html");

        Set<String> hrefs = futureHrefs.get();
        krawkrawSUT.destroyAsync();

        assertEquals(hrefs.size(), 6);
        verify(mockAction, times(6)).execute(any(FetchedPage.class));

        testServer.shutDown();
    }


    //==================================================== Helpers ====================================================


    private Document createDocument(String type) {


        switch (type) {
            case ("fivelinks"): {
                URL path = this.getClass().getResource("/fivelinks.html");
                File file = new File(path.getFile());
                Document doc = null;
                try {
                    doc = Jsoup.parse(file, "UTF-8");
                } catch (IOException e) {
                    logger.error("Error with file {}", file);
                }

                return doc;
            }
            default: {
                return null;
            }
        }
    }

    private class TestServer {

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
                    response.getWriter().println(getContent(target));
                }


                private String getContent(String filename) {
                    byte[] contentAsBytes = null;
                    try {
                        URL pathAsUrl = this.getClass().getResource(filename);
                        String pathAsString = pathAsUrl.getPath();

                        contentAsBytes = Files.readAllBytes(Paths.get(pathAsString));
                    } catch (IOException e) {
                        logger.error("Exception while reading {}", filename);
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
}