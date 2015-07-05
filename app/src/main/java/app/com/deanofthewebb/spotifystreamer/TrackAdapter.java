package app.com.deanofthewebb.spotifystreamer;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import java.util.List;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

public class TrackAdapter extends ArrayAdapter<Track> {
    private static final String LOG_TAG = TrackAdapter.class.getSimpleName();

    public TrackAdapter(Activity context, List<Track> tracks){
        super(context, 0, tracks);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        Track track = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.track_result, parent, false);
        }

        SetTrackName(convertView, track.name);
        SetTrackAlbumName(convertView, track.album.name);
        SetAlbumImage(convertView, track);

        return  convertView;
    }

    private void SetAlbumImage(View convertView, Track track) {
        ImageView iconView = (ImageView) convertView.findViewById(R.id.track_album_imageview);

        if (!track.album.images.isEmpty()) {
            Image trackAlbumImage = (track.album.images.get(0));
            String url = trackAlbumImage.url;

            Picasso.with(getContext())
                    .load(url)
                    .resizeDimen(R.dimen.artist_image_dimen, R.dimen.artist_image_dimen)
                    .centerCrop()
                    .into(iconView);
        }
        else{
            iconView.setImageResource(R.mipmap.spotify_streamer_launcher);
        }
    }

    private void SetTrackName(View convertView, String track_name) {
        TextView trackNameView = (TextView) convertView.findViewById(R.id.track_name_textview);
        trackNameView.setText(track_name);
    }

    private void SetTrackAlbumName(View convertView, String trackAlbumName) {
        TextView trackAlbumView = (TextView) convertView.findViewById(R.id.track_album_textview);
        trackAlbumView.setText(trackAlbumName);
    }

}
