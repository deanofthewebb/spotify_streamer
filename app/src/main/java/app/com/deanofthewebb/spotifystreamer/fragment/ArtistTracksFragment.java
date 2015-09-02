package app.com.deanofthewebb.spotifystreamer.fragment;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import app.com.deanofthewebb.spotifystreamer.activity.PlaybackActivity;
import app.com.deanofthewebb.spotifystreamer.adapter.TrackCursorAdapter;
import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract;
import app.com.deanofthewebb.spotifystreamer.model.ParceableTrack;
import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.activity.DetailActivity;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.RetrofitError;


public class ArtistTracksFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>{
    private final String LOG_TAG = ArtistTracksFragment.class.getSimpleName();
    private final String PARCEL_TRACKS = "parcel_tracks";
    private ArrayList<ParceableTrack> tracksFound;
    private static String mArtistApiId;
    private static String mArtistRowId;
    private static String mArtistName;

    private TrackCursorAdapter mTrackCursorAdapter;
    private ListView mListView;
    private int mPosition = mListView.INVALID_POSITION;

    private static final String SELECTED_KEY = "selected_position";

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
        tracksFound = new ArrayList<>();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putParcelableArrayList(PARCEL_TRACKS, tracksFound);
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
        if (mArtistApiId != null) { UpdateTopTracks(); }
    }



    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Intent artistDetailIntent = getActivity().getIntent();
        super.onActivityCreated(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        Intent artistDetailIntent = getActivity().getIntent();

        mTrackCursorAdapter = new TrackCursorAdapter(getActivity(), null, 0);

        mListView = (ListView) rootView.findViewById(R.id.track_results_listview);
        mListView.setAdapter(mTrackCursorAdapter);

        if (artistDetailIntent != null && artistDetailIntent.hasExtra(SpotifyStreamerContract.ArtistEntry.COLUMN_API_ID)) {
            mArtistApiId = artistDetailIntent.getStringExtra(SpotifyStreamerContract.ArtistEntry.COLUMN_API_ID);
            mArtistRowId = artistDetailIntent.getStringExtra(SpotifyStreamerContract.ArtistEntry._ID);
            mArtistName = artistDetailIntent.getStringExtra(SpotifyStreamerContract.ArtistEntry.COLUMN_NAME);
            ((DetailActivity)getActivity()).setActionBarSubTitle(mArtistName);
            UpdateTopTracks();
        } else { Log.e(LOG_TAG, "Intent passed is null!"); }

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                Intent playbackIntent = new Intent(getActivity(), PlaybackActivity.class)
                        .putExtras(getTrackBundle(cursor))
                        .putExtra(SpotifyStreamerContract.TrackEntry.FULLY_QUALIFIED_ID, cursor.getString(ArtistTracksFragment.COL_TRACK_ID));

                startActivity(playbackIntent);
            }
        });

        return rootView;
    }

    @NonNull
    private Bundle getTrackBundle(Cursor cursor) {
        Bundle trackBundle = new Bundle();
        trackBundle.putString(SpotifyStreamerContract.TrackEntry.COLUMN_API_ID,
                cursor.getString(ArtistTracksFragment.COL_TRACK_API_ID));
        trackBundle.putString(SpotifyStreamerContract.TrackEntry.COLUMN_MARKETS,
                cursor.getString(ArtistTracksFragment.COL_TRACK_MARKETS));
        trackBundle.putString(SpotifyStreamerContract.TrackEntry.COLUMN_NAME,
                cursor.getString(ArtistTracksFragment.COL_TRACK_NAME));
        trackBundle.putString(SpotifyStreamerContract.TrackEntry.COLUMN_ALBUM_NAME,
                cursor.getString(ArtistTracksFragment.COL_TRACK_ALBUM_NAME));
        trackBundle.putString(SpotifyStreamerContract.TrackEntry.COLUMN_IMAGE_URL,
                cursor.getString(ArtistTracksFragment.COL_TRACK_IMAGE_URL));
        trackBundle.putString(SpotifyStreamerContract.TrackEntry.COLUMN_PREVIEW_URL,
                cursor.getString(ArtistTracksFragment.COL_TRACK_PREVIEW_URL));
        trackBundle.putString(SpotifyStreamerContract.TrackEntry.COLUMN_ARTIST_KEY,
                cursor.getString(ArtistTracksFragment.COL_TRACK_ARTIST_KEY));
        return trackBundle;
    }

    private void UpdateTopTracks() {
        FetchTopTracksTask topTracksTask = new FetchTopTracksTask();
        topTracksTask.execute(mArtistApiId);

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

        Uri projection = SpotifyStreamerContract.TrackEntry.buildTrackArtist(mArtistApiId);

        return new CursorLoader(getActivity(),
                projection,
                TRACK_COLUMNS,
                null,
                null,
                sortOrder);
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
                    Log.v(LOG_TAG, "Inserted Row: " + rowsUpdate);
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

            Log.d(LOG_TAG, "SpotifyStreamer Service Complete: " + inserted + " tracks inserted");
        }


        private void ShowNoTracksFoundToast() {
            CharSequence text = getString(R.string.no_tracks_found);
            int duration = Toast.LENGTH_LONG;
            Toast.makeText(getActivity(), text, duration).show();
        }
    }
}
