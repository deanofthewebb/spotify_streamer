package app.com.deanofthewebb.spotifystreamer.helpers;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract;

public class Constants {
    public interface ACTION {
        String MAIN = "app.com.deanofthewebb.spotifystreamer.action.main";
        String CREATE = "app.com.deanofthewebb.spotifystreamer.action.create";
        String SKIP_BACK = "app.com.deanofthewebb.spotifystreamer.action.prev";
        String PLAY = "app.com.deanofthewebb.spotifystreamer.action.play";
        String PAUSE = "app.com.deanofthewebb.spotifystreamer.action.pause";
        String SKIP_FORWARD = "app.com.deanofthewebb.spotifystreamer.action.next";
        String STOP = "app.com.deanofthewebb.spotifystreamer.action.stop";
        String START_FOREGROUND = "app.com.deanofthewebb.spotifystreamer.action.start_foreground";
        String STOP_FOREGROUND = "app.com.deanofthewebb.spotifystreamer.action.stop_foreground";
        String SET_POSITION = "app.com.deanofthewebb.spotifystreamer.action.set_position";
        String UPDATE_VIEW = "app.com.deanofthewebb.spotifystreamer.action.update_view";
        String UPDATE_PROGRESS = "app.com.deanofthewebb.spotifystreamer.action.update_progress";
        String SET_PLAYING_FLAG = "app.com.deanofthewebb.spotifystreamer.action.set_is_playing_flag";
    }


    public interface KEY {
        String INTENT_ACTION = "intent_action_key";
        String TRACK_POSITION = "track_position_key";
        String PROGRESS = "progress_key";
        String SELECTED_ITEM_POSITION = "selected_position_key";
        String LARGE_LAYOUT_FLAG = "is_large_layout_key";
        String DURATION = "duration_key";
        String IS_PLAYING = "is_playing_key";
    }


    public interface FILTER {
        String RECEIVER_INTENT_FILTER = "broadcast_receiver_filter";
    }


    public interface NOTIFICATION_ID {
        int FOREGROUND_SERVICE = 202;
    }


    public interface LOADER_ID {
        int ARTIST_LOADER = 0;
        int TRACK_LOADER_ID = 0;
    }


    public interface TAG {
         String DETAILFRAGMENT = "DFTAG";
    }


    public interface CONTENT_PROVIDER {

        String[] ARTIST_COLUMNS = {
                // In this case the id needs to be fully qualified with a table name, since
                // the content provider joins the artist & track tables in the background
                // (both have an _id column)
                // On the one hand, that's annoying.  On the other, you can search the track table
                // using the artist set by the user, which is only in the Artist table.
                // So the convenience is worth it.
                SpotifyStreamerContract.ArtistEntry.TABLE_NAME + "." + SpotifyStreamerContract.ArtistEntry._ID,
                SpotifyStreamerContract.ArtistEntry.TABLE_NAME + "." + SpotifyStreamerContract.ArtistEntry.COLUMN_NAME,
                SpotifyStreamerContract.ArtistEntry.TABLE_NAME + "." + SpotifyStreamerContract.ArtistEntry.COLUMN_API_ID,
                SpotifyStreamerContract.ArtistEntry.TABLE_NAME + "." + SpotifyStreamerContract.ArtistEntry.COLUMN_API_URI,
                SpotifyStreamerContract.ArtistEntry.TABLE_NAME + "." + SpotifyStreamerContract.ArtistEntry.COLUMN_POPULARITY,
                SpotifyStreamerContract.ArtistEntry.TABLE_NAME + "." + SpotifyStreamerContract.ArtistEntry.COLUMN_IMAGE_URL
        };

        // These indices are tied to ARTIST_COLUMNS.  If ARTIST_COLUMNS changes, these
        // must change.
        int COL_ARTIST_ID = 0;
        int COL_ARTIST_NAME = 1;
        int COL_ARTIST_API_ID = 2;
        int COL_ARTIST_API_URI = 3;
        int COL_ARTIST_POPULARITY = 4;
        int COL_ARTIST_IMAGE_URL = 5;




        String[] TRACK_COLUMNS = {
                // In this case the id needs to be fully qualified with a table name, since
                // the content provider joins the artist & track tables in the background
                // (both have an _id column)
                // On the one hand, that's annoying.  On the other, you can search the track table
                // using the artist set by the user, which is only in the Artist table.
                // So the convenience is worth it.
                SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry._ID,
                SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry.COLUMN_NAME,
                SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry.COLUMN_API_ID,
                SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry.COLUMN_API_URI,
                SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry.COLUMN_POPULARITY,
                SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry.COLUMN_IMAGE_URL,
                SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry.COLUMN_PREVIEW_URL,
                SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry.COLUMN_MARKETS,
                SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry.COLUMN_ALBUM_NAME,
                SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry.COLUMN_ARTIST_KEY
        };

        // These indices are tied to TRACK_COLUMNS.  If TRACK_COLUMNS changes, these
        // must change.
        int COL_TRACK_ID = 0;
        int COL_TRACK_NAME = 1;
        int COL_TRACK_API_ID = 2;
        int COL_TRACK_API_URI = 3;
        int COL_TRACK_POPULARITY = 4;
        int COL_TRACK_IMAGE_URL = 5;
        int COL_TRACK_PREVIEW_URL = 6;
        int COL_TRACK_MARKETS = 7;
        int COL_TRACK_ALBUM_NAME = 8;
        int COL_TRACK_ARTIST_KEY = 9;

    }



    public static class PlaybackFragmentViewHolder {

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


        public PlaybackFragmentViewHolder (View view) {
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



    public static class TrackCursorViewHolder {
        public final ImageView icon;
        public final TextView trackName;
        public final TextView trackAlbumName;

        public TrackCursorViewHolder(View view) {
            trackName = (TextView) view.findViewById(R.id.list_item_track_name);
            trackAlbumName = (TextView) view.findViewById(R.id.list_item_track_album);
            icon = (ImageView) view.findViewById(R.id.list_item_album_art);
        }
    }


    public static class ArtistCursorViewHolder {
        public final ImageView icon;
        public final TextView artistName;

        public ArtistCursorViewHolder(View view) {
            artistName = (TextView) view.findViewById(R.id.list_item_artist_name);
            icon = (ImageView) view.findViewById(R.id.list_item_artist_art);
        }
    }
}
