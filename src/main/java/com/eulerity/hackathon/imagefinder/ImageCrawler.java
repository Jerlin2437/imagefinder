

package com.eulerity.hackathon.imagefinder;

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
    public List<String> imageUrls;
    public List<String> subPageUrls;
    public List<String> filteredUrls;
    private List<String> filteredImages;

    public Set<String> visitedUrls;
    public Set<String> visitedImages;
    private String baseImageUrl;

    public List<String> crawlImages(String initialUrl) {
        //one layer web crawl
        visitedUrls = new HashSet<>();
        visitedImages = new HashSet<>();
        imageUrls = new ArrayList<>();
        subPageUrls = new ArrayList<>();
        filteredUrls = new ArrayList<>();
        filteredImages = new ArrayList<>();
        baseImageUrl = initialUrl;
        List<Thread> threads = new ArrayList<>();
        try {
            Document document = Jsoup.connect(initialUrl).get();
            extractUrls(document);
       //     extractAndAddImages(document);
            int maxThreads1Layer = subPageUrls.size();

            List<ThreadedUrlExtractor> urlExtractorThreads = new ArrayList<>();


            for (int x = 0; x < maxThreads1Layer; x++) {
                try {
                    ThreadedUrlExtractor threadedUrlExtractor = new ThreadedUrlExtractor(subPageUrls.get(x), baseImageUrl);
                    Thread thread = threadedUrlExtractor.getThread();  // Get the associated thread
                    thread.start();
                    urlExtractorThreads.add(threadedUrlExtractor);
                    System.out.println("thread url number: " + x);
                } catch (Exception e) {
                    // Handle exception
                }
            }

// Wait for all threads to complete
            for (ThreadedUrlExtractor extractorThread : urlExtractorThreads) {
                try {
                    extractorThread.getThread().join();
                } catch (InterruptedException e) {
                    // Handle InterruptedException
                }
            }

// Collect results and add them to subPageUrls
            for (ThreadedUrlExtractor extractorThread : urlExtractorThreads) {
                subPageUrls.addAll(extractorThread.getSubPageUrls());
            }


            filterUrls();
            System.out.println("Here are the filtered urls: "+ filteredUrls);

            List<ThreadedImageExtractor> imageExtractorThreads = new ArrayList<>();
            maxThreads1Layer = filteredUrls.size();
            for (int x = 0; x < maxThreads1Layer; x++){
                try{
                    ThreadedImageExtractor threadedImageExtractor = new ThreadedImageExtractor(filteredUrls.get(x));
                    Thread thread = new Thread(threadedImageExtractor);
                    thread.start();
                    imageExtractorThreads.add(threadedImageExtractor);
                    System.out.println("thread image number: " + x);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            // Wait for all threads to complete
            for (ThreadedImageExtractor extractorThread : imageExtractorThreads) {
                try {
                    extractorThread.run();
                } catch (Exception e) {
                    // Handle exception
                }
            }

// Collect results and add them to imageUrls
            for (ThreadedImageExtractor extractorThread : imageExtractorThreads) {
                imageUrls.addAll(extractorThread.getImageUrls());
            }


            filterImages();
            System.out.println("Here are the filtered images: "+ filteredImages);

        } catch (Exception e) {
            System.out.println("Run has failed");
        }
        return filteredImages;
    }
    private void filterImages() {
        for (int x = 0; x < imageUrls.size(); x++) {
            String imageUrl = imageUrls.get(x);
          //  System.out.println(imageUrl);
            if (!filteredImages.contains(imageUrl)) {
                filteredImages.add(imageUrl);
            }
        }

        // Clear the original subPageUrls and add the filtered URLs back
        imageUrls.clear();
    }

    private void filterUrls() {
        for (int x = 0; x < subPageUrls.size(); x++) {
            String subPageUrl = subPageUrls.get(x);
      //      System.out.println(subPageUrl);
            if (!filteredUrls.contains(subPageUrl)) {
                filteredUrls.add(subPageUrl);
            }
        }

        // Clear the original subPageUrls and add the filtered URLs back
        subPageUrls.clear();
    }
//    public List<String> crawlImages(String initialUrl) {
//        visitedUrls = new HashSet<>();
//        visitedImages = new HashSet<>();
//        imageUrls = new ArrayList<>();
//        subPageUrls = new ArrayList<>();
//        baseImageUrl = initialUrl;
//        try {
//            Document document = Jsoup.connect(initialUrl).get();
//            extractUrls(document);
//            extractAndAddImages(document);
//
//            for (int x = 0; x < subPageUrls.size(); x++){
//                String subPageUrl = subPageUrls.get(x);
//                try {
//                    extractUrls(Jsoup.connect(subPageUrl).get());
//                    System.out.println("Fetching images from subpage: " + subPageUrl);
//                    Document subDocument = Jsoup.connect(subPageUrl).get();
//                    extractAndAddImages(subDocument);
//
//                } catch (IOException e) {
//                    System.err.println("Error fetching images from subpage: " + subPageUrl);
//                //    e.printStackTrace();
//                }
//            }
//        } catch (IOException e) {
//          //  e.printStackTrace();
//        }
//        return imageUrls;
//    }

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
