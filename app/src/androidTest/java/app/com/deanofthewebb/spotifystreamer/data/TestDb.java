package app.com.deanofthewebb.spotifystreamer.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import java.util.HashSet;

public class TestDb extends AndroidTestCase {

    public static final String LOG_TAG = TestDb.class.getSimpleName();

    // Since we want each test to start with a clean slate
    void deleteTheDatabase() {
        mContext.deleteDatabase(SpotifyStreamerDbHelper.DATABASE_NAME);
    }

    /*
        This function gets called before each test is executed to delete the database.  This makes
        sure that we always have a clean test.
     */
    public void setUp() {
        deleteTheDatabase();
    }

    public void testCreateDb() throws Throwable {
        // build a HashSet of all of the table names we wish to look for
        // Note that there will be another table in the DB that stores the
        // Android metadata (db version information)
        final HashSet<String> tableNameHashSet = new HashSet<String>();
        tableNameHashSet.add(SpotifyStreamerContract.ArtistEntry.TABLE_NAME);
        tableNameHashSet.add(SpotifyStreamerContract.TrackEntry.TABLE_NAME);

        mContext.deleteDatabase(SpotifyStreamerDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new SpotifyStreamerDbHelper(
                this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());

        // have we created the tables we want?
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        assertTrue("Error: This means that the database has not been created correctly",
                c.moveToFirst());

        // verify that the tables have been created
        do {
            tableNameHashSet.remove(c.getString(0));
        } while( c.moveToNext() );

        // if this fails, it means that database doesn't contain both the image entry, artist entry,
        // and track entry tables
        assertTrue("Error: Database was created without the proper tables",
                tableNameHashSet.isEmpty());

        // now, do our tables contain the correct columns?
        c = db.rawQuery("PRAGMA table_info(" + SpotifyStreamerContract.ArtistEntry.TABLE_NAME + ")",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());

        // Build a HashSet of all of the column names we want to look for
        final HashSet<String> artistColumnHashSet = new HashSet<String>();
        artistColumnHashSet.add(SpotifyStreamerContract.ArtistEntry._ID);
        artistColumnHashSet.add(SpotifyStreamerContract.ArtistEntry.COLUMN_NAME);
        artistColumnHashSet.add(SpotifyStreamerContract.ArtistEntry.COLUMN_API_ID);
        artistColumnHashSet.add(SpotifyStreamerContract.ArtistEntry.COLUMN_API_URI);
        artistColumnHashSet.add(SpotifyStreamerContract.ArtistEntry.COLUMN_POPULARITY);
        artistColumnHashSet.add(SpotifyStreamerContract.ArtistEntry.COLUMN_IMAGE_URL);

        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            artistColumnHashSet.remove(columnName);
        } while(c.moveToNext());

        // if this fails, it means that your database doesn't contain all of the required location
        // entry columns
        assertTrue("Error: The database doesn't contain all of the required artist entry columns",
                artistColumnHashSet.isEmpty());


        // now, do our tables contain the correct columns?
        c = db.rawQuery("PRAGMA table_info(" + SpotifyStreamerContract.TrackEntry.TABLE_NAME + ")",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());

        // Build a HashSet of all of the column names we want to look for
        final HashSet<String> trackColumnHashSet = new HashSet<String>();
        trackColumnHashSet.add(SpotifyStreamerContract.TrackEntry._ID);
        trackColumnHashSet.add(SpotifyStreamerContract.TrackEntry.COLUMN_NAME);
        trackColumnHashSet.add(SpotifyStreamerContract.TrackEntry.COLUMN_API_ID);
        trackColumnHashSet.add(SpotifyStreamerContract.TrackEntry.COLUMN_API_URI);
        trackColumnHashSet.add(SpotifyStreamerContract.TrackEntry.COLUMN_POPULARITY);
        trackColumnHashSet.add(SpotifyStreamerContract.TrackEntry.COLUMN_MARKETS);
        trackColumnHashSet.add(SpotifyStreamerContract.TrackEntry.COLUMN_PREVIEW_URL);
        trackColumnHashSet.add(SpotifyStreamerContract.TrackEntry.COLUMN_IMAGE_URL);
        trackColumnHashSet.add(SpotifyStreamerContract.TrackEntry.COLUMN_ARTIST_KEY);

        do {
            String columnName = c.getString(columnNameIndex);
            trackColumnHashSet.remove(columnName);
        } while(c.moveToNext());

        // if this fails, it means that your database doesn't contain all of the required location
        // entry columns
        assertTrue("Error: The database doesn't contain all of the required track entry columns",
                trackColumnHashSet.isEmpty());
        db.close();
    }


    public void testArtistTable() {
        insertArtist();
    }



//    /*
//        Students:  Here is where you will build code to test that we can insert and query the
//        database.  We've done a lot of work for you.  You'll want to look in TestUtilities
//        where you can use the "createWeatherValues" function.  You can
//        also make use of the validateCurrentRecord function from within TestUtilities.
//     */
    public long insertArtist() {

        // Instead of rewriting all of the code we've already written in testLocationTable
        // we can move this code to insertLocation and then call insertLocation from both
        // tests. Why move it? We need the code to return the ID of the inserted location
        // and our testLocationTable can only return void because it's a test.

        // First step: Get reference to writable database
        // If there's an error in those massive SQL table creation Strings,
        // errors will be thrown here when you try to get a writable database.
        SpotifyStreamerDbHelper dbHelper = new SpotifyStreamerDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Second Step (Weather): Create weather values
        ContentValues artistValues = TestUtilities.createArtistValues();

        // Third Step (Weather): Insert ContentValues into database and get a row ID back
        long artistRowId = db.insert(SpotifyStreamerContract.ArtistEntry.TABLE_NAME, null, artistValues);
        assertTrue(artistRowId != -1);

        // Fourth Step: Query the database and receive a Cursor back
        // A cursor is your primary interface to the query results.
        Cursor artistCursor = db.query(
                SpotifyStreamerContract.ArtistEntry.TABLE_NAME,  // Table to Query
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null  // sort order
        );

        // Move the cursor to the first valid database row and check to see if we have any rows
        assertTrue("Error: No Records returned from artist query", artistCursor.moveToFirst());

        // Fifth Step: Validate the location Query
        TestUtilities.validateCurrentRecord("testInsertReadDb artistEntry failed to validate",
                artistCursor, artistValues);

        // Move the cursor to demonstrate that there is only one record in the database
        assertFalse("Error: More than one record returned from artist query",
                artistCursor.moveToNext());

        // Sixth Step: Close cursor and database
        artistCursor.close();
        dbHelper.close();

        return artistRowId;
    }


    public void testTrackTable() {
        // Instead of rewriting all of the code we've already written in testLocationTable
        // we can move this code to insertLocation and then call insertLocation from both
        // tests. Why move it? We need the code to return the ID of the inserted location
        // and our testLocationTable can only return void because it's a test.

        long artistRowId = insertArtist();

        // Make sure we have a valid row ID.
        assertFalse("Error: Artist Not Inserted Correctly", artistRowId == -1L);

        // First step: Get reference to writable database
        // If there's an error in those massive SQL table creation Strings,
        // errors will be thrown here when you try to get a writable database.
        SpotifyStreamerDbHelper dbHelper = new SpotifyStreamerDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Second Step (Weather): Create weather values
        ContentValues trackValues = TestUtilities.createTrackValues((int) artistRowId);

        // Third Step (Weather): Insert ContentValues into database and get a row ID back
        long trackRowId = db.insert(SpotifyStreamerContract.TrackEntry.TABLE_NAME, null, trackValues);
        assertTrue(trackRowId != -1);

        // Fourth Step: Query the database and receive a Cursor back
        // A cursor is your primary interface to the query results.
        Cursor trackCursor = db.query(
                SpotifyStreamerContract.TrackEntry.TABLE_NAME,  // Table to Query
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null  // sort order
        );

        // Move the cursor to the first valid database row and check to see if we have any rows
        assertTrue( "Error: No Records returned from track query", trackCursor.moveToFirst() );

        // Fifth Step: Validate the location Query
        TestUtilities.validateCurrentRecord("testInsertReadDb trackEntry failed to validate",
                trackCursor, trackValues);

        // Move the cursor to demonstrate that there is only one record in the database
        assertFalse( "Error: More than one record returned from track query",
                trackCursor.moveToNext() );

        // Sixth Step: Close cursor and database
        trackCursor.close();
        dbHelper.close();
    }
}