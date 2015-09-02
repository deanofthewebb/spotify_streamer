package app.com.deanofthewebb.spotifystreamer;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract;
import app.com.deanofthewebb.spotifystreamer.fragment.ArtistSearchFragment;
import app.com.deanofthewebb.spotifystreamer.fragment.ArtistTracksFragment;
import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

public class Utility {
    private static final String LOG_TAG = Utility.class.getSimpleName();

    public static void SafelyLoadImageFromPicasso(ImageView icon, String imageUrl, boolean useSmallestArt) {
        try {
            URL url = new URL(imageUrl);
            Uri uri = Uri.parse( url.toURI().toString() );

            if (useSmallestArt) {
                Picasso.with(icon.getContext())
                        .load(uri)
                        .resizeDimen(R.dimen.artist_image_dimen, R.dimen.artist_image_dimen)
                        .centerCrop()
                        .into(icon);
            }
            else {
                Picasso.with(icon.getContext())
                        .load(uri)
                        .resize(icon.getMaxWidth(), icon.getMaxHeight())
                        .centerCrop()
                        .into(icon);
            }
        }
        catch (MalformedURLException e1) {
            icon.setImageResource(R.mipmap.spotify_streamer_launcher);
        }
        catch (URISyntaxException e) {
            icon.setImageResource(R.mipmap.spotify_streamer_launcher);
        }
        catch (Exception ex) {
            Log.d(LOG_TAG, "An error has occured" + ex.getMessage());
            Log.d(LOG_TAG, ex.getStackTrace().toString());
            icon.setImageResource(R.mipmap.spotify_streamer_launcher);
        }
    }

    public static Track buildTrackFromContentProvider(Context context, String trackRowId) throws Exception {

        Log.v(LOG_TAG, "TRACKROWID: " + trackRowId);


        Cursor trackCursor = context.getContentResolver().query(
                SpotifyStreamerContract.TrackEntry.CONTENT_URI,
                null,
                SpotifyStreamerContract.TrackEntry._ID + " = ? ",
                new String[]{trackRowId},
                null);


        Track track = new Track();
        Album album = new Album();
        Image image = new Image();


       if (trackCursor.moveToFirst()) {
           Artist artist = buildArtistFromContentProvider(context, trackCursor.getString(ArtistTracksFragment.COL_TRACK_ARTIST_KEY));
           track.id = trackCursor.getString(ArtistTracksFragment.COL_TRACK_API_ID);
           track.name = trackCursor.getString(ArtistTracksFragment.COL_TRACK_NAME);
           track.uri = trackCursor.getString(ArtistTracksFragment.COL_TRACK_API_URI);
           track.popularity = Integer.getInteger(trackCursor.getString(6), 10);
           track.preview_url = trackCursor.getString(4);

           album.name = trackCursor.getString(ArtistTracksFragment.COL_TRACK_ALBUM_NAME);
           album.images = new ArrayList<>();
           image.url = trackCursor.getString(7);
           album.images.add(image);

           track.album = album;
           track.artists = new ArrayList<>();
           //track.artists.add(artist);
           String[] markets = new String[] {trackCursor.getString(5)};
           track.available_markets = new ArrayList<>(Arrays.asList(markets));

           if (trackCursor.moveToNext()) throw new Exception("More than one row returned in Track Cursor (row id): " + trackRowId);
       } else { throw new Exception("Track Cursor did not return any results for row id: " + trackRowId);}

        trackCursor.close();
        return track;
    }


    public static Artist buildArtistFromContentProvider(Context context, String artistRowId) throws Exception {
        Artist artist = new Artist();

        Log.v(LOG_TAG, "ARTISTROWID: " + artistRowId);
        Cursor artistCursor = context.getContentResolver().query(
                SpotifyStreamerContract.ArtistEntry.CONTENT_URI,
                null,
                SpotifyStreamerContract.ArtistEntry._ID + " = ? ",
                new String[]{artistRowId},
                null);

        if (artistCursor.moveToFirst()) {
            artist.id = artistCursor.getString(ArtistSearchFragment.COL_ARTIST_API_ID);
            artist.name = artistCursor.getString(ArtistSearchFragment.COL_ARTIST_NAME);
            artist.uri = artistCursor.getString(ArtistSearchFragment.COL_ARTIST_API_URI);
            artist.popularity = Integer.getInteger(artistCursor.getString(ArtistSearchFragment.COL_ARTIST_POPULARITY), 0);
            Image image = new Image();
            image.url = artistCursor.getString(ArtistSearchFragment.COL_ARTIST_IMAGE_URL);
            artist.images = new ArrayList<>();
            artist.images.add(image);

            if (artistCursor.moveToNext()) throw new Exception("More than one row returned in Artist Cursor (row id): " + artistRowId);
        } else { throw new Exception("Artist Cursor did not return any results for row id: " + artistRowId);}

        artistCursor.close();
        return artist;
    }
}
