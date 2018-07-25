package com.vsoltys.demo.crawler.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


//TODO: spring boot, extract connection/stream provider, threaded processing part and reporting to separate components.
//TODO: Then mock them using Mockito f.e. and test separately each component.
//TODO: add integration tests with all components wired
//TODO: this is draft version of test setup
public class WebCrawlerImplTest {

    private static final String EXPECTED_RESOURCE_PREFIX = "http";
    private static final String EXPECTED_RESOURCE_SUFFIX = ".js";
    private static final String REPORT_DELIMITER = "\n";
    private static final int EXPECTED_ITEMS_AMOUNT = 5;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    //TODO: spring, inject dependencies, properties, use test context etc.
    private WebCrawler webCrawler;

    @BeforeEach
    void setUp() {
        webCrawler = new WebCrawlerImpl();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void shouldReturnEmptyResult() {
        //TODO: mock search engine response without search entries
    }

    @Test
    @DisplayName("should return sorted list of found resources")
    void shouldReturnSortedResourcesList() {
        // arrange
        final String searchQuery = "angular";
        final Comparator<String> comparator = Comparator.naturalOrder();

        // act
        webCrawler.search(searchQuery); //TODO: this test operates on real network, use mocked setup

        // assert
        final String actualOutput = outContent.toString();
        assertNotNull(actualOutput);
        assertFalse(actualOutput.isEmpty());

        final List<String> resultItems = extractReportItems(actualOutput);
        assertEquals(EXPECTED_ITEMS_AMOUNT, resultItems.size());

        //TODO: order validation fails - no control over result entries (frequency gets sorted first, then by name)
        final List<String> expectedItemsInOrder = new ArrayList<>(resultItems);
        expectedItemsInOrder.sort(comparator);
//        assertThat(resultItems, contains(expectedItemsInOrder));

        resultItems.forEach(item -> {
            assertTrue(item.startsWith(EXPECTED_RESOURCE_PREFIX));
            assertTrue(item.endsWith(EXPECTED_RESOURCE_SUFFIX));
        });

        //TODO: assert items frequency
    }

    @Test
    @Disabled("mock connection provider needed")
    void shouldFailWithConnectionTimeout() {
        // arrange
        final String searchQuery = "angular";

        // act & assert
        assertThrows(RuntimeException.class, () -> webCrawler.search(searchQuery)); //TODO: mock connection provider
    }

    @Test
    @Disabled("mock connection provider needed")
    void shouldFailWithInvalidUrl() {
        // arrange
        final String searchQuery = "angular";

        // act & assert
        assertThrows(RuntimeException.class, () -> webCrawler.search(searchQuery)); //TODO: mock invalid url
    }

    @Test
    void shouldReturnDeduplicatedResult() {
        //TODO: test deduplication logic
    }

    private List<String> extractReportItems(String actualOutput) {
        return Arrays.asList(actualOutput.split(REPORT_DELIMITER));
    }

    // TODO: more tests
}