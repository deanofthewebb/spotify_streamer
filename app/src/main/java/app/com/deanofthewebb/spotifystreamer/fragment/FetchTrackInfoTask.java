package app.com.deanofthewebb.spotifystreamer.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.Utility;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.RetrofitError;

public class FetchTrackInfoTask extends AsyncTask<Context, Void, FetchTrackInfoTask.ViewHolder> {
    private final String LOG_TAG = FetchTrackInfoTask.class.getSimpleName();
    private Artist artist;
    private Track track;


    public static class ViewHolder {

        public final ImageView trackAlbumArt;
        public final TextView trackName;
        public final TextView trackAlbumName;
        public final TextView artistName;


        public ViewHolder(View view) {
            trackAlbumArt = (ImageView) view.findViewById(R.id.list_item_album_art);
            trackName = (TextView) view.findViewById(R.id.list_item_track_name);
            trackAlbumName = (TextView) view.findViewById(R.id.list_item_track_album);
            artistName = (TextView) view.findViewById(R.id.list_item_artist_name);
        }
    }


    @Override
    protected ViewHolder doInBackground(Context... params) {
        SpotifyApi api = new SpotifyApi();
        SpotifyService spotify = api.getService();

        try {
            Activity activity = (Activity) params[0];
            Intent playbackIntent = activity.getIntent();

            artist = spotify.getArtist(playbackIntent.getStringExtra(ArtistTracksFragment.ARTIST_ID_EXTRA));
            track = spotify.getTrack(playbackIntent.getStringExtra(ArtistTracksFragment.TRACK_ID_EXTRA));

            View playbackView = activity.findViewById(R.id.playback_container);

            return new ViewHolder(playbackView);
        }
        catch (RetrofitError re) {
            Log.e(LOG_TAG, "Retrofit error has occured: " + re.getMessage());
            return null;
        }
        catch (Exception ex) {
            Log.e(LOG_TAG, "An error has occured: " + ex.getMessage());
            return null;
        }
    }


    @Override
    protected void onPostExecute(ViewHolder viewHolder) {

        if (track != null) {
            viewHolder.trackAlbumName.setText(track.album.name);
            viewHolder.trackName.setText(track.name);
            Utility.SetAlbumArt(viewHolder.trackAlbumArt, track, false);
        } else {
            Log.e(LOG_TAG, "Track Object is null!");
        }

        if (artist != null) {
            viewHolder.artistName.setText(artist.name);
        } else {
            Log.e(LOG_TAG, "Artist Object is null!");
        }

    }
}
