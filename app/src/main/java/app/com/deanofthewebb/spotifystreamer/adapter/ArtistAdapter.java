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
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;

public class ArtistAdapter extends ArrayAdapter<Artist> {
    private static final String LOG_TAG = ArtistAdapter.class.getSimpleName();

    public ArtistAdapter(Activity context, List<Artist> artists){
        super(context, 0, artists);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        Artist artist = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.artist_result, parent, false);
        }

        SetTextView(convertView, artist);
        SetImageView(convertView, artist);

        return  convertView;
    }

    private void SetTextView(View convertView, Artist artist) {
        TextView nameView = (TextView) convertView.findViewById(R.id.artist_name_textview);
        nameView.setText(artist.name);
    }

    private void SetImageView(View convertView, Artist artist) {
        ImageView iconView = (ImageView) convertView.findViewById(R.id.artist_image_imageview);

        if (!artist.images.isEmpty()) {
            Image artistImage = (artist.images.get(0));
            String url = artistImage.url;

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
}
