package com.example.rajesh.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;

import com.example.rajesh.photogallery.PhotoGalleryGSON.PhotosBean.PhotoBean;

import java.util.List;

/**
 * Created by Rajesh on 2/22/2017.
 */

public class PhotoPagerActivity extends FragmentActivity {

    private ViewPager mViewPager;
    private List<PhotoBean> mPhotos;

    public static Intent newIntent(Context context, Uri photoPageUri) {
        Intent i = new Intent(context, PhotoPagerActivity.class);
        i.setData(photoPageUri);
        return i;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_gallery_pager);

        mViewPager = (ViewPager) findViewById(R.id.photo_gallery_view_pager);
        mPhotos = FlickrPhotoDatabase.getInstance().getImages();
        FragmentManager fm = getSupportFragmentManager();
        mViewPager.setAdapter(new FragmentStatePagerAdapter(fm) {
            @Override
            public Fragment getItem(int position) {
                PhotoBean photo = mPhotos.get(position);
                return PhotoUrlFragment.newInstance(photo.getPhotoPageUri());
            }

            @Override
            public int getCount() {
                return mPhotos.size();
            }
        });

        Uri photoUri = getIntent().getData();

        for(int i = 0; i < mPhotos.size(); i++) {
            if (photoUri.toString().equals(mPhotos.get(i).getPhotoPageUri().toString())) {
                mViewPager.setCurrentItem(i);
                break;
            }
        }

    }
}
