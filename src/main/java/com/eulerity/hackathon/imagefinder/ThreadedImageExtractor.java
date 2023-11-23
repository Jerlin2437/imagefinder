package com.eulerity.hackathon.imagefinder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ThreadedImageExtractor implements Runnable{
    private List<String> imageUrls;
    private Set<String> visitedImages;
    private String url;
    private Thread thread;
    public ThreadedImageExtractor(String url) {
        this.url = url;
        imageUrls = new ArrayList<>();
        visitedImages = new HashSet<>();
        this.thread = new Thread(this);
    }

    @Override
    public void run() {
        try {
            extractAndAddImages(Jsoup.connect(url).get());
        } catch (IOException e) {
            System.err.println("Error fetching images from subpage: " + url);
        }
    }
    private void extractAndAddImages(Document document) {
        Elements images = document.select("img");
        imageUrls.addAll(images.stream()
                .map(img -> img.absUrl("src"))
                .filter(url -> !visitedImages.contains(url))
                .peek(visitedImages::add)  // Add the image URL to the visitedImages set
                .collect(Collectors.toList()));

        // Print the image URLs to the console
        System.out.println(imageUrls);
        // imageUrls.forEach(System.out::println);
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public Thread getThread() {
        return thread;
    }
}
