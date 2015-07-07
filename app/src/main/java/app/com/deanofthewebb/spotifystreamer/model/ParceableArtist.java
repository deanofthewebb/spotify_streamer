package app.com.deanofthewebb.spotifystreamer.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;

public class ParceableArtist extends Artist implements Parcelable {

    public ParceableArtist() {
        super();
        images = new ArrayList<Image>();
    }

    public ParceableArtist(String name, String id, Image image) {
        this();

        this.name = name;
        this.id = id;

        if (image != null) {
            images.add(image);
        }
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (!this.images.isEmpty()) {
            Image image = this.images.get(0);

            dest.writeStringArray(new String[]{name, id, image.url});
            dest.writeIntArray(new int[]{image.width, image.height});
        }
        else {
            dest.writeStringArray(new String[]{name, id});
        }
    }

    public static final Parcelable.Creator<ParceableArtist> CREATOR
            = new Parcelable.Creator<ParceableArtist>() {
        public ParceableArtist createFromParcel(Parcel in) {
            return new ParceableArtist(in);
        }

        public ParceableArtist[] newArray(int size) {
            return new ParceableArtist[size];
        }
    };

    private ParceableArtist(Parcel in) {
        int[] imageValues = in.createIntArray();
        String[] artistValues = in.createStringArray();

        name = artistValues[0];
        id = artistValues[1];

        if (imageValues.length == 2 && artistValues.length == 3) {
            images.add(new ParceableImage(imageValues[0], imageValues[1], artistValues[2]));
        }
    }
}