package app.com.deanofthewebb.spotifystreamer;

import android.app.ActionBar;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;


public class ArtistDetailActivityFragment extends Fragment {
    private final String LOG_TAG = ArtistDetailActivityFragment.class.getSimpleName();
    private TrackAdapter trackResultsAdapter;

    public ArtistDetailActivityFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_artist_detail, container, false);
        Intent artistDetailIntent = getActivity().getIntent();
        trackResultsAdapter = new TrackAdapter(getActivity(),new ArrayList<Track>());

        ListView trackResultsView = (ListView) rootView.findViewById(R.id.track_results_listview);
        trackResultsView.setAdapter(trackResultsAdapter);

        if (artistDetailIntent != null && artistDetailIntent.hasExtra(Intent.EXTRA_TEXT)) {
            String artistId = artistDetailIntent.getStringExtra(Intent.EXTRA_TEXT);
            String artistName = artistDetailIntent.getStringExtra(Intent.EXTRA_TITLE);

            ((ArtistDetailActivity)getActivity()).setActionBarSubTitle(artistName);
            UpdateTopTracks(artistId);
        }
        else {
            Log.d(LOG_TAG, "Intent passed is null.");
        }


        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_artist_detail, menu);
    }

    private void UpdateTopTracks(String artistID) {
        FetchTopTracksTask topTracksTask = new FetchTopTracksTask();
        topTracksTask.execute(artistID);

    }


    public class FetchTopTracksTask extends AsyncTask<String, Void, Tracks> {
        private final String LOG_TAG = FetchTopTracksTask.class.getSimpleName();


        @Override
        protected Tracks doInBackground(String... params) {

            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();


            Tracks results = new Tracks();

            if (params != null) {
                Log.v(LOG_TAG, "Artist ID: " + params[0]);
                Map<String, Object> options = new HashMap<String, Object>() {
                };
                options.put(SpotifyService.COUNTRY, "US");

                results = spotify.getArtistTopTrack(params[0], options);

                for(Track track : results.tracks) {
                    Log.v(LOG_TAG, "Track found: " + track.name);
                    Log.v(LOG_TAG, "Album for track found: " + track.album.name);
                }
            }

            return results;
        }

        @Override
        protected void onPostExecute(Tracks results) {

            if (results != null && trackResultsAdapter != null) {
                trackResultsAdapter.clear();

                if (results.tracks.size() > 10) {
                    trackResultsAdapter.addAll(results.tracks.subList(0, 10));
                }
                else {
                    trackResultsAdapter.addAll(results.tracks);
                }
            }
            else {
                Log.d(LOG_TAG, "No results object returned");
            }

            if (trackResultsAdapter != null && trackResultsAdapter.getCount() == 0) {
                ShowNoTracksFoundToast();
            }
        }

        private void ShowNoTracksFoundToast() {
            CharSequence text = getString(R.string.no_tracks_found);
            int duration = Toast.LENGTH_LONG;
            Toast.makeText(getActivity(), text, duration).show();
        }
    }
}
