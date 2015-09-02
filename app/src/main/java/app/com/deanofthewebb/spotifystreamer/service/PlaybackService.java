package app.com.deanofthewebb.spotifystreamer.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.*;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.Utility;
import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract;
import app.com.deanofthewebb.spotifystreamer.fragment.PlaybackFragment;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;


public class PlaybackService extends Service {
    private final String LOG_TAG = PlaybackService.class.getSimpleName();

    //MediaPlayer State Actions
    public static final String ACTION_PLAY = "action.PLAY";
    public static final String ACTION_RECONNECT = "action.RECONNECT";
    public static final String ACTION_PAUSE = "action.PAUSE";
    public static final String ACTION_DESTROY = "action.DESTROY";
    public static final String ACTION_CREATE = "action.CREATE";
    public static final String ACTION_STOP = "action.STOP";
    public static final String ACTION_SKIP_BACK = "action.SKIP_BACK";
    public static final String ACTION_SKIP_FORWARD = "action.SKIP_FORWARD";
    public MediaPlayer mMediaPlayer;

    private SpotifyService mSpotifyService;
    private Track mTrack;
    private String mTrackRowId;
    private int mCurrentPosition = 0;
    private static final String INTENT_ACTION_KEY = "intent_action";

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private IBinder mBinder =  new LocalBinder();
    private Timer mTimer;


    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();

            mTrackRowId = data.getString(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID);
            initializeSpotifyApi();
            try { mTrack = Utility.buildTrackFromContentProviderId(getApplicationContext(), mTrackRowId);}
            catch (Exception e) { Log.e(LOG_TAG, Log.getStackTraceString(e));}

            updateState(data.getString(INTENT_ACTION_KEY), mTrack.id);
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
        HandlerThread thread = new HandlerThread("PlaybackService",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle data = new Bundle();
        data.putString(INTENT_ACTION_KEY, intent.getAction());
        data.putString(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID,
                intent.getStringExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID));


        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.setData(data);
        mServiceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_REDELIVER_INTENT;
    }


    @Override
    public void onDestroy() {
        Toast.makeText(this, R.string.service_destroy_toast, Toast.LENGTH_SHORT).show();
    }


    public class LocalBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    public void updateTrackProgress (int progress) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(progress);
        }
    }


    public void sendDataToReceivers(boolean timer, boolean changeSong) {
        Intent intent;
        if (timer) {
            intent = new Intent(PlaybackFragment.RECEIVER_INTENT_FILTER);
            intent.putExtra(PlaybackFragment.TRACK_POSITION, Integer.toString(mMediaPlayer.getCurrentPosition()));

        }
        else {
            intent = new Intent(PlaybackFragment.RECEIVER_INTENT_FILTER);
            intent.putExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID,mTrackRowId);
            intent.putExtra(PlaybackFragment.CHANGE_TRACK, changeSong);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    public void startTimerTask() {
        int INTERVAL_TIME = 250;
        int DELAY_TRIGGER = 1000;
        if (null == mTimer) mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    sendDataToReceivers(true, false);
                }
            }
        }, DELAY_TRIGGER, INTERVAL_TIME);}


    private void initializeSpotifyApi() {
        if (mSpotifyService == null) {
            SpotifyApi api = new SpotifyApi();
            mSpotifyService = api.getService();
        }
    }


    private boolean trackChanged(String trackId) {
        return !mTrack.id.equals(trackId);
    }


    private void resetMediaPlayer() {
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        initMediaPlayer();
    }


    private void initMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mMediaPlayer.setDataSource(mTrack.preview_url);
            mMediaPlayer.prepare();
        }
        catch (IOException ioe) { LogError("An IOException has occured", ioe); }
        catch (IllegalStateException ise) { LogError("An IllegalStateException has occurred", ise); }
    }


    public void updateState(String ACTION, String trackId) {
        //TODO: Restart track and duration once song finishes
        switch (ACTION) {
            case ACTION_CREATE:
                if (mMediaPlayer == null) initMediaPlayer();
                else if (trackChanged(trackId)) resetMediaPlayer();
                mMediaPlayer.seekTo(mCurrentPosition);
                mMediaPlayer.start();
                sendDataToReceivers(false, false);
                startTimerTask();
                break;

            case ACTION_PLAY:
                if (mMediaPlayer == null) initMediaPlayer();
                else if (trackChanged(trackId)) resetMediaPlayer();
                mMediaPlayer.seekTo(mCurrentPosition);
                mMediaPlayer.start();
                break;

            case ACTION_RECONNECT:
                if (mMediaPlayer == null) initMediaPlayer();
                mMediaPlayer.seekTo(mCurrentPosition);
                if (!mMediaPlayer.isPlaying()) mMediaPlayer.start();
                break;

            case ACTION_SKIP_BACK:
                if (mMediaPlayer != null) {
                    resetMediaPlayer();
                    initMediaPlayer();
                    mMediaPlayer.start();
                    sendDataToReceivers(false, true);
                    startTimerTask();
                }
                break;

            case ACTION_SKIP_FORWARD:
                if (mMediaPlayer != null) {
                    resetMediaPlayer();
                    initMediaPlayer();
                    mMediaPlayer.start();
                    sendDataToReceivers(false, true);
                    startTimerTask();
                }
                break;

            case ACTION_PAUSE:
                if (mMediaPlayer != null) {
                    mMediaPlayer.pause();
                    mCurrentPosition = mMediaPlayer.getCurrentPosition();
                }
                break;

            case ACTION_STOP:
                if (mMediaPlayer != null) {
                    mMediaPlayer.stop();
                }
                break;

            case ACTION_DESTROY:
                if (mMediaPlayer != null) {
                    try {
                        mMediaPlayer.stop();
                    } catch (IllegalStateException ise) {Log.e(LOG_TAG, "Illegal State Exception has occurred, ignoring for now.");}
                    mMediaPlayer.release();
                    if (mTimer != null) mTimer.cancel();
                }
                break;
        }
    }


    private void LogError(String Message, Exception ex) {
        Log.v(LOG_TAG, Message + ": " + ex.getMessage());
        Log.e(LOG_TAG, Log.getStackTraceString(ex));
    }
}


