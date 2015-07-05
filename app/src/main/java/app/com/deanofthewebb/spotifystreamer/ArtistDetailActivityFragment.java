package app.com.deanofthewebb.spotifystreamer;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import kaaes.spotify.webapi.android.models.TracksPager;


public class ArtistDetailActivityFragment extends Fragment {
    private final String LOG_TAG = ArtistDetailActivityFragment.class.getSimpleName();
    private final String PARCEL_TRACKS = "parcel_tracks";
    private TrackAdapter trackResultsAdapter;
    private ArrayList<ParceableTrack> tracksFound;

    public ArtistDetailActivityFragment() {
        setHasOptionsMenu(true);
        tracksFound = new ArrayList<ParceableTrack>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist_detail, container, false);
        Intent artistDetailIntent = getActivity().getIntent();

        if (savedInstanceState != null) {

            tracksFound = savedInstanceState.getParcelableArrayList(PARCEL_TRACKS);

            List<Track> trackList = new ArrayList<Track>();
            for (ParceableTrack parceableTrack : tracksFound) {
                trackList.add(parceableTrack);
            }

            trackResultsAdapter = new TrackAdapter(getActivity(), trackList);
        }
        else {
            trackResultsAdapter = new TrackAdapter(getActivity(),new ArrayList<Track>());
        }


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
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putParcelableArrayList(PARCEL_TRACKS, tracksFound);
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
                Map<String, Object> options = new HashMap<String, Object>() {};
                options.put(SpotifyService.COUNTRY, "US");

                results = spotify.getArtistTopTrack(params[0], options);
            }

            return results;
        }

        @Override
        protected void onPostExecute(Tracks results) {
            if (results != null && trackResultsAdapter != null) {
                trackResultsAdapter.clear();

                if (results.tracks.size() > 10) {
                    results.tracks = results.tracks.subList(0, 10);
                }

                trackResultsAdapter.addAll(results.tracks);
                CreateParceableTracks(results);
            }
            else {
                Log.d(LOG_TAG, "No results object returned");
            }

            if (trackResultsAdapter != null && trackResultsAdapter.getCount() == 0) {
                ShowNoTracksFoundToast();
            }
        }

        private void CreateParceableTracks(Tracks results) {
            tracksFound.clear();

            for(Track track : results.tracks) {
                if (!track.album.images.isEmpty()) {
                    Image artistImage = track.album.images.get(0);
                    tracksFound.add(new ParceableTrack(track.name, track.album.name, artistImage));
                }
                else{
                    tracksFound.add(new ParceableTrack(track.name, track.album.name, null));
                }
            }
        }

        private void ShowNoTracksFoundToast() {
            CharSequence text = getString(R.string.no_tracks_found);
            int duration = Toast.LENGTH_LONG;
            Toast.makeText(getActivity(), text, duration).show();
        }
    }
}
