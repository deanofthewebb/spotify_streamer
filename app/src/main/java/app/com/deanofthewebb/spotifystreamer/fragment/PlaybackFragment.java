package app.com.deanofthewebb.spotifystreamer.fragment;

import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.helpers.Constants;
import app.com.deanofthewebb.spotifystreamer.helpers.Utility;
import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract;
import app.com.deanofthewebb.spotifystreamer.service.PlaybackService;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Track;

public class PlaybackFragment extends DialogFragment {
    private final String LOG_TAG = PlaybackFragment.class.getSimpleName();

    private Constants.PlaybackFragmentViewHolder mViewHolder;
    private Track mTrack;
    private String mTrackRowId;
    private boolean mIsPlaying = true;

    private BroadcastReceiver mTrackDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getStringExtra(Constants.KEY.INTENT_ACTION)) {
                case Constants.ACTION.SET_POSITION:
                    if (intent.hasExtra(Constants.KEY.TRACK_POSITION)){
                        int currentPosition = intent.getIntExtra(Constants.KEY.TRACK_POSITION, 0);
                        mViewHolder.seekBar.setProgress(currentPosition);
                        mViewHolder.startTime.setText(formatDuration(currentPosition));
                    }
                    break;

                case Constants.ACTION.UPDATE_VIEW:
                    if (intent.hasExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID)) {
                        mTrackRowId = intent.getStringExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID);
                        int duration = intent.getIntExtra(Constants.KEY.DURATION, 0);
                        try {mTrack = Utility.buildTrackFromContentProviderId(context, mTrackRowId);}
                        catch (Exception e) { e.printStackTrace(); }
                        updateView(duration);
                    }
                    break;
            }
        }
    };



    public PlaybackFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mTrackDataReceiver,
                new IntentFilter(Constants.FILTER.RECEIVER_INTENT_FILTER));

        Bundle arguments = getArguments();

        if (arguments != null) {
            mTrackRowId = arguments.getString(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID);
            boolean largeLayout =  arguments.getBoolean(Constants.KEY.LARGE_LAYOUT_FLAG);

            getActivity().startService(getServiceIntent(Constants.ACTION.START_FOREGROUND)
                    .putExtra(Constants.KEY.LARGE_LAYOUT_FLAG, largeLayout));

            getActivity().startService(getServiceIntent(Constants.ACTION.CREATE));
        }
        else if (savedInstanceState != null) {
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
                mTrackRowId = trackCursor.getString(Constants.CONTENT_PROVIDER.COL_TRACK_ID);
                trackCursor.close();

            } catch (Exception e) { e.printStackTrace(); }

            boolean largeLayout = savedInstanceState.
            getBoolean(Constants.KEY.LARGE_LAYOUT_FLAG);

            getActivity().startService(getServiceIntent(Constants.ACTION.START_FOREGROUND)
                    .putExtra(Constants.KEY.LARGE_LAYOUT_FLAG, largeLayout));

            getActivity().startService(getServiceIntent(Constants.ACTION.CREATE));
        }
        else {
                mTrackRowId = getActivity().getIntent().getStringExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID);
                getActivity().startService(getServiceIntent(Constants.ACTION.CREATE));
        }

        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_playback, container, false);
        mViewHolder = new Constants.PlaybackFragmentViewHolder(rootView);
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
                new IntentFilter(Constants.FILTER.RECEIVER_INTENT_FILTER));
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(SpotifyStreamerContract.TrackEntry.COLUMN_API_ID, mTrack.id);
        super.onSaveInstanceState(outState);
    }


    public void updateView(int duration) {
        if (mTrack != null) {

            mViewHolder.trackAlbumName.setText(mTrack.album.name);
            mViewHolder.trackName.setText(mTrack.name);
            SetAlbumArt(mViewHolder.trackAlbumArt, mTrack, false);

            ArrayList<String> artists = new ArrayList<>();
            for (ArtistSimple artist : mTrack.artists) { artists.add(artist.name); }
            String artistNames = TextUtils.join(", ", artists);

            mViewHolder.artistName.setText(artistNames);
            mViewHolder.startTime.setText(formatDuration(0));
            mViewHolder.playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            mViewHolder.playPauseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mIsPlaying) {
                        getActivity().startService(getServiceIntent(Constants.ACTION.PAUSE));
                        mViewHolder.playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                        mIsPlaying = false;
                    } else {
                        getActivity().startService(getServiceIntent(Constants.ACTION.PLAY));
                        mViewHolder.playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                        mIsPlaying = true;
                    }
                }
            });


            mViewHolder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        mViewHolder.seekBar.setProgress(progress);
                        Intent serviceIntent = getServiceIntent(Constants.ACTION.UPDATE_PROGRESS)
                                .putExtra(Constants.KEY.PROGRESS, progress);
                        getActivity().startService(serviceIntent);
                    }
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
                    String previousTrackRowId = getPreviousTrackRowId();
                    Intent serviceIntent = new Intent(getActivity(), PlaybackService.class);
                    serviceIntent
                            .setAction(Constants.ACTION.SKIP_BACK)
                            .putExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID,
                                    previousTrackRowId);

                    getActivity().startService(serviceIntent);
                }
            });

            mViewHolder.nextTrackButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String nextTrackRowId = getNextTrackRowId();
                    Intent serviceIntent = new Intent(getActivity(), PlaybackService.class);
                    serviceIntent
                            .setAction(Constants.ACTION.SKIP_FORWARD)
                            .putExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID,
                                    nextTrackRowId);

                    getActivity().startService(serviceIntent);
                }
            });

            String formattedDuration = formatDuration(duration);

            mViewHolder.endTime.setText(formattedDuration);
            mViewHolder.seekBar.setVisibility(ProgressBar.VISIBLE);
            mViewHolder.seekBar.setMax(duration);
        } else { Log.e(LOG_TAG, "No Track Found!"); }
    }


    @NonNull
    private Intent getServiceIntent(String Action) {
        Intent serviceIntent;
        serviceIntent = new Intent(getActivity(), PlaybackService.class);
        serviceIntent.setAction(Action)
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
            trackId = tracklistCursor.getString(Constants.CONTENT_PROVIDER.COL_TRACK_API_ID);

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
        nextTrackRowID = tracklistCursor.getString(Constants.CONTENT_PROVIDER.COL_TRACK_ID);
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
            trackId = tracklistCursor.getString(Constants.CONTENT_PROVIDER.COL_TRACK_API_ID);

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
            previousTrackRowID = tracklistCursor.getString(Constants.CONTENT_PROVIDER.COL_TRACK_ID);
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