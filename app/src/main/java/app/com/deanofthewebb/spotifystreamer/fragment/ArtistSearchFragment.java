package app.com.deanofthewebb.spotifystreamer.fragment;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;


import java.net.MalformedURLException;

import app.com.deanofthewebb.spotifystreamer.adapter.ArtistCursorAdapter;
import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract;
import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.helpers.Constants;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import retrofit.RetrofitError;

import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract.ArtistEntry;

public class ArtistSearchFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>{
    private final String LOG_TAG = ArtistSearchFragment.class.getSimpleName();
    private ArtistCursorAdapter mArtistCursorAdapter;
    private ListView mListView;
    private int mPosition = mListView.INVALID_POSITION;

    public static String mArtistQuery = "";
    public ArtistSearchFragment() { }


    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * ArtistSearchFragmentCallback for when an item has been selected.
         */
        void onItemSelected(Uri trackUri);
    }


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

                if (cursor != null) {
                    ((ArtistSearchFragment.Callback) getActivity())
                            .onItemSelected(SpotifyStreamerContract.TrackEntry.buildTrackArtist(
                                    cursor.getString(Constants.CONTENT_PROVIDER.COL_ARTIST_API_ID)
                            ));
                }
                mPosition = position;
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey(Constants.KEY.SELECTED_ITEM_POSITION)) {
            mPosition = savedInstanceState.getInt(Constants.KEY.SELECTED_ITEM_POSITION);
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
                getLoaderManager().restartLoader(Constants.LOADER_ID.ARTIST_LOADER, null,
                        (ArtistSearchFragment) getFragmentManager().findFragmentById(R.id.fragment_artist_search));
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mArtistQuery = searchText.getQuery().toString();
                UpdateArtistResults(mArtistQuery);
                getLoaderManager().restartLoader(Constants.LOADER_ID.ARTIST_LOADER, null,
                        (ArtistSearchFragment) getFragmentManager().findFragmentById(R.id.fragment_artist_search));
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
            getLoaderManager().initLoader(Constants.LOADER_ID.ARTIST_LOADER, null, this);
        } else {
            // Restart if query changes - not working yet
            getLoaderManager().destroyLoader(Constants.LOADER_ID.ARTIST_LOADER);
            getLoaderManager().initLoader(Constants.LOADER_ID.ARTIST_LOADER, null, this);
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

        return new CursorLoader(
                getActivity(),
                projection,
                Constants.CONTENT_PROVIDER.ARTIST_COLUMNS,
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
            outState.putInt(Constants.KEY.SELECTED_ITEM_POSITION, mPosition);
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
