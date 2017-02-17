package com.example.rajesh.photogallery;

import com.example.rajesh.photogallery.PhotoGalleryGSON.PhotosBean.PhotoBean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rajesh on 2/17/2017.
 */

public class FlickrPhotoDatabase {

    private static FlickrPhotoDatabase sDatabase;
    ArrayList<PhotoBean> mImageList;

    public static FlickrPhotoDatabase getInstance() {
        if(sDatabase == null) {
            sDatabase = new FlickrPhotoDatabase();
        }
        return sDatabase;
    }

    public ArrayList<PhotoBean> getImages() {
        return mImageList;
    }

    public void addImages(List<PhotoBean> imageList) {
        mImageList.addAll(imageList);
    }

    public void clear() {
        mImageList.clear();
    }

    private FlickrPhotoDatabase()
    {
        mImageList = new ArrayList<>();
    }

}
