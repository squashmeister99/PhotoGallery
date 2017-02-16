package com.example.rajesh.photogallery;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.example.rajesh.photogallery.PhotoGalleryGSON.PhotosBean.PhotoBean;

import java.util.ArrayList;
import java.util.List;

import it.sephiroth.android.library.picasso.Picasso;


/**
 * Created by Rajesh on 1/31/2017.
 */

public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private List<PhotoBean> mItems = null;
    private RequestQueue mRequestQueue;
    boolean needNewPictures = true;
    GridLayoutManager mGridLayoutManager = null;
    int mPage = 1; // default to 1 page at a time;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        Log.i(TAG, "onCreate  called");
        mItems = new ArrayList<>();

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                QueryPreferences.setStoredQuery(getActivity(), query);
                updatePhotos();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void updatePhotos() {
        needNewPictures = true;
        mPage = 1;
        mItems.clear();
        displayPhotos(mPage);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {

            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updatePhotos();
                return true;

                default:
                    return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        // cancel all requests for this
        mRequestQueue.cancelAll(TAG);
        Log.i(TAG, "onStop called");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);

        int numberofColumns = 3;
        if(getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)  {
            numberofColumns = 4;
        }

        mGridLayoutManager = new GridLayoutManager(getActivity(), numberofColumns);
        mPhotoRecyclerView.setLayoutManager(mGridLayoutManager);
        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int visibleItemCount, totalItemCount, pastVisiblesItems;
                if(dy > 0) //check for scroll down
                {
                    visibleItemCount = mGridLayoutManager.getChildCount();
                    totalItemCount = mGridLayoutManager.getItemCount();
                    pastVisiblesItems = mGridLayoutManager.findFirstVisibleItemPosition();

                    if ( (visibleItemCount + pastVisiblesItems) >= totalItemCount)
                    {
                        Log.v(TAG, "Last Item Wow !");
                        //Do pagination.. i.e. fetch new data
                        mPage++; // increment the page
                        needNewPictures = true;
                        displayPhotos(mPage);
                    }
                }
            }
        });

        Log.i(TAG, "onCreateView called");
        return v;
    }

    private void setupAdapter() {
        if(isAdded() && mItems != null) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
            mPhotoRecyclerView.scrollToPosition((mPage - 1)*100); // scroll to the correct page
        }
    }

    private void displayPhotos(int page) {

        // if no items have been fetched, call flickr and get the new list
        if(needNewPictures == true) {
            Response.Listener<PhotoGalleryGSON> listener = new Response.Listener<PhotoGalleryGSON>() {
                @Override
                public void onResponse(PhotoGalleryGSON response) {
                    Log.i(TAG, "onResponse listener  called");
                    mItems.addAll(response.getPhotos().getPhoto());
                    setupAdapter();
                    needNewPictures = false;
                }
            };

            Response.ErrorListener errorListerner = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "volley error", error);
                }
            };

            FlickrFetcher instance = FlickrFetcher.getInstance();
            mRequestQueue = Volley.newRequestQueue(getActivity());
            GsonRequest<PhotoGalleryGSON> buildGsonRequest;

            String query = QueryPreferences.getStoredQuery(getActivity());
            if(query == null) {
               buildGsonRequest = instance.downloadRecentPhotos(page, listener, errorListerner);
            }
            else  {
                buildGsonRequest = instance.searchPhotos(query, page, listener, errorListerner);
            }

            buildGsonRequest.addMarker(TAG);
            mRequestQueue.add(buildGsonRequest);
        }

        // setup adapter
        setupAdapter();
    }

    @Override
    public void onResume() {
        super.onResume();
        mPage = 1;
        displayPhotos(mPage);
        Log.i(TAG, "onResume called");
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {

        private ImageView mImageView;

        public PhotoHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
        }

        public void bindPhotoBean(PhotoBean item) {
            Picasso.with(getActivity()).
                    load(item.getUrl_s()).
                    placeholder(R.drawable.placeholder)
                    .into(mImageView);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<PhotoBean> mGalleryItems;
        public PhotoAdapter(List<PhotoBean> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            holder.bindPhotoBean(mGalleryItems.get(position));
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }
}
