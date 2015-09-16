package com.blogspot.geekabyte.krwler;

import com.blogspot.geekabyte.krwler.interfaces.KrwlerAction;
import com.blogspot.geekabyte.krwler.interfaces.callbacks.KrwlerExitCallback;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.runners.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class KrwkrwIntegrationTest {

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
    public void test_extractAllFromUrl() throws Exception {
        KrwlerAction mockAction = mock(KrwlerAction.class);
        Krwkrw krwkrwSUT = new Krwkrw(mockAction);

        krwkrwSUT.setDelay(0);
        // System under test
        Set<String> hrefs = krwkrwSUT.crawl(host + "/mocksite/index.html");

        assertEquals(hrefs.size(), 6);
        verify(mockAction, times(6)).execute(any(FetchedPage.class));
    }

    @Test
    public void test_extractAllFromUrl_exclude_url() throws Exception {
        KrwlerAction mockAction = mock(KrwlerAction.class);
        Krwkrw krwkrwSUT = new Krwkrw(mockAction);
        Set<String> urlToExclude = new HashSet<>();
        urlToExclude.add("http://localhost:50036/mocksitetestexclude/three.html");
        urlToExclude.add("http://localhost:50036/mocksitetestexclude/one.html");

        krwkrwSUT.setDelay(0);
        krwkrwSUT.setExcludeURLs(urlToExclude);
        // System under test
        Set<String> hrefs = krwkrwSUT.crawl(host + "/mocksitetestexclude/index.html");

        assertEquals(hrefs.size(), 2);
        verify(mockAction, times(2)).execute(any(FetchedPage.class));
    }

    @Test
    public void test_extractAllFromUrl_exclude_url_via_varags() throws Exception {
        KrwlerAction mockAction = mock(KrwlerAction.class);
        Krwkrw krwkrwSUT = new Krwkrw(mockAction);

        krwkrwSUT.setDelay(0);
        krwkrwSUT.setExcludeURLs("http://localhost:50036/mocksitetestexclude/three.html",
                                 "http://localhost:50036/mocksitetestexclude/one.html");
        // System under test
        Set<String> hrefs = krwkrwSUT.crawl(host + "/mocksitetestexclude/index.html");

        assertEquals(hrefs.size(), 2);
        verify(mockAction, times(2)).execute(any(FetchedPage.class));
    }

    @Test
    public void test_extractAllFromUrl_Asynchronously() throws Exception {
        KrwlerAction mockAction = mock(KrwlerAction.class);
        Krwkrw krwkrwSUT = new Krwkrw(mockAction);

        krwkrwSUT.setDelay(0);
        // System under test
        Future<Set<String>> futureHrefs = krwkrwSUT.crawlAsync(host + "/mocksite/index.html");

        Set<String> hrefs = futureHrefs.get();

        assertEquals(hrefs.size(), 6);
        verify(mockAction, times(6)).execute(any(FetchedPage.class));

    }
    
    @Test
    public void test_on_exit_callback() throws Exception {
        KrwlerAction mockAction = mock(KrwlerAction.class);
        KrwlerExitCallback mockCallBack = mock(KrwlerExitCallback.class);
        Krwkrw krwkrwSUT = new Krwkrw(mockAction);

        krwkrwSUT.onExit(mockCallBack);
        krwkrwSUT.setDelay(0);
        // System under test
        Future<Set<String>> futureHrefs = krwkrwSUT.crawlAsync(host + "/mocksite/index.html");

        Set<String> hrefs = futureHrefs.get();

        assertEquals(hrefs.size(), 6);
        verify(mockAction, times(6)).execute(any(FetchedPage.class));
        verify(mockCallBack).callBack(anySet());

    }
    
    @Test
    public void test_extractBrokenLink() throws Exception {
        KrwlerAction mockAction = mock(KrwlerAction.class);
        Krwkrw krwkrwSUT = new Krwkrw(mockAction);

        krwkrwSUT.setDelay(0);
        // System under test
        Set<String> hrefs = krwkrwSUT.crawl(host + "/brokenlink/index.html");

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
        KrwlerAction mockAction = mock(KrwlerAction.class);
        Krwkrw krwkrwSUT = new Krwkrw(mockAction);

        Set<String> urlPatternToExclude = new HashSet<>();
        urlPatternToExclude.add(".*");

        krwkrwSUT.skip(urlPatternToExclude);
        krwkrwSUT.setDelay(0);

        // System under test
        Set<String> hrefs = krwkrwSUT.crawl(host + "/mocksitetestexclude/path/index.html");

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
        KrwlerAction mockAction = mock(KrwlerAction.class);
        Krwkrw krwkrwSUT = new Krwkrw(mockAction);

        krwkrwSUT.skip(".*");
        krwkrwSUT.setDelay(0);

        // System under test
        Set<String> hrefs = krwkrwSUT.crawl(host + "/mocksitetestexclude/path/index.html");

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
        KrwlerAction mockAction = mock(KrwlerAction.class);
        Krwkrw krwkrwSUT = new Krwkrw(mockAction);

        krwkrwSUT.skip("\\S+(three.html|four.html)"); // omits three.html and four.html
        krwkrwSUT.setDelay(0);

        // System under test
        Set<String> hrefs = krwkrwSUT.crawl(host + "/mocksitetestexclude/path/index.html");

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
        KrwlerAction mockAction = mock(KrwlerAction.class);
        Krwkrw krwkrwSUT = new Krwkrw(mockAction);

        krwkrwSUT.match("\\S+(\\.html)");
        krwkrwSUT.setDelay(0);

        // System under test
        Set<String> hrefs = krwkrwSUT.crawl(host + "/mocksitetestexclude/path/index.html");

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
        KrwlerAction mockAction = mock(KrwlerAction.class);
        Krwkrw krwkrwSUT = new Krwkrw(mockAction);

        krwkrwSUT.match("\\S+(\\.html)"); // include all
        krwkrwSUT.skip("\\S+(three.html)", "\\S+(four.html)"); // omits three.html and four.html
        krwkrwSUT.setDelay(0);

        // System under test
        Set<String> hrefs = krwkrwSUT.crawl(host + "/mocksitetestexclude/path/index.html");

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
        KrwlerAction mockAction = mock(KrwlerAction.class);
        Krwkrw krwkrwSUT = new Krwkrw(mockAction);

        krwkrwSUT.match("\\S+(\\.html)");
        krwkrwSUT.skip("\\S+(\\.html)");
        krwkrwSUT.setDelay(0);

        // System under test
        Set<String> hrefs = krwkrwSUT.crawl(host + "/mocksitetestexclude/path/index.html");
        assertEquals(hrefs.size(), 0);
    }

}