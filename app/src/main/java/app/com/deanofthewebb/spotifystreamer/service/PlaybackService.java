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

import app.com.deanofthewebb.spotifystreamer.fragment.ArtistTracksFragment;
import app.com.deanofthewebb.spotifystreamer.model.ParceableTrack;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.RetrofitError;


public class PlaybackService extends IntentService {
    private final String LOG_TAG = PlaybackService.class.getSimpleName();
    private IBinder mBinder;

    public static final String ACTION_PLAY = "action.PLAY";
    public static final String ACTION_PAUSE = "action.PAUSE";
    public static final String ACTION_DESTROY = "action.DESTROY";
    public static final String ACTION_CREATE = "action.CREATE";
    public static final String ACTION_SCRUB = "action.SCRUB";

    public MediaPlayer mMediaPlayer;
    private SpotifyService mSpotifyService;
    public Track mTrack;
    private int mCurrentPosition = 0;


    public PlaybackService() {
        super("PlaybackService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (mBinder == null) {
            mBinder = new LocalBinder();
        }

        return mBinder;
    }

    private void sendDataToReceivers() {
        Intent intent = new Intent("my-event");
        intent.putExtra("track", new ParceableTrack(mTrack.name, mTrack.album.name,
                mTrack.album.images.get(0), mTrack.artists.get(0)));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    public class LocalBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            initializeSpotifyApi();
            initializeTrack(intent);
            updateState(intent.getAction(), intent.getStringExtra(ArtistTracksFragment.TRACK_ID_EXTRA));
        }
    }

    public void updateState(String ACTION, String trackId) {
        switch (ACTION) {
            case ACTION_CREATE:
                if (mMediaPlayer == null) initMediaPlayer();
                else if (trackChanged(trackId)) resetMediaPlayer();
                break;

            case ACTION_PLAY:
                if (mMediaPlayer == null) initMediaPlayer();
                else if (trackChanged(trackId)) resetMediaPlayer();
                mMediaPlayer.seekTo(mCurrentPosition);
                mMediaPlayer.start();
                break;

            case ACTION_PAUSE:
                if (mMediaPlayer != null) {
                    mMediaPlayer.pause();
                    mCurrentPosition = mMediaPlayer.getCurrentPosition();
                }
                break;

            case ACTION_DESTROY:
                if (mMediaPlayer != null) mMediaPlayer.release();
                break;
        }
    }

    public void updateTrackProgress (int progress) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(progress);
            mMediaPlayer.start();
        }
    }

    private void initializeSpotifyApi() {
        if (mSpotifyService == null) {
            SpotifyApi api = new SpotifyApi();
            mSpotifyService = api.getService();
        }
    }

    private void initializeTrack(Intent intent) {
        if (mTrack == null || trackChanged(intent.getStringExtra(ArtistTracksFragment.TRACK_ID_EXTRA))) {
            try {
                    mTrack = mSpotifyService.getTrack(intent.getStringExtra(ArtistTracksFragment.TRACK_ID_EXTRA));
            } catch (RetrofitError re) {
                Log.e(LOG_TAG, "Retrofit error has occured: " + re.getMessage());
            } catch (Exception ex) {
                Log.e(LOG_TAG, "An unexpected error has occured: " + ex.getMessage());
            }
        }
    }

    private boolean trackChanged(String trackId) {
        return !mTrack.id.equals(trackId);
    }


    private void resetMediaPlayer() {
        mMediaPlayer.stop();
        mMediaPlayer.reset();

        try {
            mMediaPlayer.setDataSource(mTrack.preview_url);
            mMediaPlayer.prepare();
        } catch (IOException ioe) {
            Log.d(LOG_TAG, "An error has occured. " + ioe.getMessage());
            Log.e(LOG_TAG, Log.getStackTraceString(ioe));
        }
    }

    private void initMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mMediaPlayer.setDataSource(mTrack.preview_url);
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    sendDataToReceivers();
                }
            });
            mMediaPlayer.prepare();
        } catch (IOException ioe) {
            Log.d(LOG_TAG, "An error has occured. " + ioe.getMessage());
            Log.e(LOG_TAG, Log.getStackTraceString(ioe));
        } catch (IllegalStateException ise) {
            Log.d(LOG_TAG, "An illegal state exception has occured. " + ise.getMessage());
            Log.e(LOG_TAG, Log.getStackTraceString(ise));
        }
    }
}
