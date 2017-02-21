package com.example.rajesh.photogallery;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import java.util.List;

/**
 * Created by Rajesh on 2/21/2017.
 */

@TargetApi(21)
public class JobSchedulerPollService extends JobService {

    private static final String TAG = "JobSchedulerPollService";
    private static final int JOB_ID = 85050;

    public static boolean isServiceAlarmOn(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        boolean hasBeenScheduled = false;
        for(JobInfo info : scheduler.getAllPendingJobs()) {
            if (info.getId() == JOB_ID) {
                hasBeenScheduled = true;
            }
        }

        return hasBeenScheduled;
    }

    public static void setServiceAlarm(Context context, boolean isOn) {
        Log.i(TAG, "setServiceAlarm: " + isOn);

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if(isOn) {
            // schedule the job
            JobInfo jobInfo = new JobInfo.Builder(JOB_ID, new ComponentName(context, JobSchedulerPollService.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                    .setPeriodic(1000 * 60)
                    .setPersisted(true)
                    .build();

            int result = scheduler.schedule(jobInfo);
            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "Job scheduled successfully!");
            }
        }
        else {
            // cancel the job
            scheduler.cancel(JOB_ID);
        }
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        final String lastResultId = QueryPreferences.getLAstResultId(this);

        Response.Listener<PhotoGalleryGSON> listener = new Response.Listener<PhotoGalleryGSON>() {
            @Override
            public void onResponse(PhotoGalleryGSON response) {
                Log.i(TAG, "onResponse listener  called");

                List<PhotoGalleryGSON.PhotosBean.PhotoBean> newImages = response.getPhotos().getPhoto();
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
                    jobFinished(params, false);
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

        Log.i(TAG, "Received an call to start job service");
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // return true indicates that we want the interrupted job to be rescheduled
        return true;
    }
}
