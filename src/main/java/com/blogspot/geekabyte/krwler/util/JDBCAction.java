package com.blogspot.geekabyte.krwler.util;

import com.blogspot.geekabyte.krwler.FetchedPage;
import com.blogspot.geekabyte.krwler.interfaces.KrwlerAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

/**
 * Implementation of {@link KrwlerAction} that inserts crawled pages
 * to an SQL database.
 *
 * @author Dadepo Aderemi.
 */
public class JDBCAction implements KrwlerAction {

    Logger logger = LoggerFactory.getLogger(JDBCAction.class);

    private String tableName = "krawled_webpages";
    private DataSource dataSource;

    private JDBCAction() {
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void execute(FetchedPage page) {
        if (dataSource == null) {
            logger.error("Cannot save crawled page. A database datasource was not set. Make sure you call "
                                 + "setDataSource on builder");
        }


        try(Connection connection = dataSource.getConnection()) {
            createTableIfNotExist(connection);


            String insertValuesSql = "INSERT INTO {TABLE_NAME}"
                    + "(title, url, source_url, html, status, load_time) VALUES"
                    + "(?,?,?,?,?,?)";
            insertValuesSql = insertValuesSql.replace("{TABLE_NAME}", tableName);

            PreparedStatement preparedStatement = connection.prepareStatement(insertValuesSql);
            preparedStatement.setString(1, page.getTitle());
            preparedStatement.setString(2, page.getUrl());
            preparedStatement.setString(3, page.getSourceUrl());
            preparedStatement.setString(4, page.getHtml());
            preparedStatement.setString(5, String.valueOf(page.getStatus()));
            preparedStatement.setString(6, String.valueOf(page.getLoadTime()));
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            logger.error("Exception while creating and inserting fetched contents:", e);
        }

    }

    private void createTableIfNotExist(Connection connection) {
        try {
            Statement statement = connection.createStatement();
            String createTableSql = "CREATE TABLE {TABLE_NAME} " +
                    "(title TEXT, " +
                    " url TEXT, " +
                    " source_url TEXT, " +
                    " html TEXT, " +
                    " status TEXT, " +
                    " load_time TEXT)";

            createTableSql = createTableSql.replace("{TABLE_NAME}", tableName);

            if (!isTableExist(connection, tableName)) {
                statement.execute(createTableSql);
            }

        } catch (SQLException e) {
            logger.error("Error creating statement from connection", e);
        }
    }

    private Boolean isTableExist(Connection connection, String tableName) {
        DatabaseMetaData databaseMetadata = null;
        try {
            databaseMetadata = connection.getMetaData();
            ResultSet rs = databaseMetadata.getTables(null, null, tableName.toUpperCase(), null);
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error getting metadata information", e);
        }
        return false;
    }

    /**
     * Builder for creating instances of {@link com.blogspot.geekabyte.krwler.util.JDBCAction}
     */
    public static class Builder {

        private JDBCAction jdbcAction = new JDBCAction();

        /**
         * Sets the table name the contents of the crawled pages would be stored. If not set, defaults to
         * krawled_webpages
         *
         * @param tableName the table name
         * @return the builder
         */
        public Builder setDestination(String tableName) {
            jdbcAction.setTableName(tableName);
            return this;
        }

        /**
         * Sets the {@link javax.sql.DataSource} used to interact with a sql database
         *
         * @param dataSource the dataSource used to retrieve a {@link java.sql.Connection} used
         *                   to interact with sql database
         * @return the builder
         */
        public Builder setDataSource(DataSource dataSource) {
            jdbcAction.setDataSource(dataSource);
            return this;
        }

        /**
         * Returns a properly configured instance of {@link com.blogspot.geekabyte.krwler.util.JDBCAction}
         *
         * @return an instance of {@link com.blogspot.geekabyte.krwler.util.JDBCAction}
         */
        public JDBCAction buildAction() {
            return jdbcAction;
        }
    }
}
