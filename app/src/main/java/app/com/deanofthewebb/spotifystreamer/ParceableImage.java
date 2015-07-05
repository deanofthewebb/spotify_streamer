package app.com.deanofthewebb.spotifystreamer;


import android.os.Parcel;
import android.os.Parcelable;

import kaaes.spotify.webapi.android.models.Image;

public class ParceableImage extends Image implements Parcelable {
    @Override
    public int describeContents() {
        return 0;
    }

    public ParceableImage(){
        super();
    }

    public ParceableImage(Integer width, Integer height, String url) {
        this.width = width;
        this.height = height;
        this.url = url;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeIntArray(new int[] {width, height});
        dest.writeString(url);
    }

    public static final Parcelable.Creator<ParceableImage> CREATOR
            = new Parcelable.Creator<ParceableImage>() {
        public ParceableImage createFromParcel(Parcel in) {
            return new ParceableImage(in);
        }

        public ParceableImage[] newArray(int size) {
            return new ParceableImage[size];
        }
    };

    private ParceableImage(Parcel in) {
        int[] val = in.createIntArray();

        width = val[0];
        height = val[1];

        url = in.readString();
    }

}
