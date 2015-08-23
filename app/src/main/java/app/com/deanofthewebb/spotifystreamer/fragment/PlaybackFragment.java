package app.com.deanofthewebb.spotifystreamer.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import app.com.deanofthewebb.spotifystreamer.R;

public class PlaybackFragment extends Fragment {

    protected static final String ARTIST_KEY = "a_n_k";
    protected static final String TRACK_KEY = "a_n_k";

    public PlaybackFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playback, container, false);

        FetchTrackInfoTask trackInfoTask = new FetchTrackInfoTask();
        trackInfoTask.execute(getActivity());
        return rootView;
    }
}