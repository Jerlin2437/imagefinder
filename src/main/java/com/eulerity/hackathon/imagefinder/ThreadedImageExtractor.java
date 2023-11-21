package com.eulerity.hackathon.imagefinder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ThreadedImageExtractor implements Runnable {
    private String url;
    private Set<String> visitedUrls;
    private Set<String> visitedImages;
    private String baseImageUrl;

    public ThreadedImageExtractor(String url, Set<String> visitedUrls, Set<String> visitedImages, String baseImageUrl) {
        this.url = url;
        this.visitedUrls = visitedUrls;
        this.visitedImages = visitedImages;
        this.baseImageUrl = baseImageUrl;
    }

    @Override
    public void run() {
        try {
            extractUrls(Jsoup.connect(url).get());
            System.out.println("Fetching images from subpage: " + url);
            Document subDocument = Jsoup.connect(url).get();
            extractAndAddImages(subDocument);

        } catch (IOException e) {
            System.err.println("Error fetching images from subpage: " + url);
            e.printStackTrace();
        }
    }

    private void extractUrls(Document document) {
        Elements links = document.select("a[href]");
        List<String> subPageUrls = links.stream()
                .map(link -> link.absUrl("href"))
                .filter(subUrl -> isSameDomain(baseImageUrl, subUrl) && !visitedUrls.contains(subUrl))
                .peek(visitedUrls::add)
                .collect(Collectors.toList());
        // You may want to process the subPageUrls further or store them for future crawling
    }

    private void extractAndAddImages(Document document) {
        Elements images = document.select("img");
        List<String> imageUrls = images.stream()
                .map(img -> img.absUrl("src"))
                .filter(url -> !visitedImages.contains(url))
                .peek(visitedImages::add)
                .collect(Collectors.toList());
        // You may want to process the imageUrls further or store them
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
}
