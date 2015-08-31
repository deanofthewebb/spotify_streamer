package app.com.deanofthewebb.spotifystreamer.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.test.AndroidTestCase;

import java.util.Map;
import java.util.Set;

import app.com.deanofthewebb.spotifystreamer.utils.PollingCheck;


public class TestUtilities extends AndroidTestCase {

    public static final String TEST_ARTIST_API_ID = "3fMbdgg4jU18AjLCKBhRSm";

    static void validateCursor(String error, Cursor valueCursor, ContentValues expectedValues) {
        assertTrue("Empty cursor returned. " + error, valueCursor.moveToFirst());
        validateCurrentRecord(error, valueCursor, expectedValues);
        valueCursor.close();
    }

    static void validateCurrentRecord(String error, Cursor valueCursor, ContentValues expectedValues) {
        Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertFalse("Column '" + columnName + "' not found. " + error, idx == -1);
            String expectedValue = entry.getValue().toString();
            assertEquals("Value '" + entry.getValue().toString() +
                    "' did not match the expected value '" +
                    expectedValue + "'. " + error, expectedValue, valueCursor.getString(idx));
        }
    }


    static ContentValues createArtistValues() {
        // Create a new map of values, where column names are the keys
        ContentValues testValues = new ContentValues();
        testValues.put(SpotifyStreamerContract.ArtistEntry.COLUMN_NAME, "Michael Jackson");
        testValues.put(SpotifyStreamerContract.ArtistEntry.COLUMN_API_ID, TEST_ARTIST_API_ID);
        testValues.put(SpotifyStreamerContract.ArtistEntry.COLUMN_API_URI, "spotify:artist:3fMbdgg4jU18AjLCKBhRSm");
        testValues.put(SpotifyStreamerContract.ArtistEntry.COLUMN_POPULARITY, "87");
        testValues.put(SpotifyStreamerContract.ArtistEntry.COLUMN_IMAGE_URL, "https://i.scdn.co/image/06e195eaaa853b397ccaa971edaa25554dba3c05");

        return testValues;
    }


    static ContentValues createTrackValues(long artistRowId) {
        // Create a new map of values, where column names are the keys
        ContentValues testValues = new ContentValues();
        testValues.put(SpotifyStreamerContract.TrackEntry.COLUMN_NAME, "Thriller");
        testValues.put(SpotifyStreamerContract.TrackEntry.COLUMN_API_ID, "3S2R0EVwBSAVMd5UMgKTL0");
        testValues.put(SpotifyStreamerContract.TrackEntry.COLUMN_API_URI, "spotify:track:3S2R0EVwBSAVMd5UMgKTL0");
        testValues.put(SpotifyStreamerContract.TrackEntry.COLUMN_PREVIEW_URL, "https://p.scdn.co/mp3-preview/5371616374a56bb4620612fbbc08d89747a04f2a");
        testValues.put(SpotifyStreamerContract.TrackEntry.COLUMN_POPULARITY, "66");
        testValues.put(SpotifyStreamerContract.TrackEntry.COLUMN_MARKETS, "AD,AR,AT,AU,BE,BG,BO,BR,CA,CH,CL,CO,CR,CY,CZ,DE,DK,DO,EC,EE," +
                "ES,FI,FR,GB,GR,GT,HK,HN,HU,IE,IS,IT,LI,LT,LU,LV,MC,MT,MX,MY,NI,NL,NO,NZ,PA,PE,PH,PL,PT,PY,RO,SE,SG,SI,SK,SV,TR,TW,US,UY");
        testValues.put(SpotifyStreamerContract.TrackEntry.COLUMN_IMAGE_URL, "https://i.scdn.co/image/06e195eaaa853b397ccaa971edaa25554dba3c05");
        testValues.put(SpotifyStreamerContract.TrackEntry.COLUMN_ARTIST_KEY, artistRowId);

        return testValues;
    }

    /*
        Students: You can uncomment this function once you have finished creating the
        LocationEntry part of the WeatherContract as well as the WeatherDbHelper.
     */
    static long insertArtistValues(Context context) {
        // insert our test records into the database
        SpotifyStreamerDbHelper dbHelper = new SpotifyStreamerDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues artistValues = TestUtilities.createArtistValues();

        long artistRowId;
        artistRowId = db.insert(SpotifyStreamerContract.ArtistEntry.TABLE_NAME, null, artistValues);

        // Verify we got a row back.
        assertTrue("Error: Failure to insert Artist Values", artistRowId != -1);

        return artistRowId;
    }


    /*
    Students: You can uncomment this function once you have finished creating the
    LocationEntry part of the WeatherContract as well as the WeatherDbHelper.
 */
    static long insertTrackValues(Context context, long artistRowId) {
        // insert our test records into the database
        SpotifyStreamerDbHelper dbHelper = new SpotifyStreamerDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues trackValues = TestUtilities.createTrackValues(artistRowId);

        long trackRowId;
        trackRowId = db.insert(SpotifyStreamerContract.TrackEntry.TABLE_NAME, null, trackValues);

        // Verify we got a row back.
        assertTrue("Error: Failure to insert Track Values", trackRowId != -1);

        return trackRowId;
    }

    /*
        Students: The functions we provide inside of TestProvider use this utility class to test
        the ContentObserver callbacks using the PollingCheck class that we grabbed from the Android
        CTS tests.

        Note that this only tests that the onChange function is called; it does not test that the
        correct Uri is returned.
     */
    static class TestContentObserver extends ContentObserver {
        final HandlerThread mHT;
        boolean mContentChanged;

        static TestContentObserver getTestContentObserver() {
            HandlerThread ht = new HandlerThread("ContentObserverThread");
            ht.start();
            return new TestContentObserver(ht);
        }

        private TestContentObserver(HandlerThread ht) {
            super(new Handler(ht.getLooper()));
            mHT = ht;
        }

        // On earlier versions of Android, this onChange method is called
        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            mContentChanged = true;
        }

        public void waitForNotificationOrFail() {
            // Note: The PollingCheck class is taken from the Android CTS (Compatibility Test Suite).
            // It's useful to look at the Android CTS source for ideas on how to test your Android
            // applications.  The reason that PollingCheck works is that, by default, the JUnit
            // testing framework is not running on the main Android application thread.
            new PollingCheck(5000) {
                @Override
                protected boolean check() {
                    return mContentChanged;
                }
            }.run();
            mHT.quit();
        }
    }

    static TestContentObserver getTestContentObserver() {
        return TestContentObserver.getTestContentObserver();
    }
}
