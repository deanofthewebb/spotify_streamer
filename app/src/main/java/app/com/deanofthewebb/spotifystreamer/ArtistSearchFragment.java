package app.com.deanofthewebb.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import retrofit.RetrofitError;


public class ArtistSearchFragment extends Fragment {
    private final String LOG_TAG = ArtistSearchFragment.class.getSimpleName();
    private ArtistAdapter artistResultsAdapter;
    private ArrayList<ParceableArtist> artistsFound;

    private final String PARCEL_ARTISTS = "parcel_artists";

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

                Intent artistDetailIntent = new Intent(getActivity(), ArtistDetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, artist.id)
                        .putExtra(Intent.EXTRA_TITLE, artist.name);

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
            UpdateArtistResults(searchKeyword);
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
            try {
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();

                ArtistsPager results = new ArtistsPager();

                if (params != null) {
                    results = spotify.searchArtists(params[0]);
                }

                return results;
            }
            catch (RetrofitError re) {
                Log.d(LOG_TAG, "Retrofit error has occured: " + re.getMessage());
                return null;
            }
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

        private void CreateParceableArtists(ArtistsPager results) {
            artistsFound.clear();

            for(Artist artist : results.artists.items) {
                if (!artist.images.isEmpty()) {
                    Image artistImage = artist.images.get(0);
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
