package app.com.deanofthewebb.spotifystreamer.data;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class SpotifyStreamerProvider extends ContentProvider {
    private static final String LOG_TAG = SpotifyStreamerProvider.class.getSimpleName();


    public SpotifyStreamerProvider() {
    }

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private SpotifyStreamerDbHelper mOpenHelper;

    static final int TRACK = 100;
    static final int TRACK_WITH_ARTIST = 101;
    static final int ARTIST = 300;
    static final int ARTIST_BY_NAME = 301;

    private static final SQLiteQueryBuilder sTrackByArtistQueryBuilder;
    private static final SQLiteQueryBuilder sArtistQueryBuilder;


    static{
        sTrackByArtistQueryBuilder = new SQLiteQueryBuilder();
        sArtistQueryBuilder = new SQLiteQueryBuilder();

        //This is an inner join which looks like
        //track INNER JOIN artist ON track.artist_key = artist._id
        sTrackByArtistQueryBuilder.setTables(
                SpotifyStreamerContract.TrackEntry.TABLE_NAME + " INNER JOIN " +
                        SpotifyStreamerContract.ArtistEntry.TABLE_NAME +
                        " ON " + SpotifyStreamerContract.TrackEntry.TABLE_NAME +
                        "." + SpotifyStreamerContract.TrackEntry.COLUMN_ARTIST_KEY +
                        " = " + SpotifyStreamerContract.ArtistEntry.TABLE_NAME +
                        "." + SpotifyStreamerContract.ArtistEntry._ID);

        sArtistQueryBuilder.setTables(SpotifyStreamerContract.ArtistEntry.TABLE_NAME);
    }


    //ORDER BY ID ASC
    private static final String sBestMatchSortOrder = SpotifyStreamerContract.ArtistEntry.TABLE_NAME + "." + SpotifyStreamerContract.ArtistEntry._ID + " ASC";



    //artist.artist_name = ?
    private static final String sArtistName =
            SpotifyStreamerContract.ArtistEntry.TABLE_NAME+
                    "." + SpotifyStreamerContract.ArtistEntry.COLUMN_NAME + " = ? ";

    //artist.artist_api_id = ?
    private static final String sArtistId =
            SpotifyStreamerContract.ArtistEntry.TABLE_NAME+
                    "." + SpotifyStreamerContract.ArtistEntry.COLUMN_API_ID + " = ? ";

    static UriMatcher buildUriMatcher() {
        final  UriMatcher uRIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = SpotifyStreamerContract.CONTENT_AUTHORITY;

        uRIMatcher.addURI(authority, SpotifyStreamerContract.PATH_ARTIST, ARTIST);
        uRIMatcher.addURI(authority, SpotifyStreamerContract.PATH_ARTIST + "/*", ARTIST_BY_NAME);
        uRIMatcher.addURI(authority, SpotifyStreamerContract.PATH_TRACK, TRACK);
        uRIMatcher.addURI(authority, SpotifyStreamerContract.PATH_TRACK + "/*", TRACK_WITH_ARTIST);

        return uRIMatcher;
    }


    private Cursor getTrackByArtist(Uri uri, String[] projection, String sortOrder) {
        String artistId = SpotifyStreamerContract.TrackEntry.getTrackArtistIdFromUri(uri);

        String[] selectionArgs;
        String selection;

        selection = sArtistId;
        selectionArgs = new String[]{artistId};

        return sTrackByArtistQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getArtistByBestMatch(Uri uri, String[] projection) {
        String artistQuery = SpotifyStreamerContract.ArtistEntry.getArtistQueryFromUri(uri);

        String[] selectionArgs;
        String selection;

        //WHERE word LIKE '%artistQuery%'
        selection = SpotifyStreamerContract.ArtistEntry.TABLE_NAME +
                "." + SpotifyStreamerContract.ArtistEntry.COLUMN_NAME + " LIKE '?%'";

        selection = selection.replace("?", artistQuery);

        String sortOrder = sBestMatchSortOrder.replace("?", artistQuery);
        Log.v(LOG_TAG, "SORT ORDER BEING USED: " + sortOrder);

        return sArtistQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                null,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if ( null == selection ) selection = "1";
        switch (match) {
            case TRACK:
                rowsDeleted = db.delete(
                        SpotifyStreamerContract.TrackEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case ARTIST:
                rowsDeleted = db.delete(
                        SpotifyStreamerContract.ArtistEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case ARTIST:
                return SpotifyStreamerContract.ArtistEntry.CONTENT_TYPE;
            case TRACK:
                return SpotifyStreamerContract.TrackEntry.CONTENT_TYPE;
            case TRACK_WITH_ARTIST:
                return SpotifyStreamerContract.TrackEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case TRACK: {
                long _id = db.insert(SpotifyStreamerContract.TrackEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = SpotifyStreamerContract.TrackEntry.buildTrackUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case ARTIST: {
                long _id = db.insert(SpotifyStreamerContract.ArtistEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = SpotifyStreamerContract.ArtistEntry.buildArtistUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new SpotifyStreamerDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "track/*"
            case TRACK_WITH_ARTIST: {
                retCursor = getTrackByArtist(uri, projection, sortOrder);
                break;
            }
            // "artist"
            case TRACK: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        SpotifyStreamerContract.TrackEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "artist/*"
            case ARTIST_BY_NAME: {
                retCursor = getArtistByBestMatch(uri, projection);
                break;
            }
            // "track"
            case ARTIST: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        SpotifyStreamerContract.ArtistEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case TRACK:
                rowsUpdated = db.update(SpotifyStreamerContract.TrackEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case ARTIST:
                rowsUpdated = db.update(SpotifyStreamerContract.ArtistEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case TRACK:
                db.beginTransaction();
                int trackReturnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(SpotifyStreamerContract.TrackEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            trackReturnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return trackReturnCount;
            case ARTIST:
                db.beginTransaction();
                int artistReturnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(SpotifyStreamerContract.TrackEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            artistReturnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return artistReturnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    // You do not need to call this method. This is a method specifically to assist the testing
    // framework in running smoothly. You can read more at:
    // http://developer.android.com/reference/android/content/ContentProvider.html#shutdown()
    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }
}
