package app.com.deanofthewebb.spotifystreamer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.helpers.Constants;
import app.com.deanofthewebb.spotifystreamer.helpers.Utility;
import app.com.deanofthewebb.spotifystreamer.fragment.ArtistSearchFragment;

public class ArtistCursorAdapter extends CursorAdapter {
    private static final String LOG_TAG = ArtistCursorAdapter.class.getSimpleName();

    public ArtistCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        int layoutId = R.layout.list_item_artist;

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        Constants.ArtistCursorViewHolder viewHolder = new Constants.ArtistCursorViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Constants.ArtistCursorViewHolder viewHolder = (Constants.ArtistCursorViewHolder) view.getTag();

        // Read date from cursor
        viewHolder.artistName.setText(cursor.getString(Constants.CONTENT_PROVIDER.COL_ARTIST_NAME));
        SetImageView(viewHolder.icon, cursor.getString(Constants.CONTENT_PROVIDER.COL_ARTIST_IMAGE_URL), context);
    }

    private void SetImageView(ImageView icon, String imageUrl, Context context) {
        if (!imageUrl.isEmpty()) Utility.SafelyLoadImageFromPicasso(icon, imageUrl, true);
        else icon.setImageResource(R.mipmap.spotify_streamer_launcher);
    }


}