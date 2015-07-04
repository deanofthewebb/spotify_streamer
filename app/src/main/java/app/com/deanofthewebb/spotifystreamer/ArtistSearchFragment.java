package app.com.deanofthewebb.spotifystreamer;

import android.content.Intent;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
        UpdateArtistsOnKeysEntered(rootView);

        artistResultsAdapter =  new ArtistAdapter(getActivity(),new ArrayList<Artist>());

        ListView artistResultsView = (ListView) rootView.findViewById(R.id.artist_results_listview);
        artistResultsView.setAdapter(artistResultsAdapter);

        //Top Tracks Activity
        artistResultsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Artist artist = (Artist) artistResultsAdapter.getItem(position);

                Intent artistDetailIntent = new Intent(getActivity(), ArtistDetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, artist.id);

                startActivity(artistDetailIntent);
            }
        });



        return rootView;
    }

    private void UpdateArtistsOnKeysEntered(View rootView) {
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
    }

    private void UpdateArtistResults(String artistQuery) {
        FetchArtistsTask artistTask = new FetchArtistsTask();
        artistTask.execute(artistQuery);

    }

    public class FetchArtistsTask extends AsyncTask<String, Void, ArtistsPager> {
        private final String LOG_TAG = FetchArtistsTask.class.getSimpleName();


        @Override
        protected ArtistsPager doInBackground(String... params) {

            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();

            ArtistsPager results = new ArtistsPager();

            if (params != null) {
                results = spotify.searchArtists(params[0]);
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
                Log.d(LOG_TAG, "No results object returned");
            }

            if (artistResultsAdapter.getCount() == 0) {
                ShowNoArtistsFoundToast();
            }
        }

        private void ShowNoArtistsFoundToast() {
            CharSequence text = getString(R.string.no_artists_found);
            int duration = Toast.LENGTH_LONG;
            Toast.makeText(getActivity(), text, duration).show();
        }
    }
}
