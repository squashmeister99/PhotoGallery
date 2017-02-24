package com.example.rajesh.photogallery;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import it.sephiroth.android.library.picasso.Picasso;

/**
 * Created by Rajesh on 2/23/2017.
 */

public class FullScreenPhotoFragment extends Fragment {

    private static final String ARG_URI = "photo_page_url3";
    private String mUrl;
    private ImageView mImageView;

    public static FullScreenPhotoFragment newInstance(String url) {
        Bundle args = new Bundle();
        args.putString(ARG_URI, url);
        FullScreenPhotoFragment fragment = new FullScreenPhotoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get a larger resolution image
        mUrl = getArguments().getString(ARG_URI).replace("_s", "_k");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_full_screen, container, false);
        mImageView = (ImageView) v.findViewById(R.id.fragment_full_screen_image);
        // load the image
        Picasso.with(getActivity()).
                load(mUrl).
                placeholder(R.drawable.placeholder)
                .into(mImageView);
        return v;
    }
}
