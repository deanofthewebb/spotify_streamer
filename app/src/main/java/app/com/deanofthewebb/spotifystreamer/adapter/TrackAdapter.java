package app.com.deanofthewebb.spotifystreamer.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import java.util.List;

import app.com.deanofthewebb.spotifystreamer.R;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

public class TrackAdapter extends ArrayAdapter<Track> {
    private static final String LOG_TAG = TrackAdapter.class.getSimpleName();

    public TrackAdapter(Activity context, List<Track> tracks){
        super(context, 0, tracks);
    }

    private static class ViewHolder {
        TextView track_name;
        TextView track_album_name;
        ImageView icon;
        int position;
    }
    private ViewHolder holder;

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        Track track = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.track_result, parent, false);
            holder = new ViewHolder();
            holder.track_name = (TextView) convertView.findViewById(R.id.track_name_textview);
            holder.track_album_name = (TextView) convertView.findViewById(R.id.track_album_textview);
            holder.icon = (ImageView) convertView.findViewById(R.id.track_album_imageview);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.track_name.setText(track.name);
        holder.track_album_name.setText(track.album.name);
        SetAlbumImage(holder.icon, track);

        return  convertView;
    }

    private void SetAlbumImage(ImageView icon, Track track) {

        if (!track.album.images.isEmpty()) {
            Image trackAlbumImage = (track.album.images.get(0));
            String url = trackAlbumImage.url;

            Picasso.with(getContext())
                    .load(url)
                    .resizeDimen(R.dimen.artist_image_dimen, R.dimen.artist_image_dimen)
                    .centerCrop()
                    .into(icon);
        }
        else{
            icon.setImageResource(R.mipmap.spotify_streamer_launcher);
        }
    }
}
