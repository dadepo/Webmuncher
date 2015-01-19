package com.blogspot.geekabyte.krawkraw.util;

import com.blogspot.geekabyte.krawkraw.FetchedPage;
import com.blogspot.geekabyte.krawkraw.interfaces.KrawlerAction;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Implementation of {@link com.blogspot.geekabyte.krawkraw.interfaces.KrawlerAction} that dumps crawled pages
 * to ElasticSearch.
 *  
 * Created by dadepo on 1/11/15.
 */
public class ElasticSearchAction implements KrawlerAction {
    
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
    public String getClusterName() {
        return clusterName;
    }

    /**
     * Sets the name of the elastic cluster the fetched page would be indexed in. Default is elasticsearch
     * @param clusterName the elastic cluster name
     */
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    /**
     * Gets the index name the crawled pages would be put in.
     * @return the name of the index the crawled pages would be put in
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Sets the index name the crawled pages would be put in. The default is webpages
     * @param indexName the name of the index the crawled pages would be put in
     */
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    /**
     * Gets the hostname of the instance where ElasticSearch is running
     * @return the hostname of ElasticSearch
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Sets the hostname where the ElasticSearch instance is running. Default is 127.0.0.1 
     * @param hostName the hostname of ElasticSearch
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Gets the port number the ElasticSearch is on
     * @return the port number of ElasticSearch
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port number the ElasticSearch is on. Default is 9300
     * @param port the port number of ElasticSearch
     */
    public void setPort(int port) {
        this.port = port;
    }
    
    /**
     * Gets the document type where the fetched pages are going to be indexed
     * @return the document type where the fetched pages are going to be indexed
     */
    public String getDocumentType() {
        return documentType;
    }

    /**
     * Sets the document type where the fetched pages are going to be indexed. Default is page
     * @param documentType the document type of the index
     */
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
    
    /**
     * Sets whether to convert html to plain text when indexing into ElasticSearch
     * @return
     */
    public void convertToPlainText(boolean toConvert) {
        this.convertToPlainText = toConvert;
    }

    /**
     * Get if to convert html to plain text when indexing into ElasticSearch
     * @return true or false
     */
    public boolean isPlainText() {
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
     * Builder for creating instances of {@link com.blogspot.geekabyte.krawkraw.util.ElasticSearchAction}
     */
    public static class Builder {
        private ElasticSearchAction instance = new ElasticSearchAction();

        /**
         * Use to set the name of the elastic cluster the fetched page would be indexed in 
         * @param name the name of the elastic cluster
         * @return the builder
         */
        public Builder setClusterName(String name) {
            instance.setClusterName(name);
            return this;
        }

        /**
         * Use to set the index name the crawled pages would be put in. 
         * @param indexName the index name
         * @return the builder
         */
        public Builder setIndexName(String indexName) {
            instance.setIndexName(indexName);
            return this;
        }

        /**
         * Use to set the host name of the instance ElasticSearch is on.
         * @param host the hostname
         * @return the builder
         */
        public Builder setHost(String host) {
            instance.setHostName(host);
            return this;
        }

        /**
         * Use to set the port number the ElasticSearch is on 
         * @param portNumber the ElasticSearch port
         * @return the builder
         */
        public Builder setPort(int portNumber) {
            instance.setPort(portNumber);
            return this;
        }

        /**
         * Use to set the ElasticSearch document type of the fetched pages
         * @param documentType the ElasticSearch document type
         * @return the builder
         */
        public Builder setDocumentType(String documentType) {
            instance.setDocumentType(documentType);
            return this;
        }

        /**
         * Use to set if html content should be converted to plain text before indexing into ElasticSearch. The
         * Default is false.
         * @param convertToPlainText true or false
         * @return the builder
         */
        public Builder convertToPlainText(boolean convertToPlainText) {
            instance.convertToPlainText(convertToPlainText);
            return this;
        }

        public ElasticSearchAction buildAction() {
            Settings settings = ImmutableSettings.settingsBuilder()
                                                 .put("cluster.name", instance.getClusterName()).build();
            Client client =  new TransportClient(settings)
                    .addTransportAddress(new InetSocketTransportAddress(instance.getHostName(), instance.getPort()
                    ));
            instance.setClient(client);
            return instance;
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
