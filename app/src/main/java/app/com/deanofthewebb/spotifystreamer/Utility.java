package app.com.deanofthewebb.spotifystreamer;

import android.content.Context;
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

    public static void SafelyLoadImageFromPicasso(ImageView icon, String imageUrl, Context context) {
        try {
            URL url = new URL(imageUrl);
            Uri uri = Uri.parse( url.toURI().toString() );

            Picasso.with(context)
                    .load(uri)
                    .resizeDimen(R.dimen.artist_image_dimen, R.dimen.artist_image_dimen)
                    .centerCrop()
                    .into(icon);
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
}
