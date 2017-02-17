package com.example.rajesh.photogallery;

import android.content.Context;
import android.net.Uri;

import com.android.volley.Response;

/**
 * Created by Rajesh on 2/13/2017.
 */

public class FlickrFetcher {

    private static final String API_KEY = "8be066e565ea282b21f633235627307e";

    private static final String TAG = "FlickerFetcher";
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    private static FlickrFetcher mInstance = null;

    public static FlickrFetcher getInstance() {
        if(mInstance == null) {
            mInstance = new FlickrFetcher();
        }

        return mInstance;
    }

    private String buildUrl(String method, String query, int page) {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                                .appendQueryParameter("method", method)
                                .appendQueryParameter("page", Integer.toString(page));

        if(method.equals(SEARCH_METHOD)) {
            uriBuilder.appendQueryParameter("text", query);
        }
        return uriBuilder.build().toString();
    }

    GsonRequest<PhotoGalleryGSON> buildFlickrQueryRequest(Context context, int page, Response.Listener<PhotoGalleryGSON> listener, Response.ErrorListener errorListener) {
        String query = QueryPreferences.getStoredQuery(context);
        if(query == null) {
            return downloadRecentPhotos(page, listener, errorListener);
        }
        else  {
            return searchPhotos(query, page, listener, errorListener);
        }
    }

    private GsonRequest<PhotoGalleryGSON> searchPhotos(String query, int page, Response.Listener<PhotoGalleryGSON> listener, Response.ErrorListener errorListener) {

            String url = buildUrl(SEARCH_METHOD, query, page);
            return new GsonRequest<PhotoGalleryGSON>
                    (url, PhotoGalleryGSON.class, null, listener, errorListener);
    }

    private GsonRequest<PhotoGalleryGSON> downloadRecentPhotos(int page, Response.Listener<PhotoGalleryGSON> listener, Response.ErrorListener errorListener) {

        String url = buildUrl(FETCH_RECENTS_METHOD, null, page);
        return new GsonRequest<PhotoGalleryGSON>
                (url, PhotoGalleryGSON.class, null, listener, errorListener);
    }
}
