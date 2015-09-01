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
import app.com.deanofthewebb.spotifystreamer.Utility;
import app.com.deanofthewebb.spotifystreamer.fragment.ArtistTracksFragment;

public class TrackCursorAdapter extends CursorAdapter {
    private static final String LOG_TAG = ArtistCursorAdapter.class.getSimpleName();

    public static class ViewHolder {
        public final ImageView icon;
        public final TextView trackName;
        public final TextView trackAlbumName;

        public ViewHolder(View view) {
            trackName = (TextView) view.findViewById(R.id.list_item_track_name);
            trackAlbumName = (TextView) view.findViewById(R.id.list_item_track_album);
            icon = (ImageView) view.findViewById(R.id.list_item_album_art);
        }
    }

    public TrackCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int layoutId = R.layout.list_item_track;

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.trackName.setText(cursor.getString(ArtistTracksFragment.COL_TRACK_NAME));
        viewHolder.trackAlbumName.setText(cursor.getString(ArtistTracksFragment.COL_TRACK_ALBUM_NAME));
        SetAlbumArt(viewHolder.icon, cursor.getString(ArtistTracksFragment.COL_TRACK_IMAGE_URL), context);
    }

    private static void SetAlbumArt(ImageView icon, String trackUrl, Context context) {

        if (!"".equals(trackUrl)) {
            Utility.SafelyLoadImageFromPicasso(icon, trackUrl, context);
        }
        else{
            Log.d(LOG_TAG, "No Images found, using default..");
            icon.setImageResource(R.mipmap.spotify_streamer_launcher);
        }
    }
}
