package app.com.deanofthewebb.spotifystreamer.adapter;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.squareup.picasso.Picasso;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
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
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_track, parent, false);
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
            Image trackAlbumImage = (track.album.images.get(track.album.images.size() - 1));
            SafelyLoadImageFromPicasso(icon, trackAlbumImage);
        }
        else{
            icon.setImageResource(R.mipmap.spotify_streamer_launcher);
        }
    }

    private void SafelyLoadImageFromPicasso(ImageView icon, Image trackAlbumImage) {
        try {
            URL url = new URL(trackAlbumImage.url);
            Uri uri = Uri.parse( url.toURI().toString() );

            Picasso.with(getContext())
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
