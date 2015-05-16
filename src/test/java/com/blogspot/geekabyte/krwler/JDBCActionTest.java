package com.blogspot.geekabyte.krwler;

import com.blogspot.geekabyte.krwler.exceptions.FatalError;
import com.blogspot.geekabyte.krwler.util.JDBCAction;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.*;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Integration test for {@link com.blogspot.geekabyte.krwler.util.JDBCAction}
 *
 * @author Dadepo Aderemi.
 */
public class JDBCActionTest {

    private final String HOST = "http://localhost:" + TestServer.HTTP_PORT;

    TestServer testServer;
    private JdbcDataSource dataSource;

    @Before
    public void setUp() throws Exception {
        testServer = new TestServer();
        testServer.start();

        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
    }

    @After
    public void tearDown() throws Exception {
        testServer.shutDown();
    }


    @Test
    public void testJdbcAction() throws Exception {

        JDBCAction jdbcAction = JDBCAction.builder()
                .setDataSource(dataSource)
                .setTableName("pages")
                .buildAction();

        Map<Integer, Map<String, String>> expected = new HashMap<>();
        Map<String, String> firstRow = new HashMap<>();
        Map<String, String> secondRow = new HashMap<>();
        firstRow.put("sourceUrl", "");
        firstRow.put("title", "Index page");
        firstRow.put("url", "http://localhost:50036/mocksitecsvtest/index.html");
        firstRow.put("status", "200");

        secondRow.put("sourceUrl", "http://localhost:50036/mocksitecsvtest/index.html");
        secondRow.put("title", "Page two");
        secondRow.put("url", "http://localhost:50036/mocksitecsvtest/two.html");
        secondRow.put("status", "200");

        expected.put(1,firstRow);
        expected.put(2,secondRow);


        Krwkrw krwkrwSUT = new Krwkrw(jdbcAction);

        // System under test
        krwkrwSUT.doKrawl(HOST + "/mocksitecsvtest/index.html");


        Statement statement = dataSource.getConnection().createStatement();
        ResultSet result = statement.executeQuery("SELECT * from PAGES");
        int row = 1;
        while(result.next()) {
            assertEquals(result.getString("source_url"), expected.get(row).get("sourceUrl"));
            assertEquals(result.getString("title"), expected.get(row).get("title"));
            assertEquals(result.getString("url"), expected.get(row).get("url"));
            assertEquals(result.getString("status"), expected.get(row).get("status"));

            assertNotNull(result.getString("load_time"));
            assertNotNull(result.getString("html"));
            row++;
        }

    }

    @Test(expected = FatalError.class)
    public void testJdbcAction_no_dataSource() throws Exception {

        JDBCAction jdbcAction = JDBCAction.builder()
                .setDataSource(null)
                .setTableName("pages")
                .buildAction();

        Krwkrw krwkrwSUT = new Krwkrw(jdbcAction);
        // System under test
        krwkrwSUT.doKrawl(HOST + "/mocksitecsvtest/index.html");
    }

}