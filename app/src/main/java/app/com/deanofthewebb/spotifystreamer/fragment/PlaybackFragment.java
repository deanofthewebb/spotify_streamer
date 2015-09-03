package app.com.deanofthewebb.spotifystreamer.fragment;

import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.Utility;
import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract;
import app.com.deanofthewebb.spotifystreamer.service.PlaybackService;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Track;

public class PlaybackFragment extends DialogFragment {
    private final String LOG_TAG = PlaybackFragment.class.getSimpleName();
    PlaybackService mPlaybackService;
    boolean mServiceBounded = false;

    public static final String RECEIVER_INTENT_FILTER = "my_event";
    public static final String TRACK_POSITION = "position_data";
    public static final String CHANGE_TRACK = "change_track";

    private ViewHolder mViewHolder;
    private Track mTrack;
    private String mTrackRowId;

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
                try { mTrack = Utility.buildTrackFromContentProviderId(context, trackRowId);}
                catch (Exception e) { e.printStackTrace(); }

                Intent serviceIntent = new Intent(getActivity(), PlaybackService.class);
                serviceIntent.setAction(PlaybackService.ACTION_CREATE);
                getActivity().bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

                if (intent.hasExtra(CHANGE_TRACK) && intent.getBooleanExtra(CHANGE_TRACK, true)) {
                    setChildViews();
                }
            }
            else if (intent.hasExtra(TRACK_POSITION)){

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
            setChildViews();
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
        Intent serviceIntent;
        if (savedInstanceState != null) {
            try {
                mTrack =  Utility.buildTrackFromContentProviderApiId(getActivity(),
                        savedInstanceState.getString(SpotifyStreamerContract.TrackEntry.COLUMN_API_ID));

                Cursor trackCursor = getActivity().getContentResolver().query(
                        SpotifyStreamerContract.TrackEntry.CONTENT_URI,
                        null,
                        SpotifyStreamerContract.TrackEntry.COLUMN_API_ID + " = ? ",
                        new String[]{mTrack.id},
                        null);

                trackCursor.moveToNext();
                mTrackRowId = trackCursor.getString(ArtistTracksFragment.COL_TRACK_ID);
                trackCursor.close();

            } catch (Exception e) { e.printStackTrace(); }

            serviceIntent = getServiceIntent();
            getActivity().bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            Bundle arguments = getArguments();
            if (arguments != null) {
                mTrackRowId = arguments.getString(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID);
            } else {
                mTrackRowId = getActivity().getIntent().getStringExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID);
            }

            serviceIntent = getServiceIntent();
            getActivity().startService(serviceIntent);
        }

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mTrackDataReceiver,
                new IntentFilter(RECEIVER_INTENT_FILTER));

        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_playback, container, false);
        mViewHolder = new ViewHolder(rootView);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
        outState.putString(SpotifyStreamerContract.TrackEntry.COLUMN_API_ID, mTrack.id);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unbind from the service
        if (mServiceBounded) {
            getActivity().unbindService(mServiceConnection);
            mServiceBounded = false;
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
            mViewHolder.startTime.setText(formatDuration(0));
            mViewHolder.playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
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

            mViewHolder.previousTrackButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (mPlaybackService.mMediaPlayer.isPlaying()) {

                        String previousTrackRowId = getPreviousTrackRowId();
                        Intent serviceIntent = new Intent(getActivity(), PlaybackService.class);
                        serviceIntent
                                .setAction(PlaybackService.ACTION_SKIP_BACK)
                                .putExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID, previousTrackRowId);

                        getActivity().startService(serviceIntent);
                    }
                }
            });

            mViewHolder.nextTrackButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String nextTrackRowId = getNextTrackRowId();
                    Intent serviceIntent = new Intent(getActivity(), PlaybackService.class);
                    serviceIntent
                            .setAction(PlaybackService.ACTION_SKIP_FORWARD)
                            .putExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID, nextTrackRowId);

                    getActivity().startService(serviceIntent);
                }
            });

            int duration = mPlaybackService.mMediaPlayer.getDuration();
            String formattedDuration = formatDuration(duration);

            mViewHolder.endTime.setText(formattedDuration);
            mViewHolder.seekBar.setProgress(mPlaybackService.mMediaPlayer.getCurrentPosition());
            mViewHolder.seekBar.setVisibility(ProgressBar.VISIBLE);
            mViewHolder.seekBar.setMax(duration);
        } else { Log.e(LOG_TAG, "No Track Found!"); }
    }


    @NonNull
    private Intent getServiceIntent() {
        Intent serviceIntent;
        serviceIntent = new Intent(getActivity(), PlaybackService.class);
        serviceIntent.setAction(PlaybackService.ACTION_CREATE)
                .putExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID, mTrackRowId);
        return serviceIntent;
    }


    private String getNextTrackRowId() {
        String nextTrackRowID = "";
        Cursor tracklistCursor = getActivity().getContentResolver().query(
                SpotifyStreamerContract.TrackEntry.buildTrackArtist(mTrack.artists.get(0).id),
                null,
                null,
                null,
                SpotifyStreamerContract.TrackEntry.COLUMN_POPULARITY + " DESC"
        );

        boolean foundPosition = false;
        int position = -1;
        String trackId;
        while (tracklistCursor.moveToNext() && !foundPosition) {
            trackId = tracklistCursor.getString(ArtistTracksFragment.COL_TRACK_API_ID);

            if (trackId.equals(mTrack.id)) {
                position = tracklistCursor.getPosition();
                foundPosition = true;
            }
        }

        if (position < 0 ) Log.e(LOG_TAG, "Could not find Track in Track List!");

        tracklistCursor.moveToPosition(position);
        //Get next id
        if (tracklistCursor.isLast()) tracklistCursor.moveToFirst();
        else tracklistCursor.moveToNext();
        nextTrackRowID = tracklistCursor.getString(ArtistTracksFragment.COL_TRACK_ID);
        tracklistCursor.close();

        // Change mTrack using previous id
        try {
            mTrack = Utility.buildTrackFromContentProviderId(getActivity(), nextTrackRowID);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.e(LOG_TAG, Log.getStackTraceString(e));
        }
        return  nextTrackRowID;
    }


    private String getPreviousTrackRowId() {
        String previousTrackRowID = "";
        Cursor tracklistCursor = getActivity().getContentResolver().query(
                SpotifyStreamerContract.TrackEntry.buildTrackArtist(mTrack.artists.get(0).id),
                null,
                null,
                null,
                SpotifyStreamerContract.TrackEntry.COLUMN_POPULARITY + " DESC"
        );

        boolean foundPosition = false;
        int position = -1;
        String trackId;
        while (tracklistCursor.moveToNext() && !foundPosition) {
            trackId = tracklistCursor.getString(ArtistTracksFragment.COL_TRACK_API_ID);

            if (trackId.equals(mTrack.id)) {
                position = tracklistCursor.getPosition();
                foundPosition = true;
            }
        }

        if (position < 0 ) Log.e(LOG_TAG, "Could not find Track in Track List!");
            tracklistCursor.moveToPosition(position);
            //Get previous id
            if (tracklistCursor.isFirst()) tracklistCursor.moveToLast();
            else tracklistCursor.moveToPrevious();
            previousTrackRowID = tracklistCursor.getString(ArtistTracksFragment.COL_TRACK_ID);
            tracklistCursor.close();

            // Change mTrack using previous id
            try {
                mTrack = Utility.buildTrackFromContentProviderId(getActivity(), previousTrackRowID);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                Log.e(LOG_TAG, Log.getStackTraceString(e));
            }
        return  previousTrackRowID;
    }


    private String formatDuration(int duration){
        Date curDateTime = new Date(duration);
        SimpleDateFormat formatter = new SimpleDateFormat("m:ss", Locale.US);
        return formatter.format(curDateTime);
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