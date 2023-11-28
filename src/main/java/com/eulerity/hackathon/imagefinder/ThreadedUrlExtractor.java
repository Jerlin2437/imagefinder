package com.eulerity.hackathon.imagefinder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ThreadedUrlExtractor implements Runnable {
    private List<String> subPageUrls;
    private String url;
    private Set<String> visitedUrls;
    private String baseUrl;
    private Thread thread;  // Store reference to the associated thread

    public ThreadedUrlExtractor(String url, String baseUrl) {
        this.url = url;
        this.baseUrl = baseUrl;
        visitedUrls = new HashSet<>();
        visitedUrls.add(url);
        subPageUrls = new ArrayList<>();
        this.thread = new Thread(this);  // Create the associated thread
    }

    @Override
    public void run() {
        try {
            extractUrls(Jsoup.connect(url).get());
        } catch (IOException e) {
            System.err.println("Error fetching urls from subpage: " + url);
        }
    }

    private void extractUrls(Document document) {
        Elements links = document.select("a[href]");
        subPageUrls.addAll(links.stream()
                .map(link -> link.absUrl("href"))
                .filter(url -> isSameDomain(baseUrl, url))
                .peek(visitedUrls::add)  // Add the URL to the visitedUrls set
                .collect(Collectors.toList()));

        System.out.println(subPageUrls);
    }

    private boolean isSameDomain(String baseUrl, String url) {
        try {
            URI baseUri = new URI(baseUrl);
            URI subPageUri = new URI(url);

            // Compare the host (domain) of the base URL and subpage URL
            return baseUri.getHost().equalsIgnoreCase(subPageUri.getHost());

        } catch (URISyntaxException e) {
            // Handle the exception if the URL is not well-formed
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getSubPageUrls() {
        return subPageUrls;
    }

    public Thread getThread() {
        return thread;
    }
}
