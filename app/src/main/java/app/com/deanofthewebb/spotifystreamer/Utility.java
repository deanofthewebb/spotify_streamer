package app.com.deanofthewebb.spotifystreamer;

import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

public class Utility {
    private static final String LOG_TAG = Utility.class.getSimpleName();

    public static void SetAlbumArt(ImageView icon, Track track, boolean useSmallestArt) {

        int imageIndex = useSmallestArt ? track.album.images.size() - 1 : 0;
        Log.d(LOG_TAG, "Imaged index used: " + imageIndex + " For track: " +track.name);

        if (!track.album.images.isEmpty()) {
            Image trackAlbumImage = track.album.images.get(imageIndex);
            Log.d(LOG_TAG, "Found Image, Url: " + trackAlbumImage.url +
                    " Height: " + trackAlbumImage.height +
                    " Width: " + trackAlbumImage.width);
            SafelyLoadImageFromPicasso(icon, trackAlbumImage, useSmallestArt);
        }
        else{
            Log.d(LOG_TAG, "No Images found, using default..");
            icon.setImageResource(R.mipmap.spotify_streamer_launcher);
        }
    }

    static void SafelyLoadImageFromPicasso(ImageView albumArt, Image trackAlbumImage, boolean useSmallestArt) {
        try {
            URL url = new URL(trackAlbumImage.url);
            Uri uri = Uri.parse( url.toURI().toString() );

            if (useSmallestArt) {
                Picasso.with(albumArt.getContext())
                        .load(uri)
                        .resizeDimen(R.dimen.artist_image_dimen, R.dimen.artist_image_dimen)
                        .centerCrop()
                        .into(albumArt);
            }
            else {
                Picasso.with(albumArt.getContext())
                        .load(uri)
                        .resize(albumArt.getMaxWidth(), albumArt.getMaxHeight())
                        .centerCrop()
                        .into(albumArt);
            }

        }
        catch (MalformedURLException e1) {
            albumArt.setImageResource(R.mipmap.spotify_streamer_launcher);
        }
        catch (URISyntaxException e) {
            albumArt.setImageResource(R.mipmap.spotify_streamer_launcher);
        }
        catch (Exception ex) {
            Log.d(LOG_TAG, "An error has occured. " + ex.getMessage());

            StringBuilder builder = new StringBuilder();
            StackTraceElement[] stackTrace = ex.getStackTrace();
            for (StackTraceElement ste : stackTrace) { builder.append(ste.toString()); }


            Log.d(LOG_TAG, builder.toString());
            albumArt.setImageResource(R.mipmap.spotify_streamer_launcher);
        }
    }
}
