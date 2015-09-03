package app.com.deanofthewebb.spotifystreamer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.helpers.Constants;
import app.com.deanofthewebb.spotifystreamer.helpers.Utility;
import app.com.deanofthewebb.spotifystreamer.fragment.ArtistTracksFragment;

public class TrackCursorAdapter extends CursorAdapter {
    private static final String LOG_TAG = ArtistCursorAdapter.class.getSimpleName();

    public TrackCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int layoutId = R.layout.list_item_track;

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        Constants.TrackCursorViewHolder viewHolder = new Constants.TrackCursorViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Constants.TrackCursorViewHolder viewHolder = (Constants.TrackCursorViewHolder) view.getTag();

        viewHolder.trackName.setText(cursor.getString(Constants.CONTENT_PROVIDER.COL_TRACK_NAME));
        viewHolder.trackAlbumName.setText(cursor.getString(Constants.CONTENT_PROVIDER.COL_TRACK_ALBUM_NAME));
        SetAlbumArt(viewHolder.icon, cursor.getString(Constants.CONTENT_PROVIDER.COL_TRACK_IMAGE_URL), true);
    }

    private static void SetAlbumArt(ImageView icon, String trackUrl, Boolean useSmallestArt) {

        if (!"".equals(trackUrl)) {
            Utility.SafelyLoadImageFromPicasso(icon, trackUrl, useSmallestArt);
        }
        else{
            Log.d(LOG_TAG, "No Images found, using default..");
            icon.setImageResource(R.mipmap.spotify_streamer_launcher);
        }
    }
}
