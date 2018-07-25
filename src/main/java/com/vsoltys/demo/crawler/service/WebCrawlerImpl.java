package com.vsoltys.demo.crawler.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This {@link WebCrawler} implementation performs libraries search in order:
 *
 * 1) Get a Google result page for the search term
 * 2) Extract main result links from the page
 * 3) Download the respective pages and extract the names of Javascript libraries used in them
 * 4) Print top 5 most used libraries to standard output
 */
public class WebCrawlerImpl implements WebCrawler {

    // TODO: use jsoup
    private static final Pattern SEARCH_RESULTS_PATTERN = Pattern.compile("<cite .*?>(.*?)</cite>");

    // example: https://apis.google.com/js/base.js
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("(http.[^\"']*?\\.js)\"");

    private static final int REGEXP_RESULT_GROUP = 1;
    private static final int QUERY_RESPONSE_TIMEOUT = 20;
    private static final int REPORT_ITEMS_AMOUNT = 5;

    // TODO: spring boot, extract properties
    private static final String SEARCH_QUERY = "https://www.google.com/search?q=%s";
    private static final String CHARSET = "UTF-8";
    private static final String EOF_DELIMITER = "\\Z";

    private static final String ACCEPT_PROPERTY_KEY = "accept";
    private static final String ACCEPT_PROPERTY_VALUE = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
    private static final String ACCEPT_LANGUAGE_PROPERTY_KEY = "accept-language";
    private static final String ACCEPT_LANGUAGE_PROPERTY_VALUE = "en-GB,en;q=0.9,en-US;q=0.8,uk;q=0.7,ru;q=0.6,de;q=0.5";
    private static final String USER_AGENT_PROPERTY_KEY = "user-agent";
    private static final String USER_AGENT_PROPERTY_VALUE = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36";


    // TODO: replace init in method by task resource provider
    private ConcurrentMap<String, Integer> resourcesByFrequency;

    @Override
    public void search(final String searchQuery) {
        try {
            final List<String> targets = searchTargets(searchQuery);
            collectResources(targets);

            //TODO: extract report functionality to separate component
            generateReport();

        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Gets a search result page for the query and extracts result links from that.
     *
     * @param searchQuery source query
     * @return {@link List} with retrieved links
     */
    private List<String> searchTargets(final String searchQuery) {
        resourcesByFrequency = new ConcurrentHashMap<>();
        return extractResults(getSearchUrl(searchQuery), SEARCH_RESULTS_PATTERN);
    }

    /**
     * Downloads pages by provided urls and extracts the names of resources used in them.
     * Network part is processed by separate threads.
     *
     * @param searchResults source urls
     */
    private void collectResources(final List<String> searchResults) throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(searchResults.size());

        searchResults.stream()
                .map(this::extract)
                .forEach(executorService::submit);

        executorService.shutdown();
        executorService.awaitTermination(QUERY_RESPONSE_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Generates report: prints top list of most used libraries
     */
    private void generateReport() {
        final TreeMap<String, Integer> sortedResourcesByFrequency = new TreeMap<>(getFrequencyComparator());
        sortedResourcesByFrequency.putAll(resourcesByFrequency);

        sortedResourcesByFrequency.keySet().stream()
                .limit(REPORT_ITEMS_AMOUNT)
                .forEach(System.out::println);
    }

    /**
     * Extracts results from given url by {@link Pattern} specified.
     *
     * @param searchUrl     source url
     * @param searchPattern search pattern to apply
     * @return {@link List} of search results as {@link String}s
     */
    private List<String> extractResults(final String searchUrl, final Pattern searchPattern) {
        return getScanner(searchUrl).findAll(searchPattern)
                .map(this::getMatchResult)
                .collect(Collectors.toList());
    }

    /**
     * Creates {@link Scanner} for search url processing.
     *
     * @param searchUrl source url
     * @return {@link Scanner}
     */
    private Scanner getScanner(final String searchUrl) {
        try {
            final Scanner scanner = new Scanner(getConnection(searchUrl).getInputStream());
            scanner.useDelimiter(EOF_DELIMITER);
            return scanner;
        } catch (final IOException exception) {
            //TODO: extract to platform exception to control error codes etc.
            throw new RuntimeException(String.format("Connection error: %s", exception.getMessage()));
        }
    }

    /**
     * Initiates connection for given search url.
     *
     * @param searchUrl url to use
     * @return opened {@link URLConnection}
     * @throws IOException in case connection cannot be processed.
     */
    private URLConnection getConnection(final String searchUrl) throws IOException {
        final URLConnection connection = new URL(searchUrl).openConnection();

        connection.setRequestProperty(ACCEPT_PROPERTY_KEY, ACCEPT_PROPERTY_VALUE);
        connection.setRequestProperty(ACCEPT_LANGUAGE_PROPERTY_KEY, ACCEPT_LANGUAGE_PROPERTY_VALUE);
        connection.setRequestProperty(USER_AGENT_PROPERTY_KEY, USER_AGENT_PROPERTY_VALUE);
        connection.connect();

        return connection;
    }

    /**
     * Extracts matched result.
     *
     * @param matchResult
     * @return {@link String}
     */
    private String getMatchResult(final MatchResult matchResult) {
        return matchResult.group(REGEXP_RESULT_GROUP);
    }

    /**
     * Formats search query with source text.
     *
     * @param searchText text to query
     * @return formatted search query
     */
    private String getSearchUrl(final String searchText) {
        try {
            return String.format(SEARCH_QUERY, URLEncoder.encode(searchText, CHARSET));
        } catch (final UnsupportedEncodingException exception) {
            throw new RuntimeException(String.format("Charset %s not supported", CHARSET));
        }
    }

    /**
     * Returns {@link Comparator} that compares by:
     * 1) frequency, desc
     * 2) resource name, asc (natural order)
     *
     * @return {@link Comparator}
     */
    private Comparator<String> getFrequencyComparator() {
        return (first, second) -> {
            final int result = resourcesByFrequency.get(second).compareTo(resourcesByFrequency.get(first));
            return result != 0 ? result : first.compareToIgnoreCase(second);
        };
    }

    /**
     * Merges items into map by incrementing equal keys' values (frequency).
     *
     * @param resource item to add
     */
    // TODO: maintain lookup table to implement deduplication for the same Javascript libraries with different names
    private void addResource(final String resource) {
        resourcesByFrequency.merge(resource, 1, (old, in) -> old + in);
    }

    /**
     * Defines a task for url processing.
     *
     * @param url url to process by thread
     * @return {@link Callable}
     */
    private Callable<Void> extract(final String url) {
        return () -> {
            extractResults(url, RESOURCE_PATTERN).forEach(this::addResource);
            return null;
        };
    }
}
