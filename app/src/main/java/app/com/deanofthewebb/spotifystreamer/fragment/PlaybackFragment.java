package app.com.deanofthewebb.spotifystreamer.fragment;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
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
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.Utility;
import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract;
import app.com.deanofthewebb.spotifystreamer.model.ParceableTrack;
import app.com.deanofthewebb.spotifystreamer.service.PlaybackService;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

public class PlaybackFragment extends Fragment {
    private final String LOG_TAG = PlaybackFragment.class.getSimpleName();
    PlaybackService mPlaybackService;
    boolean mServiceBounded = false;
    public static final String RECEIVER_INTENT_FILTER = "my-event";
    public static final String TRACK_POSITION = "position_data";
    private ViewHolder mViewHolder;
    private Track mTrack;
    private Track mBackupTrack;

    public static class ViewHolder {

        public final ImageView trackAlbumArt;
        public final TextView trackName;
        public final TextView trackAlbumName;
        public final TextView artistName;
        public final ImageButton playPauseButton;
        public final TextView startTime;
        public final TextView endTime;
        public final SeekBar seekBar;
        public final ImageButton previousTrackButton;
        public final ImageButton nextTrackButton;


        public ViewHolder(View view) {
            trackAlbumArt = (ImageView) view.findViewById(R.id.list_item_album_art);
            trackName = (TextView) view.findViewById(R.id.list_item_track_name);
            trackAlbumName = (TextView) view.findViewById(R.id.list_item_track_album);
            artistName = (TextView) view.findViewById(R.id.list_item_artist_name);
            startTime = (TextView) view.findViewById(R.id.playback_track_start);
            endTime = (TextView) view.findViewById(R.id.playback_track_end);
            playPauseButton = (ImageButton) view.findViewById(R.id.play_pause_playback_button);
            previousTrackButton = (ImageButton) view.findViewById(R.id.previous_playback_button);
            nextTrackButton = (ImageButton) view.findViewById(R.id.next_playback_button);
            seekBar = (SeekBar) view.findViewById(R.id.playback_seek_bar);
        }
    }

    private BroadcastReceiver mTrackDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.hasExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID)) {
                String trackRowId = intent.getStringExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID);

                try { mTrack = Utility.buildTrackFromContentProvider(context, trackRowId);}
                catch (Exception e) { e.printStackTrace(); }

                setChildViews();
            }
            else if (intent.hasExtra(TRACK_POSITION)) {
                int currentPosition = Integer.parseInt(intent.getStringExtra(TRACK_POSITION));
                mViewHolder.seekBar.setProgress(currentPosition);
                mViewHolder.startTime.setText(formatDuration(currentPosition));
            }
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            PlaybackService.LocalBinder binder = (PlaybackService.LocalBinder) service;
            mPlaybackService = binder.getService();
            mServiceBounded = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mServiceBounded = false;
        }
    };


    public PlaybackFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String trackRowId = (savedInstanceState != null)
                    ? savedInstanceState.getString(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID)
                    : getActivity().getIntent().getStringExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID);


        Intent serviceIntent = new Intent(getActivity(), PlaybackService.class);
        serviceIntent
                .setAction(PlaybackService.ACTION_CREATE)
                .putExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID, trackRowId);

        getActivity().startService(serviceIntent);

        getActivity().bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mTrackDataReceiver,
                new IntentFilter(RECEIVER_INTENT_FILTER));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playback, container, false);
        mViewHolder = new ViewHolder(rootView);
        return rootView;
    }

    @Override
    public void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mTrackDataReceiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        // Reregister since the activity is visible
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mTrackDataReceiver,
                new IntentFilter(RECEIVER_INTENT_FILTER));
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID, mTrack.id);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unbind from the service
        if (mServiceBounded) {
            mPlaybackService.updateState(PlaybackService.ACTION_DESTROY, null);
            getActivity().unbindService(mServiceConnection);
            mServiceBounded = false;
            Log.v(LOG_TAG, "Exiting Activity: unBinding from Service: " + mPlaybackService.toString());
        }
    }

    public void setChildViews() {
        if (mTrack != null) {

            mViewHolder.trackAlbumName.setText(mTrack.album.name);
            mViewHolder.trackName.setText(mTrack.name);
            SetAlbumArt(mViewHolder.trackAlbumArt, mTrack, false);

            ArrayList<String> artists = new ArrayList<>();
            for (ArtistSimple artist : mTrack.artists) { artists.add(artist.name); }
            String artistNames = TextUtils.join(",", artists);

            mViewHolder.artistName.setText(artistNames);
            mViewHolder.startTime.setText("0:00");
            mViewHolder.playPauseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPlaybackService.mMediaPlayer.isPlaying()) {
                        mViewHolder.playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                        mPlaybackService.updateState(PlaybackService.ACTION_PAUSE, null);
                    } else {
                        mViewHolder.playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                        mPlaybackService.updateState(PlaybackService.ACTION_PLAY, mTrack.id);
                    }
                }
            });


            mViewHolder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) mPlaybackService.updateTrackProgress(progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            int duration = mPlaybackService.mMediaPlayer.getDuration();
            String formattedDuration = formatDuration(duration);
            mViewHolder.endTime.setText(formattedDuration);
            mViewHolder.seekBar.setProgress(0);
            mViewHolder.seekBar.setVisibility(ProgressBar.VISIBLE);
            mViewHolder.seekBar.setMax(duration);

            mViewHolder.previousTrackButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ShowToBeImplementedToast();
                }
            });

        } else { Log.v(LOG_TAG, "No Track Found!"); }
    }

    private void ShowToBeImplementedToast() {
        CharSequence text = "This button will be implemented.. eventually";
        int duration = Toast.LENGTH_LONG;
        Toast.makeText(getActivity(), text, duration).show();
    }


    private String formatDuration(int duration){
        return String.format("%d:%2d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
        );
    }

    private void SetAlbumArt(ImageView icon, Track track, boolean useSmallestArt) {
        if (!track.album.images.isEmpty()) {
            Utility.SafelyLoadImageFromPicasso(icon, track.album.images.get(0).url, useSmallestArt);
        }
        else{
            Log.d(LOG_TAG, "No Images found, using default..");
            icon.setImageResource(R.mipmap.spotify_streamer_launcher);
        }
    }
}