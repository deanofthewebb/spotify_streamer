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
import app.com.deanofthewebb.spotifystreamer.fragment.PlaybackFragment;
import app.com.deanofthewebb.spotifystreamer.model.ParceableTrack;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.RetrofitError;


public class PlaybackService extends IntentService {
    private final String LOG_TAG = PlaybackService.class.getSimpleName();

    public static final String ACTION_PLAY = "action.PLAY";
    public static final String ACTION_PAUSE = "action.PAUSE";
    public static final String ACTION_DESTROY = "action.DESTROY";
    public static final String ACTION_CREATE = "action.CREATE";

    public MediaPlayer mMediaPlayer;
    private IBinder mBinder;
    private SpotifyService mSpotifyService;
    private Track mTrack;
    private int mCurrentPosition = 0;


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
                if (mMediaPlayer != null) {
                    mMediaPlayer.stop();
                    mMediaPlayer.release();
                }
                break;
        }
    }

    public void updateTrackProgress (int progress) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(progress);
            mMediaPlayer.start();
        }
    }


    private void sendDataToReceivers() {
        Intent intent = new Intent(PlaybackFragment.RECEIVER_INTENT_FILTER);
        intent.putExtra(PlaybackFragment.TRACK_DATA, new ParceableTrack(mTrack.name, mTrack.album.name,
                mTrack.album.images.get(0), mTrack.artists.get(0)));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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
                    mTrack = mSpotifyService.
                            getTrack(intent.getStringExtra(ArtistTracksFragment.TRACK_ID_EXTRA));
            }
            catch (RetrofitError re) { LogError("A retrofit error has occured", re); }
            catch (Exception ex) { LogError("An unexpected error has occured", ex); }
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
        } catch (IOException ioe) { LogError("An IOException has occured", ioe); }
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
        }
        catch (IOException ioe) { LogError("An IOException has occured", ioe); }
        catch (IllegalStateException ise) { LogError("An IllegalStateException has occured", ise); }
    }


    private void LogError(String Message, Exception ex) {
        Log.v(LOG_TAG, Message + ": " + ex.getMessage());
        Log.e(LOG_TAG, Log.getStackTraceString(ex));
    }
}
