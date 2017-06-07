package com.blogspot.geekabyte.webmuncher.util;

import com.blogspot.geekabyte.webmuncher.FetchedPage;
import com.blogspot.geekabyte.webmuncher.exceptions.FatalError;
import com.blogspot.geekabyte.webmuncher.interfaces.FetchAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

/**
 * Implementation of {@link FetchAction} that inserts crawled pages
 * to an SQL database.
 *
 * @author Dadepo Aderemi.
 */
public class JDBCAction implements FetchAction {

    Logger logger = LoggerFactory.getLogger(JDBCAction.class);

    private String tableName = "krawled_webpages";
    private DataSource dataSource;

    // prevents direct instantiation
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
    public void process(FetchedPage page) {
        if (dataSource == null) {
            String msg = "Cannot save crawled pages. A database datasource was not set. Make sure you call "
                    + "setDataSource on builder";

            logger.error(msg);
            throw new FatalError(msg);
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
            String createTableSql = "CREATE TABLE IF NOT EXISTS {TABLE_NAME} " +
                    "(title MEDIUMTEXT, " +
                    " url MEDIUMTEXT, " +
                    " source_url MEDIUMTEXT, " +
                    " html MEDIUMTEXT, " +
                    " status MEDIUMTEXT, " +
                    " load_time MEDIUMTEXT)";

            createTableSql = createTableSql.replace("{TABLE_NAME}", tableName);
            statement.execute(createTableSql);

        } catch (SQLException e) {
            logger.error("Error creating statement from connection", e);
        }
    }

    // Convenient access to the builder constructor, alternative to having to do a
    // new JDBCAction.Build() by the client
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating instances of {@link com.blogspot.geekabyte.webmuncher.util.JDBCAction}
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
        public Builder setTableName(String tableName) {
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
         * Returns a properly configured instance of {@link com.blogspot.geekabyte.webmuncher.util.JDBCAction}
         *
         * @return an instance of {@link com.blogspot.geekabyte.webmuncher.util.JDBCAction}
         */
        public JDBCAction buildAction() {
            return jdbcAction;
        }
    }
}
