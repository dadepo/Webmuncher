package com.blogspot.geekabyte.krawkraw;

import com.blogspot.geekabyte.krawkraw.interfaces.KrawlerAction;
import com.blogspot.geekabyte.krawkraw.interfaces.callbacks.KrawlerExitCallback;
import com.blogspot.geekabyte.krawkraw.util.CSVAction;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.runners.*;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class KrawkrawIntegrationTest {

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
        KrawlerAction mockAction = mock(KrawlerAction.class);
        Krawkraw krawkrawSUT = new Krawkraw(mockAction);

        krawkrawSUT.setDelay(0);
        // System under test
        Set<String> hrefs = krawkrawSUT.doKrawl(host + "/mocksite/index.html");

        assertEquals(hrefs.size(), 6);
        verify(mockAction, times(6)).execute(any(FetchedPage.class));
    }

    @Test
    public void test_extractAllFromUrl_Asyncronously() throws Exception {
        KrawlerAction mockAction = mock(KrawlerAction.class);
        Krawkraw krawkrawSUT = new Krawkraw(mockAction);

        krawkrawSUT.setDelay(0);
        // System under test
        Future<Set<String>> futureHrefs = krawkrawSUT.doKrawlAsync(host + "/mocksite/index.html");

        Set<String> hrefs = futureHrefs.get();

        assertEquals(hrefs.size(), 6);
        verify(mockAction, times(6)).execute(any(FetchedPage.class));

    }
    
    @Test
    public void test_on_exit_callback() throws Exception {
        KrawlerAction mockAction = mock(KrawlerAction.class);
        KrawlerExitCallback mockCallBack = mock(KrawlerExitCallback.class);
        Krawkraw krawkrawSUT = new Krawkraw(mockAction);

        krawkrawSUT.onExit(mockCallBack);
        krawkrawSUT.setDelay(0);
        // System under test
        Future<Set<String>> futureHrefs = krawkrawSUT.doKrawlAsync(host + "/mocksite/index.html");

        Set<String> hrefs = futureHrefs.get();

        assertEquals(hrefs.size(), 6);
        verify(mockAction, times(6)).execute(any(FetchedPage.class));
        verify(mockCallBack).callBack(anySet());

    }
    
    @Test
    public void test_extractBrokenLink() throws Exception {
        KrawlerAction mockAction = mock(KrawlerAction.class);
        Krawkraw krawkrawSUT = new Krawkraw(mockAction);

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
    }
    
    @Test
    public void testDefaultCSVUtil () throws Exception {

        CSVAction csvAction = CSVAction.builder()
                                       .setDestination(Paths.get("out.csv"))
                                       .setCSVFormat(CSVAction.CSVFORMAT.EXCEL)
                                       .buildAction();
        
        Krawkraw krawkrawSUT = new Krawkraw(csvAction);
        // System under test
        Set<String> urls = krawkrawSUT.doKrawl(host + "/mocksitecsvtest/index.html");

        List<Map<String, String>> maps = readCSV(csvAction.getDestination().toString());
        assertEquals(maps.size(), 2);
        assertEquals(maps.get(0).get("Status"), "200");
        assertEquals(maps.get(1).get("Status"), "200");        
        assertEquals(maps.get(0).get("Title"), "Index page");
        assertEquals(maps.get(1).get("Title"), "Page two");

        Files.deleteIfExists(csvAction.getDestination());
    }
    

    //==================================================== Helpers ====================================================

    
    private List<Map<String, String>> readCSV(String filename) throws IOException {
        List<Map<String, String>> actual = new ArrayList<>();
        ICsvMapReader mapReader;
        mapReader = new CsvMapReader(new FileReader(filename),CsvPreference.STANDARD_PREFERENCE);
        final String[] header = mapReader.getHeader(true);
        Map<String, String> customerMap;
        while( (customerMap = mapReader.read(header)) != null ) {
            actual.add(customerMap);
        }
        
        return actual;
    }


}