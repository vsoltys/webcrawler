package com.vsoltys.demo.crawler;

import com.vsoltys.demo.crawler.service.WebCrawler;
import com.vsoltys.demo.crawler.service.WebCrawlerImpl;

import java.util.Scanner;

/**
 *
 * This is a java console application that counts top Javascript libraries used in web pages found on Google.
 * Usage example:
 *
 *  search: google api
 *
 * Similar network call:
 *  curl \
 *      --header "accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng;q=0.8" \
 *      --header "accept-language: en-GB,en;q=0.9,en-US;q=0.8,uk;q=0.7,de;q=0.5" \
 *      --header "user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36" \
 *      "https://www.google.com/search?q=google%20api"
 */
public class App {

    //TODO: move to spring boot & autowire components; define docker container if rest microservice
    private static final String PROMPT = "search: ";

    public static void main(String[] args) {
        final WebCrawler webCrawler = new WebCrawlerImpl();
        prompt();

        try (final Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                webCrawler.search(scanner.nextLine());
                prompt();
            }
        }
    }

    private static void prompt() {
        System.out.print(PROMPT);
    }
}
