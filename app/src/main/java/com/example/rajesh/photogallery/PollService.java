package com.example.rajesh.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.example.rajesh.photogallery.PhotoGalleryGSON.PhotosBean.PhotoBean;

import java.util.List;

/**
 * Created by Rajesh on 2/19/2017.
 */

public class PollService extends IntentService {

    private static final String TAG = "PollService";
    private static final int POLL_INTERVAL = 1000*5; // 60 sec;

    public static boolean isServiceAlarmOn(Context context) {
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);
        return pi != null;
    }

    public static void setServiceAlarm(Context context, boolean isOn) {
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if(isOn) {
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), POLL_INTERVAL, pi);
        }
        else {
            alarmManager.cancel(pi);
            pi.cancel();;
        }
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);
    }

    public PollService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(!isNetworkAvailableAndConnected()) return;
        final String lastResultId = QueryPreferences.getLAstResultId(this);

        Response.Listener<PhotoGalleryGSON> listener = new Response.Listener<PhotoGalleryGSON>() {
            @Override
            public void onResponse(PhotoGalleryGSON response) {
                Log.i(TAG, "onResponse listener  called");

                List<PhotoBean> newImages = response.getPhotos().getPhoto();
                if(newImages.isEmpty()) return;

                String resultId = newImages.get(0).getId();

                if(resultId.equals(lastResultId)) {
                    Log.i(TAG, "Got old result id: " + resultId);
                }
                else
                {
                    Log.i(TAG, "Got a new result: " + resultId);
                    QueryPreferences.setLastResultId(getApplicationContext(), resultId);

                    Resources resources = getResources();

                    Intent i = PhotoGalleryActivity.newIntent(getApplicationContext());
                    PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, i, 0);

                    Notification notification = new NotificationCompat.Builder(getApplicationContext())
                            .setTicker(resources.getString(R.string.new_pictures_title))
                            .setSmallIcon(android.R.drawable.ic_menu_report_image)
                            .setContentTitle(resources.getString(R.string.new_pictures_title))
                            .setContentText(resources.getString(R.string.new_pictures_text))
                            .setContentIntent(pi)
                            .setAutoCancel(true)
                            .build();

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                    notificationManager.notify(0, notification);
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "volley error", error);
            }
        };

        GsonRequest<PhotoGalleryGSON> gsonRequest = FlickrFetcher.getInstance().buildFlickrQueryRequest(this, 1, listener, errorListener);

        gsonRequest.addMarker(TAG);
        Volley.newRequestQueue(this).add(gsonRequest);

        Log.i(TAG, "Received an intent: " + intent);
    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();
        return isNetworkConnected;

    }
}
