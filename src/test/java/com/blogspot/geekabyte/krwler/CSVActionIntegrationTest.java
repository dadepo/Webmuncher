package com.blogspot.geekabyte.krwler;

import com.blogspot.geekabyte.krwler.exceptions.FatalError;
import com.blogspot.geekabyte.krwler.util.CSVAction;
import org.junit.*;
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

import static org.junit.Assert.assertEquals;

/**
 * @author Dadepo Aderemi.
 */
public class CSVActionIntegrationTest {
    private final String HOST = "http://localhost:" + TestServer.HTTP_PORT;

    TestServer testServer;

    @Before
    public void setUp() throws Exception {
        testServer = new TestServer();
        testServer.start();
    }

    @After
    public void tearDown() throws Exception {
        testServer.shutDown();
    }


    @Test
    public void testDefaultCSVUtil () throws Exception {

        CSVAction csvAction = CSVAction.builder()
                                       .setDestination(Paths.get("out.csv"))
                                       .setCSVFormat(CSVAction.CSVFORMAT.EXCEL)
                                       .buildAction();

        Krwkrw krwkrwSUT = new Krwkrw(csvAction);
        // System under test
        Set<String> urls = krwkrwSUT.doKrawl(HOST + "/mocksitecsvtest/index.html");

        List<Map<String, String>> maps = readCSV(csvAction.getDestination().toString());
        assertEquals(maps.size(), 2);
        assertEquals(maps.get(0).get("Status"), "200");
        assertEquals(maps.get(1).get("Status"), "200");
        assertEquals(maps.get(0).get("Title"), "Index page");
        assertEquals(maps.get(1).get("Title"), "Page two");

        Files.deleteIfExists(csvAction.getDestination());
    }

    @Test(expected = FatalError.class)
    public void testDefaultCSVUtil_File_Cannot_Be_Created() throws Exception {

        CSVAction csvAction = CSVAction.builder()
                .setDestination(Paths.get("_/xyz123_~`/out.csv"))
                .setCSVFormat(CSVAction.CSVFORMAT.EXCEL)
                .buildAction();

        Krwkrw krwkrwSUT = new Krwkrw(csvAction);
        // System under test
        Set<String> urls = krwkrwSUT.doKrawl(HOST + "/mocksitecsvtest/index.html");
    }

    //==================================================== Helpers ====================================================


    private List<Map<String, String>> readCSV(String filename) throws IOException {
        List<Map<String, String>> actual = new ArrayList<>();
        ICsvMapReader mapReader;
        mapReader = new CsvMapReader(new FileReader(filename), CsvPreference.STANDARD_PREFERENCE);
        final String[] header = mapReader.getHeader(true);
        Map<String, String> customerMap;
        while( (customerMap = mapReader.read(header)) != null ) {
            actual.add(customerMap);
        }

        return actual;
    }

}
