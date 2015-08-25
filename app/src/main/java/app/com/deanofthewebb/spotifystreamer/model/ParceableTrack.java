package app.com.deanofthewebb.spotifystreamer.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

public class ParceableTrack extends Track implements Parcelable {
    @Override
    public int describeContents() {
        return 0;
    }

    public ParceableTrack() {
        super();

        if (album == null) {
            album = new Album();
        }

        album.images = new ArrayList<Image>();
        artists = new ArrayList<ArtistSimple>();
    }

    public ParceableTrack(String trackName, String albumName, Image image, ArtistSimple artist) {
        this();

        name = trackName;
        album.name = albumName;

        if (image != null) {
            album.images.add(image);
        }

        if (artist != null) {
            artists.add(artist);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (!album.images.isEmpty()) {
            Image image = album.images.get(0);
            dest.writeStringArray(new String[] {name, album.name, album.uri, image.url, artists.get(0).name});
            dest.writeIntArray(new int[] {image.width, image.height});
        }
        else {
            dest.writeStringArray(new String[]{name, album.name});
        }
    }

    public static final Parcelable.Creator<ParceableTrack> CREATOR
            = new Parcelable.Creator<ParceableTrack>() {
        public ParceableTrack createFromParcel(Parcel in) {
            return new ParceableTrack(in);
        }

        public ParceableTrack[] newArray(int size) {
            return new ParceableTrack[size];
        }
    };

    private ParceableTrack(Parcel in) {
        String[] trackValues = in.createStringArray();
        name = trackValues[0];
        album.name = trackValues[1];
        album.uri = trackValues[2];

        Artist artist = new Artist();
        artist.name = trackValues[4];
        artists.add(artist);

        int[] imageValues = in.createIntArray();

        if (imageValues.length >= 2 && trackValues.length >= 4) {
            album.images.add(new ParceableImage(imageValues[0], imageValues[1], trackValues[3]));
        }
    }
}
