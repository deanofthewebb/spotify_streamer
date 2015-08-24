package app.com.deanofthewebb.spotifystreamer.fragment;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.Utility;
import app.com.deanofthewebb.spotifystreamer.model.ParceableTrack;
import app.com.deanofthewebb.spotifystreamer.service.PlaybackService;
import kaaes.spotify.webapi.android.models.ArtistSimple;

public class PlaybackFragment extends Fragment implements Runnable {
    private final String LOG_TAG = PlaybackFragment.class.getSimpleName();
    PlaybackService mService;
    boolean mBound = false;

    public static class ViewHolder {

        public final ImageView trackAlbumArt;
        public final TextView trackName;
        public final TextView trackAlbumName;
        public final TextView artistName;
        public final ImageButton playPauseButton;
        public final TextView startTime;
        public final TextView endTime;
        public final SeekBar seekBar;


        public ViewHolder(View view) {
            trackAlbumArt = (ImageView) view.findViewById(R.id.list_item_album_art);
            trackName = (TextView) view.findViewById(R.id.list_item_track_name);
            trackAlbumName = (TextView) view.findViewById(R.id.list_item_track_album);
            artistName = (TextView) view.findViewById(R.id.list_item_artist_name);
            startTime = (TextView) view.findViewById(R.id.playback_track_start);
            endTime = (TextView) view.findViewById(R.id.playback_track_end);
            playPauseButton = (ImageButton) view.findViewById(R.id.play_pause_playback_button);
            seekBar = (SeekBar) view.findViewById(R.id.playback_seek_bar);
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ParceableTrack track = (ParceableTrack) intent.getParcelableExtra("track");
            setChildViews(track);
        }
    };

    @Override
    public void run() {
        int currentPosition= 0;
        int total = mService.mMediaPlayer.getDuration();
        while (mService.mMediaPlayer !=null && currentPosition < total) {
            try {
                Thread.sleep(1000);
                currentPosition= mService.mMediaPlayer.getCurrentPosition();
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                return;
            }
            final ViewHolder viewHolder = new ViewHolder(getView());
            viewHolder.seekBar.setProgress(currentPosition);
        }
    }

    public PlaybackFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Start an intent service to load UI Data and music playback.
        Intent serviceIntent = new Intent(getActivity(), PlaybackService.class);
        serviceIntent.setAction(PlaybackService.ACTION_CREATE);
        serviceIntent.putExtra(ArtistTracksFragment.TRACK_ID_EXTRA,
                getActivity().getIntent().getStringExtra(ArtistTracksFragment.TRACK_ID_EXTRA));
        getActivity().startService(serviceIntent);

        //Bind to service
        getActivity().bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);


        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                new IntentFilter("my-event"));

        this.setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playback, container, false);
        return rootView;
    }

    @Override
    public void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unbind from the service
        if (mBound) {
            mService.mMediaPlayer.stop();
            mService.mMediaPlayer.release();
            getActivity().unbindService(mConnection);
            mBound = false;
            Log.d(LOG_TAG, "Exiting Activity: unBinding from Service: " + mService.toString());
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            PlaybackService.LocalBinder binder = (PlaybackService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };


    public void setChildViews(final ParceableTrack track) {
        final ViewHolder viewHolder = new ViewHolder(getView());

        if (track != null) {
            viewHolder.trackAlbumName.setText(track.album.name);
            viewHolder.trackName.setText(track.name);
            Utility.SetAlbumArt(viewHolder.trackAlbumArt, track, false);


            ArrayList<String> artists = new ArrayList<String>();

            for (ArtistSimple artist : track.artists) { artists.add(artist.name); }
            String artistNames = TextUtils.join(",", artists);

            viewHolder.artistName.setText(artistNames);
            viewHolder.startTime.setText("0:00");

            viewHolder.playPauseButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    //Play or Pause Song, depending on current state
                    if (mService.mMediaPlayer.isPlaying()) {
                        viewHolder.playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                        mService.updateState(PlaybackService.ACTION_PAUSE, track.id);
                    } else {
                        viewHolder.playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                        mService.updateState(PlaybackService.ACTION_PLAY, track.id);
                    }
                }
            });

            viewHolder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (progress > 0) {
                        mService.updateTrackProgress(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });


            int duration = mService.mMediaPlayer.getDuration();

            String formattedDuration = String.format("%d:%2d",
                    TimeUnit.MILLISECONDS.toMinutes(duration),
                    TimeUnit.MILLISECONDS.toSeconds(duration) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
            );
            updateDuration(formattedDuration, duration, viewHolder);

        }
    }

    public void updateDuration(String formattedDuration, int duration, ViewHolder viewHolder){
        viewHolder.endTime.setText(formattedDuration);
        viewHolder.seekBar.setProgress(0);
        viewHolder.seekBar.setMax(duration);
    }
}