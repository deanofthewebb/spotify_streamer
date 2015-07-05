package app.com.deanofthewebb.spotifystreamer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;


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
