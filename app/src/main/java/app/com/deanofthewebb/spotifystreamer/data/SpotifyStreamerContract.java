package app.com.deanofthewebb.spotifystreamer.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines table and column names for the weatspotify streamer local cache.
 */
public class SpotifyStreamerContract {

    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "app.com.deanofthewebb.spotifystreamer";


    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths (appended to base content URI for possible URI's)
    // For instance, content://com.example.android.sunshine.app/weather/ is a valid path for
    // looking at weather data. content://com.example.android.sunshine.app/givemeroot/ will fail,
    // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
    // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
    public static final String PATH_ARTIST = "artist";
    public static final String PATH_TRACK = "track";


    /* Inner class that defines the table contents of the artist table */
    public static final class ArtistEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ARTIST).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ARTIST;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ARTIST;

        public static final String TABLE_NAME = "artist";

        public static final String COLUMN_NAME = "name";

        public static final String COLUMN_API_ID = "api_id";

        public static final String COLUMN_API_URI = "api_uri";

        public static final String COLUMN_POPULARITY = "popularity";

        public static final String COLUMN_IMAGE_URL = "image_url";

        public static Uri buildArtistUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildArtistByQuery(String artistQuery) {
            return CONTENT_URI.buildUpon()
                    .appendPath(artistQuery).build();
        }

        public static String getArtistQueryFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

    }

    /* Inner class that defines the table contents of the artist table */
    public static final class TrackEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRACK).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_TRACK;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_TRACK;

        public static final String TABLE_NAME = "track";

        public static final String COLUMN_NAME = "name";

        public static final String COLUMN_API_ID = "api_id";

        public static final String COLUMN_API_URI = "api_uri";

        public static final String COLUMN_PREVIEW_URL = "preview_url";

        public static final String COLUMN_POPULARITY = "popularity";

        public static final String COLUMN_MARKETS = "markets";

        public static final String COLUMN_IMAGE_URL = "image_url";

        public static final String COLUMN_ARTIST_KEY = "artist_id";

        public static Uri buildTrackUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildTrackArtist(String artistName) {
            return CONTENT_URI.buildUpon()
                    .appendPath(artistName).build();
        }

        public static String getTrackArtistFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }
}
