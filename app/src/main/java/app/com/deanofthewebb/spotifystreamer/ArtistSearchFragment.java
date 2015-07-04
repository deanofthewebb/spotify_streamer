package app.com.deanofthewebb.spotifystreamer;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Artists;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import retrofit.RetrofitError;


/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistSearchFragment extends Fragment {
    private final String LOG_TAG = ArtistSearchFragment.class.getSimpleName();
    private ArtistAdapter artistResultsAdapter;


    public ArtistSearchFragment() {
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

        final TextView artistView = (TextView) rootView.findViewById(R.id.query_artist);
        artistView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.ACTION_MULTIPLE || event.getAction() == KeyEvent.ACTION_DOWN) {
                    String artistStr = artistView.getText().toString();
                    Log.v(LOG_TAG, "KEYS CLICKED: " + artistStr);
                    UpdateArtistResults(artistStr);
                    return true;
                }
                return true;
            }
        });

        artistResultsAdapter =  new ArtistAdapter(getActivity(),new ArrayList<Artist>());

        ListView listView = (ListView) rootView.findViewById(R.id.artist_results_listview);

        listView.setAdapter(artistResultsAdapter);

        return rootView;
    }

    private void UpdateArtistResults(String artistQuery) {
        FetchArtistsTask artistTask = new FetchArtistsTask();

        Log.v(LOG_TAG, "Artist entered in text field: " + artistQuery);
        artistTask.execute(artistQuery);
    }

    public class FetchArtistsTask extends AsyncTask<String, Void, ArtistsPager> {

        private final String LOG_TAG = FetchArtistsTask.class.getSimpleName();


        @Override
        protected ArtistsPager doInBackground(String... params) {

            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();

            //TODO: Handle case where no artists found
            ArtistsPager results = new ArtistsPager();

            for (String param : params) {
                results = spotify.searchArtists(param);

                for ( Artist artist : results.artists.items) {
                    Log.v(LOG_TAG, "ARTIST Results from Spotify API: " + artist.name);
                }
            }



            return results;
        }

        @Override
        protected void onPostExecute(ArtistsPager results) {

            if (results != null && artistResultsAdapter != null) {
                artistResultsAdapter.clear();

                artistResultsAdapter.addAll(results.artists.items);
            }
            else {
                Log.d(LOG_TAG, "No results found");
            }
        }
    }
}
