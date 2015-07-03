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
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistSearchFragment extends Fragment {
    private final String LOG_TAG = ArtistSearchFragment.class.getSimpleName();
    private ArrayAdapter<String> artistResultsAdapter;


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

        artistResultsAdapter =  new ArrayAdapter<String>(
                                    getActivity(), // The current context (this activity)
                                    R.layout.artist_result, // The name of the layout ID.
                                    R.id.artist_name_textview, // The ID of the textview to populate.
                                    new ArrayList<String>());

        ListView listView = (ListView) rootView.findViewById(R.id.artist_results_listview);
        listView.setAdapter(artistResultsAdapter);

        return rootView;
    }

    private void UpdateArtistResults(String artistQuery) {
        FetchArtistsTask artistTask = new FetchArtistsTask();

        Log.v(LOG_TAG, "Artist entered in text field: " + artistQuery);
        artistTask.execute(artistQuery);
    }

    public class FetchArtistsTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchArtistsTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {

            //Grab string from edit text
            //Query Spotify API with it

            String[] fakeData = {
                    "Coldplay",
                    "Coldplay & Lele",
                    "Coldplay & Kanye",
                    "Coldplay & Rihanna",
                    "Coldplay & Me",
                    "Various Artists - A Coldplay Tribute"
            };
            return fakeData;
        }

        @Override
        protected void onPostExecute(String[] results) {

            if (results != null && artistResultsAdapter != null) {
                artistResultsAdapter.clear();

                ArrayList<String> artists = new ArrayList<String>(Arrays.asList(results));
                artistResultsAdapter.addAll(artists);
            }
        }
    }
}
