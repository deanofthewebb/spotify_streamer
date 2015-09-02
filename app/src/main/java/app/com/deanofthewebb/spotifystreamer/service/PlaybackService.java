package app.com.deanofthewebb.spotifystreamer.service;

import android.app.IntentService;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import app.com.deanofthewebb.spotifystreamer.Utility;
import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract;
import app.com.deanofthewebb.spotifystreamer.fragment.PlaybackFragment;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;


public class PlaybackService extends IntentService {
    private final String LOG_TAG = PlaybackService.class.getSimpleName();

    public static final String ACTION_PLAY = "action.PLAY";
    public static final String ACTION_PAUSE = "action.PAUSE";
    public static final String ACTION_DESTROY = "action.DESTROY";
    public static final String ACTION_CREATE = "action.CREATE";
    public static final String ACTION_STOP = "action.STOP";
    public static final String ACTION_SKIP_BACK = "action.SKIP_BACK";
    public static final String ACTION_SKIP_FORWARD = "action.SKIP_FORWARD";

    public MediaPlayer mMediaPlayer;
    private IBinder mBinder;
    private SpotifyService mSpotifyService;
    private Track mTrack;
    private String mTrackRowId;
    private int mCurrentPosition = 0;
    Timer mTimer;


    public class LocalBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }


    public PlaybackService() { super("PlaybackService"); }


    @Override
    public IBinder onBind(Intent intent) {
        if (mBinder == null) {
            mBinder = new LocalBinder();
        }

        return mBinder;
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            mTrackRowId = intent.getStringExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID);
            initializeSpotifyApi();

            try { mTrack = Utility.buildTrackFromContentProviderId(getApplicationContext(), mTrackRowId);}
            catch (Exception e) { Log.e(LOG_TAG, Log.getStackTraceString(e));}
        }
        updateState(intent.getAction(), mTrack.id);
    }


    public void updateState(String ACTION, String trackId) {
        //TODO: Restart track and duration once song finishes
        switch (ACTION) {
            case ACTION_CREATE:
                if (mMediaPlayer == null) initMediaPlayer();
                else if (trackChanged(trackId)) resetMediaPlayer();
                mMediaPlayer.seekTo(mCurrentPosition);
                mMediaPlayer.start();
                break;

            case ACTION_PLAY:
                if (mMediaPlayer == null) initMediaPlayer();
                else if (trackChanged(trackId)) resetMediaPlayer();
                mMediaPlayer.seekTo(mCurrentPosition);
                mMediaPlayer.start();
                break;

            case ACTION_SKIP_BACK:
                if (mMediaPlayer != null) {
                    resetMediaPlayer();
                    initMediaPlayer();
                    mMediaPlayer.start();
                }
                break;

            case ACTION_SKIP_FORWARD:
                if (mMediaPlayer != null) {
                    resetMediaPlayer();
                    initMediaPlayer();
                    mMediaPlayer.start();
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
                    } catch (IllegalStateException ise) {Log.v(LOG_TAG, "Illegal State Exception has occurred, ignoring for now.");}
                    mMediaPlayer.release();
                    if (mTimer != null) mTimer.cancel();
                }
                break;
        }
    }


    public void updateTrackProgress (int progress) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(progress);
        }
    }


    public void sendDataToReceivers(boolean timer) {
        Intent intent;
        if (timer) {
            intent = new Intent(PlaybackFragment.RECEIVER_INTENT_FILTER);
            intent.putExtra(PlaybackFragment.TRACK_POSITION, Integer.toString(mMediaPlayer.getCurrentPosition()));
        } else {
            intent = new Intent(PlaybackFragment.RECEIVER_INTENT_FILTER);
            intent.putExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID,mTrackRowId);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    public void startTimerTask() {
        if (null == mTimer) mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    sendDataToReceivers(true);
                }
            }
        }, 1000, 1000);}


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
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    sendDataToReceivers(false);
                    startTimerTask();
                }
            });
            mMediaPlayer.prepare();
        }
        catch (IOException ioe) { LogError("An IOException has occured", ioe); }
        catch (IllegalStateException ise) { LogError("An IllegalStateException has occurred", ise); }
    }


    private void LogError(String Message, Exception ex) {
        Log.v(LOG_TAG, Message + ": " + ex.getMessage());
        Log.e(LOG_TAG, Log.getStackTraceString(ex));
    }
}