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
        if (!track.album.images.isEmpty()) {
            Image trackAlbumImage = track.album.images.get(imageIndex);
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
            Log.e(LOG_TAG, Log.getStackTraceString(ex));

            albumArt.setImageResource(R.mipmap.spotify_streamer_launcher);
        }
    }
}
