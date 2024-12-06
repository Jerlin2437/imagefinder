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

        int maxThreads1Layer = subPageUrls.size();
        for (int x = 0; x < maxThreads1Layer; x++) {
            try {
                ThreadedUrlExtractor threadedUrlExtractor = new ThreadedUrlExtractor(subPageUrls.get(x), baseImageUrl);
                Thread thread = threadedUrlExtractor.getThread();
                thread.start();
                urlExtractorThreads.add(threadedUrlExtractor);
                System.out.println("thread url number: " + x);
            } catch (Exception e) {
                System.out.println("Web crawling failed");
            }
        }

        // Wait for all threads to complete
        for (ThreadedUrlExtractor extractorThread : urlExtractorThreads) {
            try {
                extractorThread.getThread().join();
            } catch (InterruptedException e) {
                System.out.println("Thread join interrupted");
            }
        }

        // Collect results and add them to subPageUrls
        for (ThreadedUrlExtractor extractorThread : urlExtractorThreads) {
            subPageUrls.addAll(extractorThread.getSubPageUrls());
        }

        filterUrls();
        System.out.println("Here are the filtered urls: " + filteredUrls);
    }

    private void crawlImages() {
        List<ThreadedImageExtractor> imageExtractorThreads = new ArrayList<>();

        int maxThreads1Layer = filteredUrls.size();
        for (int x = 0; x < maxThreads1Layer; x++) {
            try {
                ThreadedImageExtractor threadedImageExtractor = new ThreadedImageExtractor(filteredUrls.get(x));
                Thread thread = new Thread(threadedImageExtractor);
                thread.start();
                imageExtractorThreads.add(threadedImageExtractor);
                System.out.println("thread image number: " + x);
            } catch (Exception e) {
                System.out.println("Image crawling failed");
            }
        }

        // Wait for all threads to complete
        for (ThreadedImageExtractor extractorThread : imageExtractorThreads) {
            try {
                extractorThread.run();
            } catch (Exception e) {
                System.out.println("Image thread execution failed");
            }
        }

        // Collect results and add them to imageUrls
        for (ThreadedImageExtractor extractorThread : imageExtractorThreads) {
            imageUrls.addAll(extractorThread.getImageUrls());
        }

        filterImages();
        System.out.println("Here are the filtered images: " + filteredImages);
    }

    private void filterImages() {
        Set<String> uniqueImageUrls = new HashSet<>();

        for (String imageUrl : imageUrls) {
            String imageUrlWithoutWidth = removeWidthParameter(imageUrl);

            if (!filteredImages.contains(imageUrlWithoutWidth) && uniqueImageUrls.add(imageUrlWithoutWidth)) {
                filteredImages.add(imageUrlWithoutWidth);
            }
        }

        // Clear the original imageUrls and add the filtered URLs back
        imageUrls.clear();
        imageUrls.addAll(uniqueImageUrls);
    }

    private String removeWidthParameter(String imageUrl) {
        // Remove the "width" parameter and its value from the URL
        return imageUrl.replaceAll("&width=\\d+", "");
    }


    private void filterUrls() {
        for (int x = 0; x < subPageUrls.size(); x++) {
            String subPageUrl = subPageUrls.get(x);
            if (!filteredUrls.contains(subPageUrl)) {
                filteredUrls.add(subPageUrl);
            }
        }

        // Clear the original subPageUrls and add the filtered URLs back
        subPageUrls.clear();
    }


    //extracts subpages and adds them to subPageUrl, a list of all subpages. Only adds if not visited.
    private void extractUrls(Document document) {
        Elements links = document.select("a[href]");
        subPageUrls.addAll(links.stream()
                .map(link -> link.absUrl("href"))
                .filter(url -> isSameDomain(baseImageUrl, url) && !visitedUrls.contains(url))
                .peek(visitedUrls::add)  // Add the URL to the visitedUrls set
                .collect(Collectors.toList()));
    }

    //extracts image in given URL. Only adds if has not been added before.
    private void extractAndAddImages(Document document) {
        Elements images = document.select("img");
        imageUrls.addAll(images.stream()
                .map(img -> img.absUrl("src"))
                .filter(url -> !visitedImages.contains(url))
                .peek(visitedImages::add)  // Add the image URL to the visitedImages set
                .collect(Collectors.toList()));

        // Print the image URLs to the console
        // imageUrls.forEach(System.out::println);
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
}
