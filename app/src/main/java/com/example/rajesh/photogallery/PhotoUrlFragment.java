package com.example.rajesh.photogallery;


import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Rajesh on 2/23/2017.
 */

public class PhotoUrlFragment extends Fragment {

    private static final String ARG_URI = "photo_page_url2";
    private Uri mUri;
    private TextView mTextView;

    public static PhotoUrlFragment newInstance(Uri uri) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, uri);
        PhotoUrlFragment fragment = new PhotoUrlFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUri = getArguments().getParcelable(ARG_URI);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_url, container, false);
        mTextView = (TextView) v.findViewById(R.id.fragment_photo_url_text_view);
        mTextView.setText(mUri.toString());
        return v;
    }
}
