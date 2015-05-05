package com.blogspot.geekabyte.krawkraw;

import com.blogspot.geekabyte.krawkraw.util.ElasticSearchAction;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.*;

/**
 * Integration test for {@link com.blogspot.geekabyte.krawkraw.util.ElasticSearchAction}
 *
 * @author Dadepo Aderemi.
 */
public class ElasticSearchActionIntegrationTest {

    private static final String INDEX_NAME = "data";
    private static final String DOC_TYPE = "webpages";
    private static final String CLUSTER_NAME = "krawkraw_test";
    private final String HOST = "http://localhost:" + TestServer.HTTP_PORT;

    TestServer testServer;

    private Node node;
    private Client client;
    private File dataDir;

    @Before
    public void setUp() throws Exception {
        testServer = new TestServer();
        testServer.start();

        dataDir = Files.createTempDirectory("elasticsearch_data_").toFile();
        Settings settings = ImmutableSettings.settingsBuilder()
                                             .put("path.data", dataDir.toString())
                                             .put("cluster.name", CLUSTER_NAME)
                                             .build();

        node = nodeBuilder().local(true).settings(settings).build().start();
        this.client = node.client();
    }

    @After
    public void tearDown() throws Exception {
        testServer.shutDown();
        FileUtils.deleteDirectory(dataDir);
        node.close();
    }


    @Test
    public void testElasticSearchAction() throws InterruptedException, IOException, URISyntaxException {

        ElasticSearchAction esAction = ElasticSearchAction.builder()
                                                          .setClusterName(CLUSTER_NAME)
                                                          .setClient(client)
                                                          .setIndexName(INDEX_NAME)
                                                          .setDocumentType(DOC_TYPE)
                                                          .setHost("127.0.0.1")
                                                          .buildAction();

        Krawkraw krawkrawSUT = new Krawkraw(esAction);

        // System under test
        krawkrawSUT.doKrawl(HOST + "/mocksitecsvtest/index.html");

        SearchResponse searchResponse = client.prepareSearch(INDEX_NAME).setQuery(matchAllQuery())
                                  .execute()
                                  .actionGet();

        assertEquals(searchResponse.getHits().getTotalHits(), 2);
        Map<String, Object> firstPage = searchResponse.getHits().getAt(0).getSource();
        Map<String, Object> secondPage = searchResponse.getHits().getAt(1).getSource();

        assertEquals(firstPage.get("sourceUrl"), "");
        assertNotNull(firstPage.get("loadTime"));
        assertNotNull(firstPage.get("html"));
        assertEquals(firstPage.get("title"), "Index page");
        assertEquals(firstPage.get("url"), "http://localhost:50036/mocksitecsvtest/index.html");
        assertEquals(firstPage.get("status"), 200);

        assertEquals(secondPage.get("sourceUrl"), "http://localhost:50036/mocksitecsvtest/index.html");
        assertNotNull(secondPage.get("loadTime"));
        assertNotNull(secondPage.get("html"));
        assertEquals(secondPage.get("title"), "Page two");
        assertEquals(secondPage.get("url"), "http://localhost:50036/mocksitecsvtest/two.html");
        assertEquals(secondPage.get("status"), 200);
    }
}
