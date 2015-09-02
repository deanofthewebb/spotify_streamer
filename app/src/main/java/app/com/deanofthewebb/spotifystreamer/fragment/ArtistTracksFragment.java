package app.com.deanofthewebb.spotifystreamer.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.com.deanofthewebb.spotifystreamer.activity.PlaybackActivity;
import app.com.deanofthewebb.spotifystreamer.model.ParceableTrack;
import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.activity.DetailActivity;
import app.com.deanofthewebb.spotifystreamer.adapter.TrackAdapter;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.RetrofitError;


public class ArtistTracksFragment extends Fragment {
    private final String LOG_TAG = ArtistTracksFragment.class.getSimpleName();
    private final String PARCEL_TRACKS = "parcel_tracks";
    private TrackAdapter trackResultsAdapter;
    private ArrayList<ParceableTrack> mTracksFound;
    private String artistId;
    private String artistName;


    public static final String TRACK_ID_EXTRA = "t_n_e";
    public static final String TRACK_LIST_EXTRA = "t_l_e";

    public ArtistTracksFragment() {
        setHasOptionsMenu(true);
        mTracksFound = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        Intent artistDetailIntent = getActivity().getIntent();

        if (savedInstanceState != null) {
            mTracksFound = savedInstanceState.getParcelableArrayList(PARCEL_TRACKS);

            List<Track> trackList = new ArrayList<Track>();
            for (ParceableTrack parceableTrack : mTracksFound) {
                trackList.add(parceableTrack);
            }

            trackResultsAdapter = new TrackAdapter(getActivity(), trackList);
        }
        else {
            trackResultsAdapter = new TrackAdapter(getActivity(),new ArrayList<Track>());
        }

        ListView trackResultsView = (ListView) rootView.findViewById(R.id.track_results_listview);
        trackResultsView.setAdapter(trackResultsAdapter);

        if (artistDetailIntent != null && artistDetailIntent.hasExtra(ArtistSearchFragment.ARTIST_ID_EXTRA)) {
            artistId = artistDetailIntent.getStringExtra(ArtistSearchFragment.ARTIST_ID_EXTRA);
            artistName = artistDetailIntent.getStringExtra(ArtistSearchFragment.ARTIST_NAME_EXTRA);

            ((DetailActivity)getActivity()).setActionBarSubTitle(artistName);
            UpdateTopTracks(artistId);
        } else { Log.e(LOG_TAG, "Intent passed is null!"); }

        trackResultsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Track track = trackResultsAdapter.getItem(position);
                Intent playbackIntent = getPlaybackIntent(track.id);

                startActivity(playbackIntent);
            }
        });

        return rootView;
    }

    private Intent getPlaybackIntent(String trackId) {
        return new Intent(getActivity(), PlaybackActivity.class)
                            .putExtra(TRACK_ID_EXTRA, trackId)
                            .putExtra(TRACK_LIST_EXTRA, mTracksFound);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putParcelableArrayList(PARCEL_TRACKS, mTracksFound);
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
        if (artistId != null) { UpdateTopTracks(artistId); }
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

            try {
                    Tracks results = new Tracks();
                    if (params != null) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        String countryCode = preferences.getString(getString(R.string.pref_country_key), getString(R.string.pref_country_code_usa));

                        Map<String, Object> options = new HashMap<String, Object>() {};
                        options.put(SpotifyService.COUNTRY, countryCode);

                        results = spotify.getArtistTopTrack(params[0], options);
                    }

                    return results;
            }
            catch (RetrofitError re) {
                Log.d(LOG_TAG, "Retrofit error has occured: " + re.getMessage());
                return null;
            }
            catch (Exception ex) {
                Log.d(LOG_TAG, "An unexpected error has occured: " + ex.getMessage());
                return null;
            }
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
            else { Log.d(LOG_TAG, "No results object returned"); }

            if (trackResultsAdapter != null && trackResultsAdapter.getCount() == 0) {
                ShowNoTracksFoundToast();
            }
        }

        private void CreateParceableTracks(Tracks results) {
            mTracksFound.clear();

            for(Track track : results.tracks) {
                Artist artist = new Artist();
                artist.name = artistName;

                if (!track.album.images.isEmpty()) {
                    Image artistImage = track.album.images.get(0);
                    mTracksFound.add(new ParceableTrack(track.name, track.album.name, artistImage, artist));
                }
                else{
                    mTracksFound.add(new ParceableTrack(track.name, track.album.name, new Image(), artist));
                }
            }

            Log.d(LOG_TAG, "TRACKS FOUND: " + mTracksFound.size());
        }

        private void ShowNoTracksFoundToast() {
            CharSequence text = getString(R.string.no_tracks_found);
            int duration = Toast.LENGTH_LONG;
            Toast.makeText(getActivity(), text, duration).show();
        }
    }
}
