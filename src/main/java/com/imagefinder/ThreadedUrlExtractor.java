package com.imagefinder;

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
    private Thread thread;

    public ThreadedUrlExtractor(String url, String baseUrl) {
        this.url = url;
        this.baseUrl = baseUrl;
        visitedUrls = new HashSet<>();
        visitedUrls.add(url);
        subPageUrls = new ArrayList<>();
        this.thread = new Thread(this);
    }

    @Override
    public void run() {
        try {
            extractUrls(Jsoup.connect(url).get());
        } catch (IOException e) {
            System.err.println("Error fetching URLs from subpage: " + url);
        }
    }

    private void extractUrls(Document document) {
        Elements links = document.select("a[href]");
        subPageUrls.addAll(links.stream()
                .map(link -> link.absUrl("href"))
                .filter(url -> isSameDomain(baseUrl, url))
                .peek(visitedUrls::add)
                .collect(Collectors.toList()));
    }

    private boolean isSameDomain(String baseUrl, String url) {
        try {
            URI baseUri = new URI(baseUrl);
            URI subPageUri = new URI(url);
            return baseUri.getHost().equalsIgnoreCase(subPageUri.getHost());
        } catch (URISyntaxException e) {
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
