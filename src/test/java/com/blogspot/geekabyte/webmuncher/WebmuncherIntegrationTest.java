package com.blogspot.geekabyte.webmuncher;

import com.blogspot.geekabyte.webmuncher.interfaces.FetchAction;
import com.blogspot.geekabyte.webmuncher.interfaces.callbacks.FetchExitCallback;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.runners.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WebmuncherIntegrationTest {

    private final String host = "http://localhost:" + TestServer.HTTP_PORT;
    TestServer testServer;

    @Before
    public void  startServer() throws Exception {
        testServer = new TestServer();
        testServer.start();
    }

    @After
    public void shutDownServer() throws Exception {
        testServer.shutDown();
    }

    @Test
    public void testBuilder() throws Exception {
        Webmuncher webmuncher = Webmuncher.newBuilder()
                .withDelayInBetweenRequest(2)
                .withFetchAction(mock(FetchAction.class))
                .withExcludePattern(Collections.EMPTY_SET)
                .withExcludeUrl(Collections.EMPTY_SET)
                .withExitCallBack(mock(FetchExitCallback.class))
                .withMatchPattern(Collections.EMPTY_SET)
                .withMaxRetry(3)
                .withReferrals(Collections.EMPTY_LIST)
                .withRequestTimeOut(4)
                .withUserAgents(Collections.EMPTY_LIST)
                .build();

        assertEquals(webmuncher.getDelay(), 2);
        assertEquals(webmuncher.getExcludeURLs(), Collections.EMPTY_SET);
        assertEquals(webmuncher.getReferrals(), Collections.EMPTY_LIST);
        assertEquals(webmuncher.getUserAgents(), Collections.EMPTY_LIST);
    }


    @Test
    public void test_extractAllFromUrl() throws Exception {
        FetchAction mockAction = mock(FetchAction.class);
        Webmuncher webmuncherSUT = new Webmuncher(mockAction);

        webmuncherSUT.setDelay(0);
        // System under test
        Set<String> hrefs = webmuncherSUT.crawl(host + "/mocksite/index.html");

        assertEquals(hrefs.size(), 6);
        verify(mockAction, times(6)).process(any(FetchedPage.class));
    }

    @Test
    public void test_extractAllFromUrl_exclude_url() throws Exception {
        FetchAction mockAction = mock(FetchAction.class);
        Webmuncher webmuncherSUT = new Webmuncher(mockAction);
        Set<String> urlToExclude = new HashSet<>();
        urlToExclude.add("http://localhost:50036/mocksitetestexclude/three.html");
        urlToExclude.add("http://localhost:50036/mocksitetestexclude/one.html");

        webmuncherSUT.setDelay(0);
        webmuncherSUT.setExcludeURLs(urlToExclude);
        // System under test
        Set<String> hrefs = webmuncherSUT.crawl(host + "/mocksitetestexclude/index.html");

        assertEquals(hrefs.size(), 2);
        verify(mockAction, times(2)).process(any(FetchedPage.class));
    }

    @Test
    public void test_extractAllFromUrl_exclude_url_via_varags() throws Exception {
        FetchAction mockAction = mock(FetchAction.class);
        Webmuncher webmuncherSUT = new Webmuncher(mockAction);

        webmuncherSUT.setDelay(0);
        webmuncherSUT.setExcludeURLs("http://localhost:50036/mocksitetestexclude/three.html",
                                 "http://localhost:50036/mocksitetestexclude/one.html");
        // System under test
        Set<String> hrefs = webmuncherSUT.crawl(host + "/mocksitetestexclude/index.html");

        assertEquals(hrefs.size(), 2);
        verify(mockAction, times(2)).process(any(FetchedPage.class));
    }

    @Test
    public void test_extractAllFromUrl_Asynchronously() throws Exception {
        FetchAction mockAction = mock(FetchAction.class);
        Webmuncher webmuncherSUT = new Webmuncher(mockAction);

        webmuncherSUT.setDelay(0);
        // System under test
        Future<Set<String>> futureHrefs = webmuncherSUT.crawlAsync(host + "/mocksite/index.html");

        Set<String> hrefs = futureHrefs.get();

        assertEquals(hrefs.size(), 6);
        verify(mockAction, times(6)).process(any(FetchedPage.class));

    }
    
    @Test
    public void test_on_exit_callback() throws Exception {
        FetchAction mockAction = mock(FetchAction.class);
        FetchExitCallback mockCallBack = mock(FetchExitCallback.class);
        Webmuncher webmuncherSUT = new Webmuncher(mockAction);

        webmuncherSUT.onExit(mockCallBack);
        webmuncherSUT.setDelay(0);
        // System under test
        Future<Set<String>> futureHrefs = webmuncherSUT.crawlAsync(host + "/mocksite/index.html");

        Set<String> hrefs = futureHrefs.get();

        assertEquals(hrefs.size(), 6);
        verify(mockAction, times(6)).process(any(FetchedPage.class));
        verify(mockCallBack).callBack(anySet());

    }
    
    @Test
    public void test_extractBrokenLink() throws Exception {
        FetchAction mockAction = mock(FetchAction.class);
        Webmuncher webmuncherSUT = new Webmuncher(mockAction);

        webmuncherSUT.setDelay(0);
        // System under test
        Set<String> hrefs = webmuncherSUT.crawl(host + "/brokenlink/index.html");

        ArgumentCaptor<FetchedPage> captor = ArgumentCaptor.forClass(FetchedPage.class);
        assertEquals(hrefs.size(), 5);
        verify(mockAction, times(5)).process(captor.capture());
        List<FetchedPage> fetchedPages = captor.getAllValues();
        
        int notFoundCount = 0;
        for (FetchedPage page: fetchedPages) {
            if (page.getStatus() == 404) {
                notFoundCount++;
            }
        }
        assertEquals(notFoundCount, 3);
    }

    @Test
    public void test_pattern_exclude_all_using_hashset_api() throws Exception {
        /**
         * Available paths
         * /mocksitetestexclude/path/index.html
         * /mocksitetestexclude/path/one/one.html
         * /mocksitetestexclude/path/one/two/two.html
         * /mocksitetestexclude/path/one/two/three/three.html
         * /mocksitetestexclude/path/one/two/three/four/four.html
         *
         **/
        FetchAction mockAction = mock(FetchAction.class);
        Webmuncher webmuncherSUT = new Webmuncher(mockAction);

        Set<String> urlPatternToExclude = new HashSet<>();
        urlPatternToExclude.add(".*");

        webmuncherSUT.skip(urlPatternToExclude);
        webmuncherSUT.setDelay(0);

        // System under test
        Set<String> hrefs = webmuncherSUT.crawl(host + "/mocksitetestexclude/path/index.html");

        assertEquals(hrefs.size(), 0);
    }

    @Test
    public void test_pattern_exclude_all_using_varags_api() throws Exception {
        /**
         * Available paths
         * /mocksitetestexclude/path/index.html
         * /mocksitetestexclude/path/one/one.html
         * /mocksitetestexclude/path/one/two/two.html
         * /mocksitetestexclude/path/one/two/three/three.html
         * /mocksitetestexclude/path/one/two/three/four/four.html
         *
         **/
        FetchAction mockAction = mock(FetchAction.class);
        Webmuncher webmuncherSUT = new Webmuncher(mockAction);

        webmuncherSUT.skip(".*");
        webmuncherSUT.setDelay(0);

        // System under test
        Set<String> hrefs = webmuncherSUT.crawl(host + "/mocksitetestexclude/path/index.html");

        assertEquals(hrefs.size(), 0);
    }

    @Test
    public void test_pattern_exclude_all_under_a_path_using_varags_api() throws Exception {
        /**
         * Available paths
         * /mocksitetestexclude/path/index.html
         * /mocksitetestexclude/path/one/one.html
         * /mocksitetestexclude/path/one/two/two.html
         * /mocksitetestexclude/path/one/two/three/three.html
         * /mocksitetestexclude/path/one/two/three/four/four.html
         *
         **/
        FetchAction mockAction = mock(FetchAction.class);
        Webmuncher webmuncherSUT = new Webmuncher(mockAction);

        webmuncherSUT.skip("\\S+(three.html|four.html)"); // omits three.html and four.html
        webmuncherSUT.setDelay(0);

        // System under test
        Set<String> hrefs = webmuncherSUT.crawl(host + "/mocksitetestexclude/path/index.html");

        assertEquals(hrefs.size(), 3);
        assertTrue(!hrefs.contains(host + "/mocksitetestexclude/path/one/two/three/three.html"));
        assertTrue(!hrefs.contains(host + "/mocksitetestexclude/path/one/two/three/four/four.html"));
    }

    @Test
    public void test_pattern_crawl_include_all_using_varags_api() throws Exception {
        /**
         * Available paths
         * /mocksitetestexclude/path/index.html
         * /mocksitetestexclude/path/one/one.html
         * /mocksitetestexclude/path/one/two/two.html
         * /mocksitetestexclude/path/one/two/three/three.html
         * /mocksitetestexclude/path/one/two/three/four/four.html
         *
         **/
        FetchAction mockAction = mock(FetchAction.class);
        Webmuncher webmuncherSUT = new Webmuncher(mockAction);

        webmuncherSUT.match("\\S+(\\.html)");
        webmuncherSUT.setDelay(0);

        // System under test
        Set<String> hrefs = webmuncherSUT.crawl(host + "/mocksitetestexclude/path/index.html");

        assertEquals(hrefs.size(), 5);
        assertTrue(hrefs.contains(host + "/mocksitetestexclude/path/index.html"));
        assertTrue(hrefs.contains(host + "/mocksitetestexclude/path/one/one.html"));
        assertTrue(hrefs.contains(host + "/mocksitetestexclude/path/one/two/two.html"));
        assertTrue(hrefs.contains(host + "/mocksitetestexclude/path/one/two/three/three.html"));
        assertTrue(hrefs.contains(host + "/mocksitetestexclude/path/one/two/three/four/four.html"));
    }


    @Test
    public void test_pattern_crawl_include_and_exclude_using_varags_api() throws Exception {
        /**
         * Available paths
         * /mocksitetestexclude/path/index.html
         * /mocksitetestexclude/path/one/one.html
         * /mocksitetestexclude/path/one/two/two.html
         * /mocksitetestexclude/path/one/two/three/three.html
         * /mocksitetestexclude/path/one/two/three/four/four.html
         *
         **/
        FetchAction mockAction = mock(FetchAction.class);
        Webmuncher webmuncherSUT = new Webmuncher(mockAction);

        webmuncherSUT.match("\\S+(\\.html)"); // include all
        webmuncherSUT.skip("\\S+(three.html)", "\\S+(four.html)"); // omits three.html and four.html
        webmuncherSUT.setDelay(0);

        // System under test
        Set<String> hrefs = webmuncherSUT.crawl(host + "/mocksitetestexclude/path/index.html");

        assertEquals(hrefs.size(), 3);
        assertTrue(hrefs.contains(host + "/mocksitetestexclude/path/index.html"));
        assertTrue(hrefs.contains(host + "/mocksitetestexclude/path/one/one.html"));
        assertTrue(hrefs.contains(host + "/mocksitetestexclude/path/one/two/two.html"));
        assertTrue(!hrefs.contains(host + "/mocksitetestexclude/path/one/two/three/three.html"));
        assertTrue(!hrefs.contains(host + "/mocksitetestexclude/path/one/two/three/four/four.html"));
    }

    @Test
    public void test_pattern_add_all_exclude_all_using_varags_api() throws Exception {
        /**
         * Available paths
         * /mocksitetestexclude/path/index.html
         * /mocksitetestexclude/path/one/one.html
         * /mocksitetestexclude/path/one/two/two.html
         * /mocksitetestexclude/path/one/two/three/three.html
         * /mocksitetestexclude/path/one/two/three/four/four.html
         *
         **/
        FetchAction mockAction = mock(FetchAction.class);
        Webmuncher webmuncherSUT = new Webmuncher(mockAction);

        webmuncherSUT.match("\\S+(\\.html)");
        webmuncherSUT.skip("\\S+(\\.html)");
        webmuncherSUT.setDelay(0);

        // System under test
        Set<String> hrefs = webmuncherSUT.crawl(host + "/mocksitetestexclude/path/index.html");
        assertEquals(hrefs.size(), 0);
    }
}