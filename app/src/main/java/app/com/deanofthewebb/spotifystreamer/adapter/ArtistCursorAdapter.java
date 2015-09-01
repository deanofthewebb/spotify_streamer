package app.com.deanofthewebb.spotifystreamer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.fragment.ArtistSearchFragment;

public class ArtistCursorAdapter extends CursorAdapter {
    private static final String LOG_TAG = ArtistCursorAdapter.class.getSimpleName();

    public static class ViewHolder {
        public final ImageView icon;
        public final TextView artistName;

        public ViewHolder(View view) {
            artistName = (TextView) view.findViewById(R.id.list_item_artist_name);
            icon = (ImageView) view.findViewById(R.id.list_item_artist_art);
        }
    }

    public ArtistCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        int layoutId = R.layout.list_item_artist;

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        // Read date from cursor
        viewHolder.artistName.setText(cursor.getString(ArtistSearchFragment.COL_ARTIST_NAME));
        SetImageView(viewHolder.icon, cursor.getString(ArtistSearchFragment.COL_ARTIST_IMAGE_URL), context);
    }

    private void SetImageView(ImageView icon, String imageUrl, Context context) {
        if (!imageUrl.isEmpty()) SafelyLoadImageFromPicasso(icon, imageUrl, context);
        else icon.setImageResource(R.mipmap.spotify_streamer_launcher);
    }

    private void SafelyLoadImageFromPicasso(ImageView icon, String imageUrl, Context context) {
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