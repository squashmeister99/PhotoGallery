package com.example.rajesh.photogallery;

import android.net.Uri;

import com.android.volley.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Rajesh on 2/13/2017.
 */

public class FlickrFetcher {

    private static final String API_KEY = "8be066e565ea282b21f633235627307e";

    private static final String TAG = "FlickerFetcher";

    private static FlickrFetcher mInstance = null;

    public static FlickrFetcher getInstance() {
        if(mInstance == null) {
            mInstance = new FlickrFetcher();
        }

        return mInstance;
    }

    GsonRequest<PhotoGalleryGSON> buildGsonRequest(int page, Response.Listener<PhotoGalleryGSON> listener, Response.ErrorListener errorListener) {

            String url = Uri.parse("https://api.flickr.com/services/rest/").buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s")
                    .appendQueryParameter("page", Integer.toString(page))
                    .build().toString();

            return new GsonRequest<PhotoGalleryGSON>
                    (url, PhotoGalleryGSON.class, null, listener, errorListener);

    }

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + " : with " + urlSpec);
            }

            int bytesRead = 0;
            byte buffer[] = new byte[1024];
            while ((bytesRead =  in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return  out.toByteArray();

        } finally {
            connection.disconnect();
        }
    }

    public String getUrlString( String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }
}
