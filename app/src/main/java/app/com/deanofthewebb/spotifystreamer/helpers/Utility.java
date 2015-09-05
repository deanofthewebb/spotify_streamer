package app.com.deanofthewebb.spotifystreamer.helpers;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract;
import app.com.deanofthewebb.spotifystreamer.fragment.ArtistSearchFragment;
import app.com.deanofthewebb.spotifystreamer.fragment.ArtistTracksFragment;
import app.com.deanofthewebb.spotifystreamer.fragment.PlaybackFragment;
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
            Log.d(LOG_TAG, Log.getStackTraceString(ex));
            icon.setImageResource(R.mipmap.spotify_streamer_launcher);
        }
    }


    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.e(LOG_TAG, Log.getStackTraceString(e));
            return null;
        }
    }


    public static void showPlaybackDialog(Activity activity, String trackRowId, boolean isLargeLayout) {
        FragmentManager fragmentManager = activity.getFragmentManager();
        PlaybackFragment fragment = new PlaybackFragment();

        Bundle args = new Bundle();
        args.putString(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID, trackRowId);
        args.putBoolean(Constants.KEY.LARGE_LAYOUT_FLAG, isLargeLayout);
        fragment.setArguments(args);

        if (isLargeLayout) {
            // The device is using a large layout, so show the fragment as a dialog
            fragment.show(fragmentManager, "dialog");
        } else {
            ArtistTracksFragment trackFragment = (ArtistTracksFragment) fragmentManager.findFragmentById(R.id.track_detail_container);
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.remove(trackFragment).addToBackStack(null)
                    .addToBackStack(null);
            transaction.add(R.id.track_detail_container, fragment)
                    .addToBackStack(null).commit();
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager)  context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void ShowNoNetworkFoundToast(Context context) {
        CharSequence text = context.getString(R.string.no_network_found);
        int duration = Toast.LENGTH_LONG;
        Toast.makeText(context, text, duration).show();
    }



    public static Track buildTrackFromContentProviderId(Context context, String trackRowId) throws Exception {
        return convertCursorToTrack(context, SpotifyStreamerContract.TrackEntry._ID + " = ? ", trackRowId);
    }


    public static Track buildTrackFromContentProviderApiId(Context context, String ApiId) throws Exception {
        return convertCursorToTrack(context, SpotifyStreamerContract.TrackEntry.COLUMN_API_ID + " = ? ", ApiId);
    }


    public static Track convertCursorToTrack(Context context, String selection, String selectionArg)  throws Exception {
        Cursor trackCursor = context.getContentResolver().query(
                SpotifyStreamerContract.TrackEntry.CONTENT_URI,
                null,
                selection,
                new String[]{selectionArg},
                null);


        Track track = new Track();
        Album album = new Album();
        Image image = new Image();


        if (trackCursor.moveToNext()) {
            Artist artist = buildArtistFromContentProviderId(context, trackCursor.getString(Constants.CONTENT_PROVIDER.COL_TRACK_ARTIST_KEY));
            track.id = trackCursor.getString(Constants.CONTENT_PROVIDER.COL_TRACK_API_ID);
            track.name = trackCursor.getString(Constants.CONTENT_PROVIDER.COL_TRACK_NAME);
            track.uri = trackCursor.getString(Constants.CONTENT_PROVIDER.COL_TRACK_API_URI);
            track.popularity = Integer.getInteger(trackCursor.getString(6), 10);
            track.preview_url = trackCursor.getString(4);

            album.name = trackCursor.getString(Constants.CONTENT_PROVIDER.COL_TRACK_ALBUM_NAME);
            album.images = new ArrayList<>();
            image.url = trackCursor.getString(7);
            album.images.add(image);

            track.album = album;
            track.artists = new ArrayList<>();
            track.artists.add(artist);
            String[] markets = new String[] {trackCursor.getString(5)};
            track.available_markets = new ArrayList<>(Arrays.asList(markets));

            if (trackCursor.moveToNext()) throw new Exception("More than one row returned in Track Cursor (selectionArg): " + selectionArg);
        } else { throw new Exception("Track Cursor did not return any results for selection Arg: " + selectionArg);}

        trackCursor.close();
        return track;
    }



    public static Artist buildArtistFromContentProviderApiId(Context context, String ApiId) throws Exception {
        return convertCursorToArtist(context, SpotifyStreamerContract.ArtistEntry.COLUMN_API_ID + " = ? ", ApiId);
    }


    public static Artist buildArtistFromContentProviderId(Context context, String artistRowId) throws Exception {
        return  convertCursorToArtist(context, SpotifyStreamerContract.ArtistEntry._ID + " = ? ", artistRowId);
    }


    public static Artist convertCursorToArtist(Context context, String selection, String selectionArgs) throws Exception {
        Artist artist = new Artist();

        Cursor artistCursor = context.getContentResolver().query(
                SpotifyStreamerContract.ArtistEntry.CONTENT_URI,
                null,
                selection,
                new String[] {selectionArgs},
                null);

        if (artistCursor.moveToNext()) {
            artist.id = artistCursor.getString(Constants.CONTENT_PROVIDER.COL_ARTIST_API_ID);
            artist.name = artistCursor.getString(Constants.CONTENT_PROVIDER.COL_ARTIST_NAME);
            artist.uri = artistCursor.getString(Constants.CONTENT_PROVIDER.COL_ARTIST_API_URI);
            artist.popularity = Integer.getInteger(artistCursor.getString(Constants.CONTENT_PROVIDER.COL_ARTIST_POPULARITY), 0);
            Image image = new Image();
            image.url = artistCursor.getString(Constants.CONTENT_PROVIDER.COL_ARTIST_IMAGE_URL);
            artist.images = new ArrayList<>();
            artist.images.add(image);

            if (artistCursor.moveToNext()) throw new Exception("More than one row returned in Artist Cursor (selectionArg): " + selectionArgs);
        } else { throw new Exception("Artist Cursor did not return any results for selectionArgs: " + selectionArgs);}

        artistCursor.close();
        return artist;
    }


    public static void LogError(String Message, Exception ex) {
        Log.e(LOG_TAG, Message + ": " + ex.getMessage());
        Log.e(LOG_TAG, Log.getStackTraceString(ex));
    }
}
