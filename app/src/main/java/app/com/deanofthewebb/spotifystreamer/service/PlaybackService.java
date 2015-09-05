package app.com.deanofthewebb.spotifystreamer.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.*;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.activity.MainActivity;
import app.com.deanofthewebb.spotifystreamer.activity.PlaybackActivity;
import app.com.deanofthewebb.spotifystreamer.helpers.Constants;
import app.com.deanofthewebb.spotifystreamer.helpers.Utility;
import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract;
import kaaes.spotify.webapi.android.models.Track;


public class PlaybackService extends Service {
    private final String LOG_TAG = PlaybackService.class.getSimpleName();

    public MediaPlayer mMediaPlayer;
    private Track mTrack;
    private String mTrackRowId;
    private int mCurrentPosition = 0;
    private ServiceHandler mServiceHandler;
    private Timer mTimer;
    private boolean mIsPlaying = true;
    private  NotificationManager mNotifyManager;


    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            mTrackRowId = data.getString(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID);
            setTrack();

            int progress = data.getInt(Constants.KEY.PROGRESS, 0);
            String action = data.getString(Constants.KEY.INTENT_ACTION);
            boolean isLargeLayout = data.getBoolean(Constants.KEY.LARGE_LAYOUT_FLAG);

            if (data.getInt(Constants.KEY.TRACK_POSITION, -1) != -1) {
                mCurrentPosition = data.getInt(Constants.KEY.TRACK_POSITION, -1);
                progress = mCurrentPosition;
            }

            if (mCurrentPosition != -1 && progress == 0) {
                updateState(action, isLargeLayout, mCurrentPosition);
            } else {
                updateState(action, isLargeLayout, progress);
            }
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job

        }
    }


    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread(PlaybackService.class.getSimpleName(),
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        Looper mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle data = new Bundle();

        if (intent.getAction() != null) {
            data.putString(Constants.KEY.INTENT_ACTION, intent.getAction());
        }

        if (intent.hasExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID)) {
            data.putString(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID,
                    intent.getStringExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID));
        }

        if (intent.hasExtra(Constants.KEY.LARGE_LAYOUT_FLAG)) {
            data.putBoolean(Constants.KEY.LARGE_LAYOUT_FLAG,
                    intent.getBooleanExtra(Constants.KEY.LARGE_LAYOUT_FLAG, false));
        }

        if (intent.hasExtra(Constants.KEY.PROGRESS)) {
            data.putInt(Constants.KEY.PROGRESS,
                    intent.getIntExtra(Constants.KEY.PROGRESS, 0));
        }

        if (intent.hasExtra(Constants.KEY.TRACK_POSITION)) {
            data.putInt(Constants.KEY.TRACK_POSITION,
                    intent.getIntExtra(Constants.KEY.TRACK_POSITION, -1));
        }

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.setData(data);
        mServiceHandler.sendMessage(msg);

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Toast.makeText(this, R.string.service_destroy_toast, Toast.LENGTH_SHORT).show();
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void sendDataToReceivers(String ACTION) {
        Intent intent = new Intent(Constants.FILTER.RECEIVER_INTENT_FILTER);

        switch (ACTION) {
            case Constants.ACTION.SET_POSITION:
                intent.putExtra(Constants.KEY.INTENT_ACTION, Constants.ACTION.SET_POSITION);
                intent.putExtra(Constants.KEY.TRACK_POSITION, mMediaPlayer.getCurrentPosition());
                break;

            case Constants.ACTION.UPDATE_VIEW:
                intent.putExtra(Constants.KEY.INTENT_ACTION, Constants.ACTION.UPDATE_VIEW);
                intent.putExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID, mTrackRowId);
                if (mMediaPlayer != null) {
                    intent.putExtra(Constants.KEY.DURATION, mMediaPlayer.getDuration());
                }
                break;
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    private void startTimerTask() {
        int INTERVAL_TIME = 250;
        int DELAY_TRIGGER = 1000;
        if (null == mTimer) mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    sendDataToReceivers(Constants.ACTION.SET_POSITION);
                }
            }
        }, DELAY_TRIGGER, INTERVAL_TIME);}



    private void updateState(String ACTION, boolean isLargeLayout, int progress) {

        String trackImageUrl = mTrack.album.images.get(0).url;
        Bitmap icon = Utility.getBitmapFromURL(trackImageUrl);

        switch (ACTION) {
            case Constants.ACTION.START_FOREGROUND:
                Log.i(LOG_TAG, "Received Start Foreground Intent ");
                Notification notification = buildForegroundNotification(isLargeLayout, ACTION, icon);

                startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                        notification);
                break;

            case Constants.ACTION.CREATE:
                Log.i(LOG_TAG, "Received Create Intent. Resetting the mMediaPlayer ");
                if (mMediaPlayer == null) initMediaPlayer();
                else resetMediaPlayer();

                mMediaPlayer.seekTo(mCurrentPosition);
                mMediaPlayer.start();
                sendDataToReceivers(Constants.ACTION.UPDATE_VIEW);
                startTimerTask();

                mNotifyManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                        buildForegroundNotification(isLargeLayout, ACTION, icon));
                break;

            case Constants.ACTION.UPDATE_VIEW:
                Log.i(LOG_TAG, "Received Create Intent. Resetting the mMediaPlayer ");
                sendDataToReceivers(Constants.ACTION.UPDATE_VIEW);
                mNotifyManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                        buildForegroundNotification(isLargeLayout, ACTION, icon));
                break;

            case Constants.ACTION.PLAY:
                Log.i(LOG_TAG, "Received Play Intent ");
                if (mMediaPlayer == null) initMediaPlayer();
                else resetMediaPlayer();
                if (!mIsPlaying) {
                    mMediaPlayer.seekTo(mCurrentPosition);
                    mMediaPlayer.start();
                    mIsPlaying = true;
                }
                sendDataToReceivers(Constants.ACTION.UPDATE_VIEW);

                trackImageUrl = mTrack.album.images.get(0).url;
                icon = Utility.getBitmapFromURL(trackImageUrl);

                if (mNotifyManager != null) {
                    mNotifyManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                            buildForegroundNotification(isLargeLayout, ACTION, icon));
                }

                break;

            case Constants.ACTION.UPDATE_PROGRESS:
                if (mMediaPlayer != null) {
                    mCurrentPosition = progress;
                    mMediaPlayer.seekTo(progress);
                }

                mNotifyManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                        buildForegroundNotification(isLargeLayout, ACTION, icon));
                break;

            case Constants.ACTION.SKIP_BACK:
                Log.i(LOG_TAG, "Received Skip Back Foreground Intent ");
                if (mMediaPlayer != null) {
                    resetMediaPlayer();
                    initMediaPlayer();
                    mMediaPlayer.start();
                    sendDataToReceivers(Constants.ACTION.UPDATE_VIEW);
                    startTimerTask();
                }

                mNotifyManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                        buildForegroundNotification(isLargeLayout, ACTION, icon));
                break;

            case Constants.ACTION.SKIP_FORWARD:
                Log.i(LOG_TAG, "Received Skip Forward Intent ");
                if (mMediaPlayer != null) {
                    resetMediaPlayer();
                    initMediaPlayer();
                    mMediaPlayer.start();
                    sendDataToReceivers(Constants.ACTION.UPDATE_VIEW);
                    startTimerTask();
                }

                mNotifyManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                        buildForegroundNotification(isLargeLayout, ACTION, icon));
                break;

            case Constants.ACTION.PAUSE:
                Log.i(LOG_TAG, "Received Pause Intent ");
                if (mMediaPlayer != null) {
                    mMediaPlayer.pause();
                    mCurrentPosition = mMediaPlayer.getCurrentPosition();
                    mIsPlaying = false;

                    trackImageUrl = mTrack.album.images.get(0).url;
                    icon = Utility.getBitmapFromURL(trackImageUrl);

                    mNotifyManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                            buildForegroundNotification(isLargeLayout, ACTION, icon));
                }
                break;

            case Constants.ACTION.STOP:
                Log.i(LOG_TAG, "Received Stop Intent ");
                if (mMediaPlayer != null) mMediaPlayer.stop();
                break;

            case Constants.ACTION.STOP_FOREGROUND:
                Log.i(LOG_TAG, "Received Stop Foreground Intent. Stopping Notification ");
                stopForeground(true);
                break;
        }
    }

    private Notification buildForegroundNotification(boolean isLargeLayout, String ACTION,
                                             Bitmap icon) {

        Intent notificationIntent;

        if (isLargeLayout) notificationIntent  = new Intent(this, MainActivity.class);
        else notificationIntent = new Intent(this, PlaybackActivity.class);

        notificationIntent.setAction(ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Intent previousIntent = new Intent(this, PlaybackService.class);
        previousIntent.setAction(Constants.ACTION.SKIP_BACK);
        PendingIntent pPreviousIntent = PendingIntent.getService(this, 0,
                previousIntent, 0);

        Intent playIntent = new Intent(this, PlaybackService.class);
        playIntent.setAction(Constants.ACTION.PAUSE);
        PendingIntent pPlayIntent = PendingIntent.getService(this, 0,
                playIntent, 0);

        Intent pauseIntent = new Intent(this, PlaybackService.class);
        playIntent.setAction(Constants.ACTION.PAUSE);
        PendingIntent pPauseIntent = PendingIntent.getService(this, 0,
                pauseIntent, 0);

        Intent nextIntent = new Intent(this, PlaybackService.class);
        nextIntent.setAction(Constants.ACTION.SKIP_FORWARD);
        PendingIntent pNextIntent = PendingIntent.getService(this, 0,
                nextIntent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);

        notificationBuilder
                .setContentTitle(mTrack.name)
                .setTicker("Spotify Streamer Playing: " + mTrack.name)
                .setContentText(mTrack.artists.get(0).name)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(
                        Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_previous, "Previous",
                        pPreviousIntent);

        if (mIsPlaying) {
            notificationBuilder.addAction(android.R.drawable.ic_media_pause, "Pause",
                   pPauseIntent);
        } else {
            notificationBuilder.addAction(android.R.drawable.ic_media_play, "Play",
                    pPlayIntent);
        }

        if (mMediaPlayer != null ) {
            int progress = mMediaPlayer.getCurrentPosition();
            if (progress > 0) {
                notificationBuilder.setProgress(mMediaPlayer.getDuration(), progress, false);
            }
        }

        Notification notification = notificationBuilder
                .addAction(android.R.drawable.ic_media_next, "Next",
                        pNextIntent).build();
        return notification;
    }


    private void setTrack() {
        try {
            mTrack = Utility.buildTrackFromContentProviderId(getApplicationContext(), mTrackRowId);
            if (mTrack != null) Log.d(LOG_TAG, "Created Track. Id: " + mTrack.id);
        }
        catch (Exception e) { Log.e(LOG_TAG, Log.getStackTraceString(e));}
    }


    private void resetMediaPlayer() {
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        initMediaPlayer();
    }


    private void initMediaPlayer() {
        Log.i(LOG_TAG, "Initializing mMediaPlayer.");
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mMediaPlayer.setDataSource(mTrack.preview_url);
            mMediaPlayer.prepare();
        }
        catch (IOException ioe) { Utility.LogError("An IOException has occured", ioe); }
        catch (IllegalStateException ise) { Utility.LogError("An IllegalStateException has occurred", ise); }
    }
}


