/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.com.deanofthewebb.spotifystreamer.data;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.test.AndroidTestCase;
import android.util.Log;

import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract.ArtistEntry;
import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract.TrackEntry;

/*
    Note: This is not a complete set of tests of the Sunshine ContentProvider, but it does test
    that at least the basic functionality has been implemented correctly.

    Students: Uncomment the tests in this class as you implement the functionality in your
    ContentProvider to make sure that you've implemented things reasonably correctly.
 */
public class TestProvider extends AndroidTestCase {

    public static final String LOG_TAG = TestProvider.class.getSimpleName();

    /*
       This helper function deletes all records from both database tables using the ContentProvider.
       It also queries the ContentProvider to make sure that the database has been successfully
       deleted, so it cannot be used until the Query and Delete functions have been written
       in the ContentProvider.

       Students: Replace the calls to deleteAllRecordsFromDB with this one after you have written
       the delete functionality in the ContentProvider.
     */
    public void deleteAllRecordsFromProvider() {
        mContext.getContentResolver().delete(
                ArtistEntry.CONTENT_URI,
                null,
                null
        );
        mContext.getContentResolver().delete(
                TrackEntry.CONTENT_URI,
                null,
                null
        );

        Cursor cursor = mContext.getContentResolver().query(
                TrackEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals("Error: Records not deleted from Track table during delete", 0, cursor.getCount());
        cursor.close();

        cursor = mContext.getContentResolver().query(
                ArtistEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals("Error: Records not deleted from Artist table during delete", 0, cursor.getCount());
        cursor.close();
    }

    /*
       This helper function deletes all records from both database tables using the database
       functions only.  This is designed to be used to reset the state of the database until the
       delete functionality is available in the ContentProvider.
     */
    public void deleteAllRecordsFromDB() {
        SpotifyStreamerDbHelper dbHelper = new SpotifyStreamerDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.delete(ArtistEntry.TABLE_NAME, null, null);
        db.delete(TrackEntry.TABLE_NAME, null, null);
        db.close();
    }

    /*
        Student: Refactor this function to use the deleteAllRecordsFromProvider functionality once
        you have implemented delete functionality there.
     */
    public void deleteAllRecords() {
        deleteAllRecordsFromProvider();
    }

    // Since we want each test to start with a clean slate, run deleteAllRecords
    // in setUp (called by the test runner before each test).
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteAllRecords();
    }

    /*
        This test checks to make sure that the content provider is registered correctly.
        Students: Uncomment this test to make sure you've correctly registered the WeatherProvider.
     */
    public void testProviderRegistry() {
        PackageManager pm = mContext.getPackageManager();

        // We define the component name based on the package name from the context and the
        // WeatherProvider class.
        ComponentName componentName = new ComponentName(mContext.getPackageName(),
                SpotifyStreamerProvider.class.getName());
        try {
            // Fetch the provider info using the component name from the PackageManager
            // This throws an exception if the provider isn't registered.
            ProviderInfo providerInfo = pm.getProviderInfo(componentName, 0);

            // Make sure that the registered authority matches the authority from the Contract.
            assertEquals("Error: SpotifyStreamerProvider registered with authority: " + providerInfo.authority +
                    " instead of authority: " + SpotifyStreamerContract.CONTENT_AUTHORITY,
                    providerInfo.authority, SpotifyStreamerContract.CONTENT_AUTHORITY);
        } catch (PackageManager.NameNotFoundException e) {
            // I guess the provider isn't registered correctly.
            assertTrue("Error: SpotifyStreamerProvider not registered at " + mContext.getPackageName(),
                    false);
        }
    }

    /*
            This test doesn't touch the database.  It verifies that the ContentProvider returns
            the correct type for each type of URI that it can handle.
            Students: Uncomment this test to verify that your implementation of GetType is
            functioning correctly.
         */
    public void testGetType() {
        // content://com.example.android.sunshine.app/track/
        String type = mContext.getContentResolver().getType(TrackEntry.CONTENT_URI);
        // vnd.android.cursor.dir/app.com.android.spotifystreamer/track
        assertEquals("Error: the TrackEntry CONTENT_URI should return TrackEntry.CONTENT_TYPE",
                TrackEntry.CONTENT_TYPE, type);

        String testArtist = "Michael Jackson";
        type = mContext.getContentResolver().getType(
                ArtistEntry.CONTENT_URI);

        // vnd.android.cursor.dir/app.com.example.deanofthewebb.sunshine/weather
        assertEquals("Error: the ArtistEntry CONTENT_URI with location should return ArtistEntry.CONTENT_TYPE",
                ArtistEntry.CONTENT_TYPE, type);

        type = mContext.getContentResolver().getType(
                TrackEntry.buildTrackArtist("api_id"));
        // vnd.android.cursor.item/com.example.android.sunshine.app/weather/1419120000
        assertEquals("Error: the TrackEntry CONTENT_URI with artist should return TrackEntry.CONTENT_ITEM_TYPE",
                TrackEntry.CONTENT_ITEM_TYPE, type);
    }


    /*
        This test uses the database directly to insert and then uses the ContentProvider to
        read out the data.  Uncomment this test to see if the basic weather query functionality
        given in the ContentProvider is working correctly.
     */
    public void testBasicTrackQuery() {
        // insert our test records into the database
        SpotifyStreamerDbHelper dbHelper = new SpotifyStreamerDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues testValues = TestUtilities.createArtistValues();
        long artistRowId = TestUtilities.insertArtistValues(mContext);

        // Fantastic.  Now that we have a location, add some weather!
        ContentValues trackValues = TestUtilities.createTrackValues(artistRowId);

        long trackRowId = db.insert(TrackEntry.TABLE_NAME, null, trackValues);
        assertTrue("Unable to Insert TrackEntry into the Database", trackRowId != -1);

        db.close();

        // Test the basic content provider query
        Cursor trackCursor = mContext.getContentResolver().query(
                TrackEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        // Make sure we get the correct cursor out of the database
        TestUtilities.validateCursor("testBasicTrackQuery", trackCursor, trackValues);
    }

    /*
        This test uses the database directly to insert and then uses the ContentProvider to
        read out the data.  Uncomment this test to see if your location queries are
        performing correctly.
     */
    public void testBasicArtistQueries() {
        // insert our test records into the database
        SpotifyStreamerDbHelper dbHelper = new SpotifyStreamerDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues testValues = TestUtilities.createArtistValues();
        long artistRowId = TestUtilities.insertArtistValues(mContext);

        // Test the basic content provider query
        Cursor artistCursor = mContext.getContentResolver().query(
                ArtistEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        // Make sure we get the correct cursor out of the database
        TestUtilities.validateCursor("testBasicArtistQueries, artist query", artistCursor, testValues);

        // Has the NotificationUri been set correctly? --- we can only test this easily against API
        // level 19 or greater because getNotificationUri was added in API level 19.
        if ( Build.VERSION.SDK_INT >= 19 ) {
            assertEquals("Error: Artist Query did not properly set NotificationUri",
                    artistCursor.getNotificationUri(), ArtistEntry.CONTENT_URI);
        }
    }

    /*
        This test uses the provider to insert and then update the data. Uncomment this test to
        see if your update location is functioning correctly.
     */
    public void testUpdateArtist() {
        // Create a new map of values, where column names are the keys
        ContentValues values = TestUtilities.createArtistValues();

        Uri locationUri = mContext.getContentResolver().
                insert(ArtistEntry.CONTENT_URI, values);
        long artistRowId = ContentUris.parseId(locationUri);

        // Verify we got a row back.
        assertTrue(artistRowId != -1);
        Log.d(LOG_TAG, "New row id: " + artistRowId);

        ContentValues updatedValues = new ContentValues(values);
        updatedValues.put(ArtistEntry._ID, artistRowId);
        updatedValues.put(ArtistEntry.COLUMN_NAME, "Santa's Village");

        // Create a cursor with observer to make sure that the content provider is notifying
        // the observers as expected
        Cursor artistCursor = mContext.getContentResolver().query(ArtistEntry.CONTENT_URI, null, null, null, null);

        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
        artistCursor.registerContentObserver(tco);

        int count = mContext.getContentResolver().update(
                ArtistEntry.CONTENT_URI, updatedValues, ArtistEntry._ID + "= ?",
                new String[] { Long.toString(artistRowId)});
        assertEquals(count, 1);

        // Test to make sure our observer is called.  If not, we throw an assertion.
        //
        // Students: If your code is failing here, it means that your content provider
        // isn't calling getContext().getContentResolver().notifyChange(uri, null);
        tco.waitForNotificationOrFail();

        artistCursor.unregisterContentObserver(tco);
        artistCursor.close();

        // A cursor is your primary interface to the query results.
        Cursor cursor = mContext.getContentResolver().query(
                ArtistEntry.CONTENT_URI,
                null,   // projection
                ArtistEntry._ID + " = " + artistRowId,
                null,   // Values for the "where" clause
                null    // sort order
        );

        TestUtilities.validateCursor("testUpdateArtist.  Error validating artist entry update.",
                cursor, updatedValues);

        cursor.close();
    }


    // Make sure we can still delete after adding/updating stuff
    //
    // Student: Uncomment this test after you have completed writing the insert functionality
    // in your provider.  It relies on insertions with testInsertReadProvider, so insert and
    // query functionality must also be complete before this test can be used.
    public void testInsertReadProvider() {
        ContentValues testValues = TestUtilities.createArtistValues();

        // Register a content observer for our insert.  This time, directly with the content resolver
        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(ArtistEntry.CONTENT_URI, true, tco);
        Uri locationUri = mContext.getContentResolver().insert(ArtistEntry.CONTENT_URI, testValues);

        // Did our content observer get called?  Students:  If this fails, your insert location
        // isn't calling getContext().getContentResolver().notifyChange(uri, null);
        tco.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(tco);

        long artistRowId = ContentUris.parseId(locationUri);

        // Verify we got a row back.
        assertTrue(artistRowId != -1);

        // Data's inserted.  IN THEORY.  Now pull some out to stare at it and verify it made
        // the round trip.

        // A cursor is your primary interface to the query results.
        Cursor cursor = mContext.getContentResolver().query(
                ArtistEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        TestUtilities.validateCursor("testInsertReadProvider. Error validating ArtistEntry.",
                cursor, testValues);

        // Fantastic.  Now that we have a location, add some weather!
        ContentValues trackValues = TestUtilities.createTrackValues(artistRowId);
        // The TestContentObserver is a one-shot class
        tco = TestUtilities.getTestContentObserver();

        mContext.getContentResolver().registerContentObserver(TrackEntry.CONTENT_URI, true, tco);

        Uri weatherInsertUri = mContext.getContentResolver()
                .insert(TrackEntry.CONTENT_URI, trackValues);
        assertTrue(weatherInsertUri != null);

        // Did our content observer get called?  Students:  If this fails, your insert weather
        // in your ContentProvider isn't calling
        // getContext().getContentResolver().notifyChange(uri, null);
        tco.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(tco);

        // A cursor is your primary interface to the query results.
        Cursor trackCursor = mContext.getContentResolver().query(
                TrackEntry.CONTENT_URI,  // Table to Query
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null // columns to group by
        );

        TestUtilities.validateCursor("testInsertReadProvider. Error validating TrackEntry insert.",
                trackCursor, trackValues);

        // Add the artist values in with the track data so that we can make
        // sure that the join worked and we actually get all the values back
        trackValues.putAll(testValues);

        // Get the joined Track and Artist data
        trackCursor = mContext.getContentResolver().query(
                TrackEntry.buildTrackArtist(TestUtilities.TEST_ARTIST_API_ID),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );
        TestUtilities.validateCursor("testInsertReadProvider.  Error validating joined Track and Artist Data.",
                trackCursor, trackValues);

    }

    // Make sure we can still delete after adding/updating stuff
    //
    // Student: Uncomment this test after you have completed writing the delete functionality
    // in your provider.  It relies on insertions with testInsertReadProvider, so insert and
    // query functionality must also be complete before this test can be used.
    public void testDeleteRecords() {
        testInsertReadProvider();

        // Register a content observer for our location delete.
        TestUtilities.TestContentObserver artistObserver = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(ArtistEntry.CONTENT_URI, true, artistObserver);

        // Register a content observer for our weather delete.
        TestUtilities.TestContentObserver trackObserver = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(TrackEntry.CONTENT_URI, true, trackObserver);

        deleteAllRecordsFromProvider();

        // Students: If either of these fail, you most-likely are not calling the
        // getContext().getContentResolver().notifyChange(uri, null); in the ContentProvider
        // delete.  (only if the insertReadProvider is succeeding)
        artistObserver.waitForNotificationOrFail();
        trackObserver.waitForNotificationOrFail();

        mContext.getContentResolver().unregisterContentObserver(artistObserver);
        mContext.getContentResolver().unregisterContentObserver(trackObserver);
    }
}
