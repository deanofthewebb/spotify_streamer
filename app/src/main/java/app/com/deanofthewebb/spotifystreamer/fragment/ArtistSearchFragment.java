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
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONObject;

import java.net.MalformedURLException;
import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract;
import app.com.deanofthewebb.spotifystreamer.model.ParceableArtist;
import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.activity.DetailActivity;
import app.com.deanofthewebb.spotifystreamer.adapter.ArtistAdapter;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import kaaes.spotify.webapi.android.models.TracksPager;
import retrofit.RetrofitError;

import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract.ArtistEntry;
import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract.TrackEntry;



public class ArtistSearchFragment extends Fragment {
    private final String LOG_TAG = ArtistSearchFragment.class.getSimpleName();
    private ArtistAdapter artistResultsAdapter;
    private ArrayList<ParceableArtist> artistsFound;

    private final String PARCEL_ARTISTS = "parcel_artists";

    public static final String ARTIST_ID_EXTRA = "a_id_e";
    public static final String ARTIST_NAME_EXTRA = "a_n_e";

    public ArtistSearchFragment() {
        artistsFound = new ArrayList<ParceableArtist>();
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
        UpdateArtistsOnKeysEntered(rootView);

        if (savedInstanceState != null) {

            artistsFound = savedInstanceState.getParcelableArrayList(PARCEL_ARTISTS);

            List<Artist> artistList = new ArrayList<Artist>();

            for (ParceableArtist parceableArtist : artistsFound) {
                artistList.add(parceableArtist);
            }

            artistResultsAdapter =  new ArtistAdapter(getActivity(), artistList);
        }
        else {
            artistResultsAdapter =  new ArtistAdapter(getActivity(),new ArrayList<Artist>());
        }

        ListView artistResultsView = (ListView) rootView.findViewById(R.id.artist_results_listview);
        artistResultsView.setAdapter(artistResultsAdapter);

        artistResultsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Artist artist = (Artist) artistResultsAdapter.getItem(position);

                Intent artistDetailIntent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(ARTIST_ID_EXTRA, artist.id)
                        .putExtra(ARTIST_NAME_EXTRA, artist.name);

                startActivity(artistDetailIntent);
            }
        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        savedInstanceState.putParcelableArrayList(PARCEL_ARTISTS, artistsFound);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    private void UpdateArtistsOnKeysEntered(View rootView) {
        final SearchView searchText = (SearchView) rootView.findViewById(R.id.search_text);
        searchText.setIconifiedByDefault(false);
        searchText.setQueryHint(getResources().getString(R.string.query_artist_hint));
        searchText.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            final String searchKeyword = searchText.getQuery().toString();
            UpdateArtistResults(searchKeyword);
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            final String searchKeyword = searchText.getQuery().toString();
            //UpdateArtistResults(searchKeyword);
            return false;
        }

        });
    }

    private void UpdateArtistResults(String artistQuery) {
        if (isNetworkAvailable()) {
            FetchArtistsTask artistTask = new FetchArtistsTask();
            artistTask.execute(artistQuery);
        }
        else {
            ShowNoNetworkFoundToast();
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

    public class FetchArtistsTask extends AsyncTask<String, Void, ArtistsPager> {
        private final String LOG_TAG = FetchArtistsTask.class.getSimpleName();


        @Override
        protected ArtistsPager doInBackground(String... params) {
            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();

            ArtistsPager results = new ArtistsPager();

            if (params != null) {
                results = getArtistDataFromSpotifyWrapper(spotify, params[0]);

            }

                return results;
        }

        @Override
        protected void onPostExecute(ArtistsPager results) {
            if (results != null && artistResultsAdapter != null) {
                CreateParceableArtists(results);

                artistResultsAdapter.clear();
                artistResultsAdapter.addAll(artistsFound);
            }
            else {
                Log.d(LOG_TAG, "No results object returned");
            }

            if (artistResultsAdapter.getCount() == 0) {
                ShowNoArtistsFoundToast();
            }
        }

        long addArtist(String name, String apiId, String apiUri, int popularity, String imageUrl) {
            long artistId;

            // First, check if the location with this city name exists in the db
            Cursor artistCursor = getActivity().getContentResolver().query(
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
            ArtistsPager results = new ArtistsPager();;
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
                Log.d(LOG_TAG, "Retrofit error has occured: " + re.getMessage());
            }
            catch (Exception ex) {
                Log.d(LOG_TAG, "An error has occured: " + ex.getMessage());
            }
            finally {
                return results;
            }
        }


        private void insertTracks(TracksPager results, int artistRowId) {
            final int TOP_X_TRACKS = 10;

            if (results.tracks.items.size() > TOP_X_TRACKS) {
                results.tracks.items = results.tracks.items.subList(0, 10);
            }

            Vector<ContentValues> cVVector = new Vector<ContentValues>(TOP_X_TRACKS);

            // Add tracks to Vector
            for (Track track : results.tracks.items) {
                // First, check if the track with this city name exists in the db
                Cursor trackCursor = getActivity().getContentResolver().query(
                        SpotifyStreamerContract.TrackEntry.CONTENT_URI,
                        new String[]{SpotifyStreamerContract.TrackEntry._ID},
                        SpotifyStreamerContract.TrackEntry.COLUMN_API_ID + " = ?",
                        new String[]{track.id},
                        null);

                if (!trackCursor.moveToFirst()) {

                    String trackImageUrl = "";
                    if (!track.album.images.isEmpty()) {
                        Image albumImage = (track.album.images.get(track.album.images.size() - 1));
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
                    trackValues.put(SpotifyStreamerContract.TrackEntry.COLUMN_ARTIST_KEY, artistRowId);

                    cVVector.add(trackValues);
                }
            }

            int inserted = 0;
            // add to database
            if ( cVVector.size() > 0 ) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                inserted = getActivity().getContentResolver().bulkInsert(SpotifyStreamerContract.TrackEntry.CONTENT_URI, cvArray);
            }

            Log.d(LOG_TAG, "SpotifyStreamer Service Complete: " + inserted + " tracks inserted");
        }


        private void CreateParceableArtists(ArtistsPager results) {
            artistsFound.clear();

            for(Artist artist : results.artists.items) {
                if (!artist.images.isEmpty()) {
                    //Image artistImage = artist.images.get(0);

                    Image artistImage = (artist.images.get(artist.images.size() - 1));

                    Log.v(LOG_TAG, "GRABBING ARTIST DATA - NAME: " + artist.name);
                    Log.v(LOG_TAG, "GRABBING ARTIST DATA - API_ID: " + artist.id);
                    Log.v(LOG_TAG, "GRABBING ARTIST DATA - API_URL: " + artist.uri);
                    Log.v(LOG_TAG, "GRABBING ARTIST DATA - POPULARITY: " + artist.popularity);


                    Log.v(LOG_TAG, "GRABBING ARTIST IMAGE DATA - WIDTH " + artistImage.width);
                    Log.v(LOG_TAG, "GRABBING ARTIST IMAGE DATA - HEIGHT: " + artistImage.height);
                    Log.v(LOG_TAG, "GRABBING ARTIST IMAGE DATA - URL: " + artistImage.url);

                    artistsFound.add(new ParceableArtist(artist.name, artist.id, artistImage));
                }
                else {
                    artistsFound.add(new ParceableArtist(artist.name, artist.id, null));
                }
            }
        }

        private void ShowNoArtistsFoundToast() {
            CharSequence text = getString(R.string.no_artists_found);
            int duration = Toast.LENGTH_LONG;
            Toast.makeText(getActivity(), text, duration).show();
        }
    }
}
