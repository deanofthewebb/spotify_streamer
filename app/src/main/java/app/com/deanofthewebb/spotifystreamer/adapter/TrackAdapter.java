package app.com.deanofthewebb.spotifystreamer.adapter;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.Utility;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

public class TrackAdapter extends ArrayAdapter<Track> {
    private static final String LOG_TAG = TrackAdapter.class.getSimpleName();

    public TrackAdapter(Activity context, List<Track> tracks){
        super(context, 0, tracks);
    }

    private static class ViewHolder {
        TextView trackName;
        TextView trackAlbumName;
        ImageView icon;
    }
    private ViewHolder holder;

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        Track track = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_track, parent, false);
            holder = new ViewHolder();
            holder.trackName = (TextView) convertView.findViewById(R.id.list_item_track_name);
            holder.trackAlbumName = (TextView) convertView.findViewById(R.id.list_item_track_album);
            holder.icon = (ImageView) convertView.findViewById(R.id.list_item_album_art);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.trackName.setText(track.name);
        holder.trackAlbumName.setText(track.album.name);
        Utility.SetAlbumArt(holder.icon, track, true);




        return  convertView;
    }
}
