package com.example.rajesh.photogallery;

import android.net.Uri;
import android.util.Log;

import com.example.rajesh.photogallery.GalleryItemGson.PhotosBean.PhotoBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rajesh on 2/13/2017.
 */

public class FlickrFetcher {

    private static final String API_KEY = "8be066e565ea282b21f633235627307e";

    private static final String TAG = "FlickerFetcher";

    public List<GalleryItem> fetchItems() {

        List<GalleryItem> items = new ArrayList<>();

        try {
            String url = Uri.parse("https://api.flickr.com/services/rest/").buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s")
                    .build().toString();
            String jsonString = getUrlString(url);

            Log.i(TAG, "Received JSON: " + jsonString);

            //JSONObject jsonBody = new JSONObject(jsonString);
            //parseItems(items, jsonBody);
            parseItemsGson(items, jsonString);

        } catch (JsonParseException je) {
            Log.e(TAG, "failed to parse json", je);

        } catch (IOException ioe) {
            Log.e(TAG, "failed to fetch items", ioe);
        }

        return items;
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody)
            throws IOException, JSONException {

        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

        for(int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

            GalleryItem item = new GalleryItem();
            item.setCaption(photoJsonObject.getString("title"));
            item.setId(photoJsonObject.getString("id"));

            if(!photoJsonObject.has("url_s")) {
                continue;
            }

            item.setUrl(photoJsonObject.getString("url_s"));
            items.add(item);
        }

    }

    private void parseItemsGson(List<GalleryItem> items, String jsonString)
    {
        Gson gson = new GsonBuilder().create();
        GalleryItemGson gsonItems = gson.fromJson(jsonString, GalleryItemGson.class);

        List<PhotoBean> photoList = gsonItems.getPhotos().getPhoto();

        for(int i =0; i < photoList.size(); i++) {
            GalleryItem item = new GalleryItem();
            item.setId(photoList.get(i).getId());
            item.setUrl(photoList.get(i).getUrl_s());
            item.setCaption(photoList.get(i).getId());

            items.add(item);
        }

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
