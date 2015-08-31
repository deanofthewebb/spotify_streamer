package app.com.deanofthewebb.spotifystreamer.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract.ArtistEntry;
import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract.TrackEntry;

/**
 * Manages a local database for spotify streamer data.
 */
public class SpotifyStreamerDbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    static final String DATABASE_NAME = "spotify_streamer.db";


    public SpotifyStreamerDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {

        final String SQL_CREATE_ARTIST_TABLE = "CREATE TABLE " + ArtistEntry.TABLE_NAME + " (" +
                ArtistEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                ArtistEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                ArtistEntry.COLUMN_API_ID + " TEXT NOT NULL, " +
                ArtistEntry.COLUMN_API_URI + " TEXT NOT NULL, " +
                ArtistEntry.COLUMN_POPULARITY + " INTEGER NOT NULL, " +
                ArtistEntry.COLUMN_IMAGE_URL + " TEXT NOT NULL " +
                " );";

        final String SQL_CREATE_TRACK_TABLE = "CREATE TABLE " + TrackEntry.TABLE_NAME + " (" +
                TrackEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                TrackEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                TrackEntry.COLUMN_API_ID + " TEXT NOT NULL, " +
                TrackEntry.COLUMN_API_URI + " TEXT NOT NULL, " +
                TrackEntry.COLUMN_PREVIEW_URL + " TEXT NOT NULL, " +
                TrackEntry.COLUMN_MARKETS + " TEXT NOT NULL, " +
                TrackEntry.COLUMN_POPULARITY + " INTEGER NOT NULL, " +
                TrackEntry.COLUMN_IMAGE_URL + " TEXT NOT NULL, " +
                TrackEntry.COLUMN_ARTIST_KEY + " INTEGER NOT NULL, " +

                // Set up the artist column as a foreign key to location table.
                " FOREIGN KEY (" + TrackEntry.COLUMN_ARTIST_KEY + ") REFERENCES " +
                ArtistEntry.TABLE_NAME + " (" + ArtistEntry._ID + ")" +
                " );";

        db.execSQL(SQL_CREATE_ARTIST_TABLE);
        db.execSQL(SQL_CREATE_TRACK_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 2 lines
        // should be your top priority before modifying this method.
        db.execSQL("DROP TABLE IF EXISTS " + ArtistEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TrackEntry.TABLE_NAME);
        onCreate(db);
    }
}

