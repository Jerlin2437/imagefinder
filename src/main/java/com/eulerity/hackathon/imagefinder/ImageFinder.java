package com.eulerity.hackathon.imagefinder;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@WebServlet(
		name = "ImageFinder",
		urlPatterns = {"/main"}
)
public class ImageFinder extends HttpServlet {
	private static final long serialVersionUID = 1L;
	protected static final Gson GSON = new GsonBuilder().create();

	@Override
	protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/json");

		// Get the URL parameter from the request
		String url = req.getParameter("url");

		// Check if the URL parameter is provided
		if (url != null && !url.isEmpty()) {
			try {

				// Create a new ImageCrawler instance
				ImageCrawler imageCrawler = new ImageCrawler();
				List<String> imageUrls = imageCrawler.crawlImages(url);

				// Convert the list of image URLs to JSON and write it to the response
				resp.getWriter().print(GSON.toJson(imageUrls));
			} catch (IOException e) {
				e.printStackTrace();
				resp.getWriter().print(GSON.toJson("Error fetching images from the specified URL."));
			}
		} else {
			// If no URL parameter is provided, return an error message in JSON
			resp.getWriter().print(GSON.toJson("No URL parameter provided."));
		}
	}
}
