package com.blogspot.geekabyte.krawkraw;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.runners.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class KrawkrawTest {


    Krawkraw krawkrawSUT = new Krawkraw();

    @Test
    public void test_extractAbsHref() {
        // System under test
        Document doc = createDocument("fivelinks");
        List<String> hrefs = krawkrawSUT.extractAbsHref(doc);
        assertEquals(hrefs.size(), 5);

    }

    @Test
    public void test_extractHref() {
        // System under test
        Document doc = createDocument("fivelinks");
        List<String> hrefs = krawkrawSUT.extractHref(doc);
        assertEquals(hrefs.size(), 5);
    }

    //==================================================== Helpers ====================================================


    private Document createDocument(String type) {


        switch(type) {
            case("fivelinks"): {
                URL path = this.getClass().getResource("/fivelinks.html");
                File file = new File(path.getFile());
                Document doc = null;
                try {
                    doc = Jsoup.parse(file, "UTF-8");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return doc;
            }
            default: {
                return null;
            }
        }

    }
}