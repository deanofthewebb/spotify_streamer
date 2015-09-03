package app.com.deanofthewebb.spotifystreamer.fragment;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import app.com.deanofthewebb.spotifystreamer.Utility;
import app.com.deanofthewebb.spotifystreamer.activity.PlaybackActivity;
import app.com.deanofthewebb.spotifystreamer.adapter.TrackCursorAdapter;
import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract;
import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.activity.DetailActivity;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.RetrofitError;


public class ArtistTracksFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>{
    private final String LOG_TAG = ArtistTracksFragment.class.getSimpleName();
    private static Artist mArtist;

    private TrackCursorAdapter mTrackCursorAdapter;
    private ListView mListView;
    private int mPosition = mListView.INVALID_POSITION;

    private static final String SELECTED_KEY = "selected_position";
    public static final String DETAIL_URI = "URI";
    private Uri mUri;
    private String mArtistRowId;


    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * ArtistTracksFragmentCallback for when an item has been selected.
         */
        void onItemSelected(Uri trackUri);
    }

    private static final int TRACK_LOADER_ID = 0;
    private static final String[] TRACK_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the artist & track tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the track table
            // using the artist set by the user, which is only in the Artist table.
            // So the convenience is worth it.
            SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry._ID,
            SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry.COLUMN_NAME,
            SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry.COLUMN_API_ID,
            SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry.COLUMN_API_URI,
            SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry.COLUMN_POPULARITY,
            SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry.COLUMN_IMAGE_URL,
            SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry.COLUMN_PREVIEW_URL,
            SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry.COLUMN_MARKETS,
            SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry.COLUMN_ALBUM_NAME,
            SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry.COLUMN_ARTIST_KEY
    };

    // These indices are tied to ARTIST_COLUMNS.  If ARTIST_COLUMNS changes, these
    // must change.
    public static final int COL_TRACK_ID = 0;
    public static final int COL_TRACK_NAME = 1;
    public static final int COL_TRACK_API_ID = 2;
    public static final int COL_TRACK_API_URI = 3;
    public static final int COL_TRACK_POPULARITY = 4;
    public static final int COL_TRACK_IMAGE_URL = 5;
    public static final int COL_TRACK_PREVIEW_URL = 6;
    public static final int COL_TRACK_MARKETS = 7;
    public static final int COL_TRACK_ALBUM_NAME = 8;
    public static final int COL_TRACK_ARTIST_KEY = 9;


    public static final String TRACK_ID_EXTRA = "t_n_e";

    public ArtistTracksFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
        if (mArtist != null) {
            UpdateTopTracks();
            getLoaderManager().restartLoader(TRACK_LOADER_ID, null, (ArtistTracksFragment) getFragmentManager().findFragmentById(R.id.track_detail_container));

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments != null) {
            Log.v(LOG_TAG, "SHOULDN'T BE IN HERE UNTIL AFTER WE CLICK BUTTON");
            mUri = arguments.getParcelable(ArtistTracksFragment.DETAIL_URI);

            try {
                mArtist = Utility.buildArtistFromContentProviderApiId(getActivity(), SpotifyStreamerContract.TrackEntry.getTrackArtistIdFromUri(mUri));
            } catch (Exception e) { e.printStackTrace(); }

            Cursor artistCursor = getActivity().getContentResolver().query(
                    SpotifyStreamerContract.ArtistEntry.CONTENT_URI,
                    null,
                    SpotifyStreamerContract.ArtistEntry.COLUMN_API_ID + " = ? ",
                    new String[] {mArtist.id},
                    null);

            if (artistCursor.moveToNext()) mArtistRowId = artistCursor.getString(ArtistSearchFragment.COL_ARTIST_ID);
            artistCursor.close();
            UpdateTopTracks();
        }

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        mTrackCursorAdapter = new TrackCursorAdapter(getActivity(), null, 0);
        mListView = (ListView) rootView.findViewById(R.id.track_results_listview);
        mListView.setAdapter(mTrackCursorAdapter);

//        Activity activity = getActivity();
//        if (activity.ge)
//                ((DetailActivity) getActivity()).setActionBarSubTitle(mArtist.name);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                Intent playbackIntent = new Intent(getActivity(), PlaybackActivity.class)
                        .putExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID, cursor.getString(ArtistTracksFragment.COL_TRACK_ID));

                startActivity(playbackIntent);
            }
        });

        return rootView;
    }


    private void UpdateTopTracks() {
        FetchTopTracksTask topTracksTask = new FetchTopTracksTask();
        topTracksTask.execute(mArtist.id);

        // Initialize Loader here
        if (!getLoaderManager().hasRunningLoaders()) {
            getLoaderManager().initLoader(TRACK_LOADER_ID, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.

        // To only show current and future dates, filter the query to return weather only for
        // dates after or including today.

        // Sort order:  Descending, by name relevance.
        String sortOrder = SpotifyStreamerContract.TrackEntry.TABLE_NAME + "." + SpotifyStreamerContract.TrackEntry.COLUMN_POPULARITY + " DESC";

        if (null != mUri) {
            return new CursorLoader(getActivity(),
                    mUri,
                    TRACK_COLUMNS,
                    null,
                    null,
                    sortOrder);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mTrackCursorAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            // If we don't need to restart the loader, and there's a desired position to restore
            // to, do so now.
            mListView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mTrackCursorAdapter.swapCursor(null);
    }


    public class FetchTopTracksTask extends AsyncTask<String, Void, Tracks> {
        private final String LOG_TAG = FetchTopTracksTask.class.getSimpleName();

        @Override
        protected Tracks doInBackground(String... params) {
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();

            try {
                    Tracks results = new Tracks();
                    if (params != null) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        String countryCode = preferences.getString(getString(R.string.pref_country_key), getString(R.string.pref_country_code_usa));

                        Map<String, Object> options = new HashMap<String, Object>() {};
                        options.put(SpotifyService.COUNTRY, countryCode);

                        results = getTrackDataFromSpotifyWrapper(spotify, params[0]);
                    }

                    return results;
            }
            catch (RetrofitError re) {
                Log.d(LOG_TAG, "Retrofit error has occured: " + re.getMessage());
                return null;
            }
            catch (Exception ex) {
                Log.d(LOG_TAG, "An error has occured: " + ex.getMessage());
                return null;
            }
        }

        private Tracks getTrackDataFromSpotifyWrapper(SpotifyService spotify, String artistId) {
            Tracks results = new Tracks();

            try {
                if (artistId != null){
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    String countryCode = preferences.getString(getString(R.string.pref_country_key), getString(R.string.pref_country_code_usa));

                    Map<String, Object> options = new HashMap<String, Object>() {};
                    options.put(SpotifyService.COUNTRY, countryCode);

                    results = spotify.getArtistTopTrack(artistId, options);
                }
            } catch (RetrofitError re) {
                Log.d(LOG_TAG, "Retrofit error has occured: " + re.getMessage());
            }
            catch (Exception ex) {
                Log.d(LOG_TAG, "An error has occured: " + ex.getMessage());
            }
            return results;
        }

        @Override
        protected void onPostExecute(Tracks results) {
           if (results.tracks == null || results.tracks.size() == 0) {
               ShowNoTracksFoundToast();
           } else {
                insertTracks(results);
            }
        }

        private void insertTracks(Tracks results) {
            final int TOP_X_TRACKS = 10;

            if (results.tracks.size() > TOP_X_TRACKS) {
                results.tracks = results.tracks.subList(0, 10);
            }

            Vector<ContentValues> cVVector = new Vector<ContentValues>(TOP_X_TRACKS);

            // Add tracks to Vector
            for (Track track : results.tracks) {
                // First, check if the track with this city name exists in the db
                Cursor trackCursor = getActivity().getContentResolver().query(
                        SpotifyStreamerContract.TrackEntry.CONTENT_URI,
                        new String[]{SpotifyStreamerContract.TrackEntry._ID},
                        SpotifyStreamerContract.TrackEntry.COLUMN_API_ID + " = ?",
                        new String[]{track.id},
                        null);

                String trackImageUrl = "";
                if (!track.album.images.isEmpty()) {
                    Image albumImage = (track.album.images.get(0));
                    trackImageUrl = albumImage.url;
                }

                ContentValues trackValues = new ContentValues();
                trackValues.put(SpotifyStreamerContract.TrackEntry.COLUMN_NAME, track.name);
                trackValues.put(SpotifyStreamerContract.TrackEntry.COLUMN_API_ID, track.id);
                trackValues.put(SpotifyStreamerContract.TrackEntry.COLUMN_API_URI, track.uri);
                trackValues.put(SpotifyStreamerContract.TrackEntry.COLUMN_PREVIEW_URL, track.preview_url);
                trackValues.put(SpotifyStreamerContract.TrackEntry.COLUMN_POPULARITY, track.popularity);
                trackValues.put(SpotifyStreamerContract.TrackEntry.COLUMN_MARKETS, TextUtils.join(",", track.available_markets));
                trackValues.put(SpotifyStreamerContract.TrackEntry.COLUMN_IMAGE_URL, trackImageUrl);
                trackValues.put(SpotifyStreamerContract.TrackEntry.COLUMN_ALBUM_NAME, track.album.name);
                trackValues.put(SpotifyStreamerContract.TrackEntry.COLUMN_ARTIST_KEY, mArtistRowId);

                if (!trackCursor.moveToFirst()) {
                    cVVector.add(trackValues);
                } else {
                    // Update Data
                    int rowsUpdate = getActivity().getContentResolver().update(
                            SpotifyStreamerContract.TrackEntry.CONTENT_URI,
                            trackValues,
                            SpotifyStreamerContract.TrackEntry.COLUMN_API_ID + " = ?",
                            new String[]{track.id}
                    );
                    if (rowsUpdate < 1)  throw new android.database.SQLException("Failed to update row: " + rowsUpdate);
                }
                trackCursor.close();
            }

            int inserted = 0;
            // add to database
            if ( cVVector.size() > 0 ) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                inserted = getActivity().getContentResolver().bulkInsert(SpotifyStreamerContract.TrackEntry.CONTENT_URI, cvArray);
            }

            if (inserted == -1) try {
                throw new MalformedURLException("THERE WAS AN ERROR BULK INSERTING TRACK DATA");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }


        private void ShowNoTracksFoundToast() {
            CharSequence text = getString(R.string.no_tracks_found);
            int duration = Toast.LENGTH_LONG;
            Toast.makeText(getActivity(), text, duration).show();
        }
    }
}
