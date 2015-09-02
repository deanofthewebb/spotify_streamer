package app.com.deanofthewebb.spotifystreamer.fragment;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;


import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Vector;

import app.com.deanofthewebb.spotifystreamer.adapter.ArtistCursorAdapter;
import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract;
import app.com.deanofthewebb.spotifystreamer.model.ParceableArtist;
import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.activity.DetailActivity;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TracksPager;
import retrofit.RetrofitError;

import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract.ArtistEntry;

public class ArtistSearchFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>{
    private final String LOG_TAG = ArtistSearchFragment.class.getSimpleName();
    private ArtistCursorAdapter mArtistCursorAdapter;
    private ListView mListView;
    private int mPosition = mListView.INVALID_POSITION;

    private static final String SELECTED_KEY = "selected_position";

    public static String mArtistQuery = "";

    private static final int ARTIST_LOADER_ID = 0;
    private static final String[] ARTIST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the artist & track tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the track table
            // using the artist set by the user, which is only in the Artist table.
            // So the convenience is worth it.
            ArtistEntry.TABLE_NAME + "." + ArtistEntry._ID,
            ArtistEntry.TABLE_NAME + "." + ArtistEntry.COLUMN_NAME,
            ArtistEntry.TABLE_NAME + "." + ArtistEntry.COLUMN_API_ID,
            ArtistEntry.TABLE_NAME + "." + ArtistEntry.COLUMN_API_URI,
            ArtistEntry.TABLE_NAME + "." + ArtistEntry.COLUMN_POPULARITY,
            ArtistEntry.TABLE_NAME + "." + ArtistEntry.COLUMN_IMAGE_URL
    };

    // These indices are tied to ARTIST_COLUMNS.  If ARTIST_COLUMNS changes, these
    // must change.
    public static final int COL_ARTIST_ID = 0;
    public static final int COL_ARTIST_NAME = 1;
    public static final int COL_ARTIST_API_ID = 2;
    public static final int COL_ARTIST_API_URI = 3;
    public static final int COL_ARTIST_POPULARITY = 4;
    public static final int COL_ARTIST_IMAGE_URL = 5;

    public ArtistSearchFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mArtistCursorAdapter = new ArtistCursorAdapter(getActivity(), null, 0);
        UpdateArtistsOnKeysEntered(rootView);


        mListView = (ListView) rootView.findViewById(R.id.artist_results_listview);
        mListView.setAdapter(mArtistCursorAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);

                Intent artistDetailIntent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(ArtistEntry.COLUMN_API_ID, cursor.getString(ArtistSearchFragment.COL_ARTIST_API_ID))
                        .putExtra(ArtistEntry._ID, cursor.getString(ArtistSearchFragment.COL_ARTIST_ID))
                        .putExtra(ArtistEntry.COLUMN_NAME, cursor.getString(ArtistSearchFragment.COL_ARTIST_NAME));

                startActivity(artistDetailIntent);
            }
        });

        // If there's instance state, mine it for useful information.
        // The end-goal here is that the user never knows that turning their device sideways
        // does crazy lifecycle related things.  It should feel like some stuff stretched out,
        // or magically appeared to take advantage of room, but data or place in the app was never
        // actually *lost*.
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            // The listview probably hasn't even been populated yet.  Actually perform the
            // swapout in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        return rootView;
    }

    private void UpdateArtistsOnKeysEntered(View rootView) {
        final SearchView searchText = (SearchView) rootView.findViewById(R.id.search_text);
        searchText.setIconifiedByDefault(false);
        searchText.setQueryHint(getResources().getString(R.string.query_artist_hint));
        searchText.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mArtistQuery = searchText.getQuery().toString();
                UpdateArtistResults(mArtistQuery);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mArtistQuery = searchText.getQuery().toString();
                //UpdateArtistResults(mArtistQuery);
                return false;
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void UpdateArtistResults(String artistQuery) {
        if (isNetworkAvailable()) {
            FetchArtistsTask artistTask = new FetchArtistsTask();
            artistTask.execute(artistQuery);
        }
        else {
            ShowNoNetworkFoundToast();
        }

        // Initialize Loader here
        if (!getLoaderManager().hasRunningLoaders()) {
            getLoaderManager().initLoader(ARTIST_LOADER_ID, null, this);
        } else {
            // Restart if query changes - not working yet
            getLoaderManager().destroyLoader(ARTIST_LOADER_ID);
            getLoaderManager().initLoader(ARTIST_LOADER_ID, null, this);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void ShowNoNetworkFoundToast() {
        CharSequence text = getString(R.string.no_network_found);
        int duration = Toast.LENGTH_LONG;
        Toast.makeText(getActivity(), text, duration).show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.

        // To only show current and future dates, filter the query to return weather only for
        // dates after or including today.

        // Sort order:  Descending, by name relevance.
        String sortOrder = ArtistEntry.COLUMN_NAME + " DESC";

        Uri projection;

        if ("".equals(mArtistQuery)) {
            projection = ArtistEntry.CONTENT_URI;
        } else {
            projection = ArtistEntry.buildArtistByQuery(mArtistQuery);
        }

        return new CursorLoader(getActivity(),
                projection,
                ARTIST_COLUMNS,
                null,
                null,
                sortOrder);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        // When no item is selected, mPosition will be set to Listview.INVALID_POSITION,
        // so check for that before storing.
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mArtistCursorAdapter.swapCursor(cursor);
        if (mPosition != ListView.INVALID_POSITION) {
            // If we don't need to restart the loader, and there's a desired position to restore
            // to, do so now.
            mListView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mArtistCursorAdapter.swapCursor(null);
    }


    public class FetchArtistsTask extends AsyncTask<String, Void, Void> {
        private final String LOG_TAG = FetchArtistsTask.class.getSimpleName();


        @Override
        protected Void doInBackground(String... params) {
            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();

            //Load Data into db
            if (params != null) { getArtistDataFromSpotifyWrapper(spotify, params[0]); }
            return null;
        }


        long addArtist(String name, String apiId, String apiUri, int popularity, String imageUrl) {
            long artistId;

            // First, check if the artist with this city name exists in the db
            Cursor artistCursor =  getActivity().getContentResolver().query(
                    SpotifyStreamerContract.ArtistEntry.CONTENT_URI,
                    new String[]{SpotifyStreamerContract.ArtistEntry._ID},
                    SpotifyStreamerContract.ArtistEntry.COLUMN_API_ID + " = ?",
                    new String[]{apiId},
                    null);

            if (artistCursor.moveToFirst()) {
                int artistIdIndex = artistCursor.getColumnIndex(SpotifyStreamerContract.ArtistEntry._ID);
                artistId = artistCursor.getLong(artistIdIndex);
            } else {
                // Now that the content provider is set up, inserting rows of data is pretty simple.
                // First create a ContentValues object to hold the data you want to insert.
                ContentValues artistValues = new ContentValues();

                // Then add the data, along with the corresponding name of the data type,
                // so the content provider knows what kind of value is being inserted.
                artistValues.put(SpotifyStreamerContract.ArtistEntry.COLUMN_NAME, name);
                artistValues.put(SpotifyStreamerContract.ArtistEntry.COLUMN_API_ID, apiId);
                artistValues.put(SpotifyStreamerContract.ArtistEntry.COLUMN_API_URI, apiUri);
                artistValues.put(SpotifyStreamerContract.ArtistEntry.COLUMN_POPULARITY, popularity);
                artistValues.put(SpotifyStreamerContract.ArtistEntry.COLUMN_IMAGE_URL, imageUrl);

                // Finally, insert location data into the database.
                Uri insertedUri = getActivity().getContentResolver().insert(
                        SpotifyStreamerContract.ArtistEntry.CONTENT_URI,
                        artistValues
                );

                // The resulting URI contains the ID for the row.  Extract the locationId from the Uri.
                artistId = ContentUris.parseId(insertedUri);
            }

            artistCursor.close();
            // Wait, that worked?  Yes!
            return artistId;
        }

        private ArtistsPager getArtistDataFromSpotifyWrapper(SpotifyService spotify, String artistQuery) {
            ArtistsPager results = new ArtistsPager();
            try {
                if (artistQuery != null) results = spotify.searchArtists(artistQuery);

                for (Artist artist : results.artists.items) {
                    String artistImageUrl;

                    if (!artist.images.isEmpty()) {
                        Image artistImage = (artist.images.get(artist.images.size() - 1));
                        artistImageUrl = artistImage.url;
                    } else{ artistImageUrl = ""; }

                    long artistRowId = addArtist(artist.name, artist.id, artist.uri, artist.popularity, artistImageUrl);

                    if (artistRowId == -1) throw new MalformedURLException("THERE WAS AN ERROR INSERTING " + artist.name + " - " + artist.id);
                }
            } catch (RetrofitError re) {
                Log.d(LOG_TAG, "Retrofit error has occurred: " + re.getMessage());
            }
            catch (Exception ex) {
                Log.d(LOG_TAG, "An error has occurred: " + ex.getMessage());
            }

            return results;
        }

        private void ShowNoArtistsFoundToast() {
            CharSequence text = getString(R.string.no_artists_found);
            int duration = Toast.LENGTH_LONG;
            Toast.makeText(getActivity(), text, duration).show();
        }
    }

}
