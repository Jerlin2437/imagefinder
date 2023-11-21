

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

public class ImageCrawler {
    public List<String> imageUrls;
    public List<String> subPageUrls;
    public Set<String> visitedUrls;
    public Set<String> visitedImages;
    private String baseImageUrl;
    int urlCrawlStatus;

    public List<String> crawlImages(String initialUrl) {

        return imageUrls;
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
    private void extractUrls(Document document) {
        Elements links = document.select("a[href]");
        subPageUrls.addAll(links.stream()
                .map(link -> link.absUrl("href"))
                .filter(url -> isSameDomain(baseImageUrl, url) && !visitedUrls.contains(url))
                .peek(visitedUrls::add)  // Add the URL to the visitedUrls set
                .collect(Collectors.toList()));
    }
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
