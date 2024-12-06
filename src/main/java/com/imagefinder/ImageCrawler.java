package com.imagefinder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ImageCrawler {
    public List<String> imageUrlsWithLabels;
    public List<String> imageUrls;
    public List<String> subPageUrls;
    public List<String> filteredUrls;
    private List<String> filteredImages;

    public Set<String> visitedUrls;
    public Set<String> visitedImages;
    private String baseImageUrl;

    public List<String> crawlImages(String initialUrl) {
        initializeCrawler(initialUrl);
        crawlWeb();
        crawlImages();
        return filteredImages;
    }

    private void initializeCrawler(String initialUrl) {
        visitedUrls = new HashSet<>();
        visitedImages = new HashSet<>();
        imageUrls = new ArrayList<>();
        subPageUrls = new ArrayList<>();
        filteredUrls = new ArrayList<>();
        filteredImages = new ArrayList<>();
        baseImageUrl = initialUrl;

        try {
            Document document = Jsoup.connect(initialUrl).get();
            extractUrls(document);
        } catch (Exception e) {
            System.out.println("Initialization failed");
        }
    }

    private void crawlWeb() {
        List<ThreadedUrlExtractor> urlExtractorThreads = new ArrayList<>();

        for (String url : subPageUrls) {
            try {
                ThreadedUrlExtractor extractor = new ThreadedUrlExtractor(url, baseImageUrl);
                Thread thread = extractor.getThread();
                thread.start();
                urlExtractorThreads.add(extractor);
            } catch (Exception e) {
                System.out.println("Web crawling failed");
            }
        }

        for (ThreadedUrlExtractor extractor : urlExtractorThreads) {
            try {
                extractor.getThread().join();
            } catch (InterruptedException e) {
                System.out.println("Thread join interrupted");
            }
        }

        for (ThreadedUrlExtractor extractor : urlExtractorThreads) {
            subPageUrls.addAll(extractor.getSubPageUrls());
        }

        filterUrls();
    }

    private void crawlImages() {
        List<ThreadedImageExtractor> imageExtractorThreads = new ArrayList<>();

        for (String url : filteredUrls) {
            try {
                ThreadedImageExtractor extractor = new ThreadedImageExtractor(url);
                Thread thread = new Thread(extractor);
                thread.start();
                imageExtractorThreads.add(extractor);
            } catch (Exception e) {
                System.out.println("Image crawling failed");
            }
        }

        for (ThreadedImageExtractor extractor : imageExtractorThreads) {
            try {
                extractor.run();
            } catch (Exception e) {
                System.out.println("Image thread execution failed");
            }
        }

        for (ThreadedImageExtractor extractor : imageExtractorThreads) {
            imageUrls.addAll(extractor.getImageUrls());
        }

        filterImages();
    }

    private void filterImages() {
        Set<String> uniqueImageUrls = new HashSet<>();

        for (String imageUrl : imageUrls) {
            String imageUrlWithoutWidth = removeWidthParameter(imageUrl);

            if (!filteredImages.contains(imageUrlWithoutWidth) && uniqueImageUrls.add(imageUrlWithoutWidth)) {
                filteredImages.add(imageUrlWithoutWidth);
            }
        }

        imageUrls.clear();
        imageUrls.addAll(uniqueImageUrls);
    }

    private String removeWidthParameter(String imageUrl) {
        return imageUrl.replaceAll("&width=\\d+", "");
    }

    private void filterUrls() {
        for (String url : subPageUrls) {
            if (!filteredUrls.contains(url)) {
                filteredUrls.add(url);
            }
        }

        subPageUrls.clear();
    }

    private void extractUrls(Document document) {
        Elements links = document.select("a[href]");
        subPageUrls.addAll(links.stream()
                .map(link -> link.absUrl("href"))
                .filter(url -> isSameDomain(baseImageUrl, url) && !visitedUrls.contains(url))
                .peek(visitedUrls::add)
                .collect(Collectors.toList()));
    }

    private void extractAndAddImages(Document document) {
        Elements images = document.select("img");
        imageUrls.addAll(images.stream()
                .map(img -> img.absUrl("src"))
                .filter(url -> !visitedImages.contains(url))
                .peek(visitedImages::add)
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
}
