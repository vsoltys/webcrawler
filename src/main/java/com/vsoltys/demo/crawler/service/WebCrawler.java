package com.vsoltys.demo.crawler.service;

/**
 * Web crawler interface.
 */
public interface WebCrawler {

    /**
     * Perform search by source query.
     *
     * @param searchQuery query to search
     */
    void search(final String searchQuery);
}
