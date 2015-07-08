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
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;

public class ArtistAdapter extends ArrayAdapter<Artist> {
    private static final String LOG_TAG = ArtistAdapter.class.getSimpleName();

    public ArtistAdapter(Activity context, List<Artist> artists){
        super(context, 0, artists);
    }

    private static class ViewHolder {
        TextView artist_name;
        ImageView icon;
        int position;
    }
    private ViewHolder holder;

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        Artist artist = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.artist_result, parent, false);
            holder = new ViewHolder();
            holder.artist_name = (TextView) convertView.findViewById(R.id.artist_name_textview);
            holder.icon = (ImageView) convertView.findViewById(R.id.artist_image_imageview);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.artist_name.setText(artist.name);
        SetImageView(holder.icon, artist);

        return  convertView;
    }

    private void SetImageView(ImageView icon, Artist artist) {

        if (!artist.images.isEmpty()) {
            Image artistImage = (artist.images.get(artist.images.size() - 1));

            SafelyLoadImageFromPicasso(icon, artistImage);
        }
        else{
            icon.setImageResource(R.mipmap.spotify_streamer_launcher);
        }
    }

    private void SafelyLoadImageFromPicasso(ImageView icon, Image image) {
        try {
            URL url = new URL(image.url);
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
