package com.blogspot.geekabyte.krwler.util;

import com.blogspot.geekabyte.krwler.FetchedPage;
import com.blogspot.geekabyte.krwler.interfaces.KrwlerAction;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Implementation of {@link KrwlerAction} that inserts crawled pages
 * to an ElasticSearch index.
 *  
 * Created by dadepo on 1/11/15.
 */
public class ElasticSearchAction implements KrwlerAction {
    
    private Logger logger = LoggerFactory.getLogger(ElasticSearchAction.class);
    private String clusterName = "elasticsearch";
    private String indexName = "webpages";
    private String documentType = "page";
    private String hostName = "127.0.0.1";
    private int idCount = 0;
    private boolean convertToPlainText = false;
    private int port = 9300;
    Client client;

    // prevents direct instantiation
    private ElasticSearchAction() {}

    /**
     * Gets the name of the elastic cluster the fetched page would be indexed in
     * @return the elastic cluster name
     */
    private String getClusterName() {
        return clusterName;
    }

    /**
     * Sets the name of the elastic cluster the fetched page would be indexed in. Default is elasticsearch
     * @param clusterName the elastic cluster name
     */
    private void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    /**
     * Gets the index name the crawled pages would be put in.
     * @return the name of the index the crawled pages would be put in
     */
    private String getIndexName() {
        return indexName;
    }

    /**
     * Sets the index name the crawled pages would be put in. The default is webpages
     * @param indexName the name of the index the crawled pages would be put in
     */
    private void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    /**
     * Gets the hostname of the instance where ElasticSearch is running
     * @return the hostname of ElasticSearch
     */
    private String getHostName() {
        return hostName;
    }

    /**
     * Sets the hostname where the ElasticSearch instance is running. Default is 127.0.0.1 
     * @param hostName the hostname of ElasticSearch
     */
    private void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Gets the port number the ElasticSearch is on
     * @return the port number of ElasticSearch
     */
    private int getPort() {
        return port;
    }

    /**
     * Sets the port number the ElasticSearch is on. Default is 9300
     * @param port the port number of ElasticSearch
     */
    private void setPort(int port) {
        this.port = port;
    }
    
    /**
     * Gets the document type where the fetched pages are going to be indexed
     * @return the document type where the fetched pages are going to be indexed
     */
    private String getDocumentType() {
        return documentType;
    }

    /**
     * Sets the document type where the fetched pages are going to be indexed. Default is page
     * @param documentType the document type of the index
     */
    private void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
    
    /**
     * Sets whether to convert html to plain text when indexing into ElasticSearch
     * @return
     */
    private void convertToPlainText(boolean toConvert) {
        this.convertToPlainText = toConvert;
    }

    /**
     * Get if to convert html to plain text when indexing into ElasticSearch
     * @return true or false
     */
    private boolean isPlainText() {
        return this.convertToPlainText;
    }

    private void setClient(Client client) {
        this.client = client;
    }

    // access to the builder so as to prevent the client
    // having to call its constructor
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating instances of {@link com.blogspot.geekabyte.krwler.util.ElasticSearchAction}
     */
    public static class Builder {
        private ElasticSearchAction elasticSearchAction = new ElasticSearchAction();


        /**
         * Use to set the {@link org.elasticsearch.client.Client} to use
         * @param client the elastic search client
         * @return the builder
         */
        public Builder setClient(Client client) {
            elasticSearchAction.setClient(client);
            return this;
        }

        /**
         * Use to set the name of the elastic cluster the fetched page would be indexed in 
         * @param clusterName the name of the elastic cluster
         * @return the builder
         */
        public Builder setClusterName(String clusterName) {
            elasticSearchAction.setClusterName(clusterName);
            return this;
        }

        /**
         * Use to set the index name the crawled pages would be put in. 
         * @param indexName the index name
         * @return the builder
         */
        public Builder setIndexName(String indexName) {
            elasticSearchAction.setIndexName(indexName);
            return this;
        }

        /**
         * Use to set the host name of the instance ElasticSearch is on.
         * @param host the hostname
         * @return the builder
         */
        public Builder setHost(String host) {
            elasticSearchAction.setHostName(host);
            return this;
        }

        /**
         * Use to set the port number the ElasticSearch is on 
         * @param portNumber the ElasticSearch port
         * @return the builder
         */
        public Builder setPort(int portNumber) {
            elasticSearchAction.setPort(portNumber);
            return this;
        }

        /**
         * Use to set the ElasticSearch document type of the fetched pages
         * @param documentType the ElasticSearch document type
         * @return the builder
         */
        public Builder setDocumentType(String documentType) {
            elasticSearchAction.setDocumentType(documentType);
            return this;
        }

        /**
         * Use to set if html content should be converted to plain text before indexing into ElasticSearch. The
         * Default is false.
         * @param convertToPlainText true or false
         * @return the builder
         */
        public Builder convertToPlainText(boolean convertToPlainText) {
            elasticSearchAction.convertToPlainText(convertToPlainText);
            return this;
        }

        public ElasticSearchAction buildAction() {
            return elasticSearchAction;
        }
    }
    
    @Override
    public void execute(FetchedPage page) {
        if (client == null) {
            logger.info("No Node client found. Sure the buildAction was properly called?");
            return;
        }

        IndexResponse response;
        boolean convert = this.isPlainText();

        try {
            response = client.prepareIndex(getIndexName(), getDocumentType(), String.valueOf(++idCount))
                                           .setSource(jsonBuilder()
                                              .startObject()
                                              .field("url", page.getUrl())
                                              .field("title", page.getTitle())
                                              .field("sourceUrl", page.getSourceUrl())
                                              .field("html", convert? Jsoup.parse(page.getHtml()).text(): page.getHtml())
                                              .field("status", page.getStatus())
                                              .field("loadTime", page.getLoadTime())
                                           .endObject())
                                           .execute()
                                           .actionGet();
        } catch (IOException e) {
            logger.debug("Failed to put page crawled from {} into ElasticSearch", page.getUrl());
            return;
        }
        
        logger.info("Index page at {} into ElasticSearch, with ElasticSearch id of {}", 
                    page.getUrl(), response.getId());
    }
}
