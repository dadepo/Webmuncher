package com.blogspot.geekabyte.krawkraw;

import com.blogspot.geekabyte.krawkraw.interfaces.KrawlerAction;
import com.blogspot.geekabyte.krawkraw.interfaces.callbacks.KrawlerExitCallback;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.runners.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    TestServer testServer;
    
    @Test
    public void test_extractAllFromUrl() throws Exception {
        KrawlerAction mockAction = mock(KrawlerAction.class);
        Krawkraw krawkrawSUT = new Krawkraw(mockAction);

        testServer = new TestServer();
        testServer.start();

        krawkrawSUT.setDelay(0);
        // System under test
        Set<String> hrefs = krawkrawSUT.doKrawl(host + "/mocksite/index.html");

        assertEquals(hrefs.size(), 6);
        verify(mockAction, times(6)).execute(any(FetchedPage.class));

        testServer.shutDown();
    }

    @Test
    public void test_extractAllFromUrl_Asyncronously() throws Exception {
        KrawlerAction mockAction = mock(KrawlerAction.class);
        Krawkraw krawkrawSUT = new Krawkraw(mockAction);

        testServer = new TestServer();
        testServer.start();

        krawkrawSUT.setDelay(0);
        // System under test
        Future<Set<String>> futureHrefs = krawkrawSUT.doKrawlAsync(host + "/mocksite/index.html");

        Set<String> hrefs = futureHrefs.get();

        assertEquals(hrefs.size(), 6);
        verify(mockAction, times(6)).execute(any(FetchedPage.class));

        testServer.shutDown();
    }
    
    @Test
    public void test_on_exit_callback() throws Exception {
        KrawlerAction mockAction = mock(KrawlerAction.class);
        KrawlerExitCallback mockCallBack = mock(KrawlerExitCallback.class);
        Krawkraw krawkrawSUT = new Krawkraw(mockAction);
        
        testServer = new TestServer();
        testServer.start();

        krawkrawSUT.onExit(mockCallBack);
        krawkrawSUT.setDelay(0);
        // System under test
        Future<Set<String>> futureHrefs = krawkrawSUT.doKrawlAsync(host + "/mocksite/index.html");

        Set<String> hrefs = futureHrefs.get();

        assertEquals(hrefs.size(), 6);
        verify(mockAction, times(6)).execute(any(FetchedPage.class));
        verify(mockCallBack).callBack(anySet());

        testServer.shutDown();
        
    }
    
    @Test
    public void test_extractBrokenLink() throws Exception {
        KrawlerAction mockAction = mock(KrawlerAction.class);
        Krawkraw krawkrawSUT = new Krawkraw(mockAction);

        testServer = new TestServer();
        testServer.start();

        krawkrawSUT.setDelay(0);
        // System under test
        Set<String> hrefs = krawkrawSUT.doKrawl(host + "/brokenlink/index.html");

        ArgumentCaptor<FetchedPage> captor = ArgumentCaptor.forClass(FetchedPage.class);
        assertEquals(hrefs.size(), 5);
        verify(mockAction, times(5)).execute(captor.capture());
        List<FetchedPage> fetchedPages = captor.getAllValues();
        
        int notFoundCount = 0;
        for (FetchedPage page: fetchedPages) {
            if (page.getStatus() == 404) {
                notFoundCount++;
            }
        }
        assertEquals(notFoundCount, 3);
        testServer.shutDown();
    }

    //==================================================== Helpers ====================================================

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

}