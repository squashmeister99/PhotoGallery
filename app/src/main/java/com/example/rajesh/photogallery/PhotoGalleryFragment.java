package com.example.rajesh.photogallery;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
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
    private static final int LANDSCAPE_MODE_COLUMNS = 4;
    private static final int PORTRAIT_MODE_COLUMNS = 3;

    private RecyclerView mPhotoRecyclerView = null;
    private RequestQueue mRequestQueue = null;
    GridLayoutManager mGridLayoutManager = null;
    boolean needNewPictures = true;
    int mCurrentDisplayPage = 1; // default to page 1
    FlickrPhotoDatabase mImageDatabase = null;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        Log.i(TAG, "onCreate  called");

        mImageDatabase = FlickrPhotoDatabase.getInstance();
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

        MenuItem toggleitem = menu.findItem(R.id.menu_item_toggle_polling);

        if(isServiceAlarmOn()) {
            toggleitem.setTitle(R.string.stop_polling);
        }
        else {
            toggleitem.setTitle(R.string.start_polling);
        }
    }

    private void updatePhotos() {
        needNewPictures = true;
        mCurrentDisplayPage = 1;
        displayPhotos(mCurrentDisplayPage);
    }

    private boolean isServiceAlarmOn() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return JobSchedulerPollService.isServiceAlarmOn(getActivity());
        }
        else
        {
            return PollService.isServiceAlarmOn(getActivity());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {

            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updatePhotos();
                return true;

            case R.id.menu_item_toggle_polling:
                // toggle the service alarm state
                setServiceAlarm(!isServiceAlarmOn());
                getActivity().invalidateOptionsMenu();
                return true;

                default:
                    return super.onOptionsItemSelected(item);
        }
    }

    private void setServiceAlarm(boolean shouldStartAlarm)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobSchedulerPollService.setServiceAlarm(getActivity(), shouldStartAlarm);
        }
        else
        {
            PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
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

        int numberofColumns = (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) 
                ? LANDSCAPE_MODE_COLUMNS: PORTRAIT_MODE_COLUMNS ;
        
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
                        appendNewImages();
                    }
                }
            }
        });

        Log.i(TAG, "onCreateView called");
        return v;
    }

    private void appendNewImages() {
        //Do pagination.. i.e. fetch new data
        mCurrentDisplayPage++; // increment the page
        needNewPictures = true;
        displayPhotos(mCurrentDisplayPage);
    }
    
    private void setupAdapter() {
        ArrayList<PhotoBean> images = FlickrPhotoDatabase.getInstance().getImages();

        if(isAdded() && images != null) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(images));
            mPhotoRecyclerView.scrollToPosition((mCurrentDisplayPage - 1)*100); // scroll to the correct page
        }
    }

    private void displayPhotos(int page) {

        // if no items have been fetched, call flickr and get the new list
        if(needNewPictures) {
            Response.Listener<PhotoGalleryGSON> listener = new Response.Listener<PhotoGalleryGSON>() {
                @Override
                public void onResponse(PhotoGalleryGSON response) {
                    Log.i(TAG, "onResponse listener  called");
                    
                    if(mCurrentDisplayPage == 1) {
                        mImageDatabase.clear();
                    }

                    mImageDatabase.addImages(response.getPhotos().getPhoto());
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

            // build a request
            GsonRequest<PhotoGalleryGSON> buildGsonRequest = FlickrFetcher.getInstance().buildFlickrQueryRequest(getActivity(), page, listener, errorListerner);
            buildGsonRequest.addMarker(TAG);

            // add to request queue
            mRequestQueue = Volley.newRequestQueue(getActivity());
            mRequestQueue.add(buildGsonRequest);
        }

        setupAdapter();
    }

    @Override
    public void onResume() {
        super.onResume();
        mCurrentDisplayPage = 1;
        displayPhotos(mCurrentDisplayPage);
        Log.i(TAG, "onResume called");
    }

    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageView mImageView;
        private PhotoBean mPhoto;

        public PhotoHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
            itemView.setOnClickListener(this);
        }

        public void bindPhotoBean(PhotoBean item) {
            // store the item
            mPhoto = item;

            Picasso.with(getActivity()).
                    load(mPhoto.getUrl_s()).
                    placeholder(R.drawable.placeholder)
                    .into(mImageView);
        }

        @Override
        public void onClick(View v) {
            Intent i = PhotoPageActivity.newIntent(getActivity(), mPhoto.getPhotoPageUri());
            startActivity(i);
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
