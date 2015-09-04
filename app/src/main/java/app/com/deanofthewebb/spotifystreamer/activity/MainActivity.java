package app.com.deanofthewebb.spotifystreamer.activity;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.data.SpotifyStreamerContract;
import app.com.deanofthewebb.spotifystreamer.fragment.ArtistSearchFragment;
import app.com.deanofthewebb.spotifystreamer.fragment.ArtistTracksFragment;
import app.com.deanofthewebb.spotifystreamer.fragment.PlaybackFragment;
import app.com.deanofthewebb.spotifystreamer.helpers.Constants;
import app.com.deanofthewebb.spotifystreamer.helpers.Utility;


public class MainActivity extends ActionBarActivity implements ArtistSearchFragment.Callback, ArtistTracksFragment.Callback {
    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private boolean mTwoPane;
    private boolean mIsLargeLayout = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (findViewById(R.id.track_detail_container) != null) {
            mTwoPane = true;
            if (savedInstanceState == null) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.track_detail_container, new ArtistTracksFragment(), Constants.TAG.DETAILFRAGMENT)
                        .commit();
            }
        } else {
            mTwoPane = false;
        }

        mIsLargeLayout = getResources().getBoolean(R.bool.large_layout);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(Uri artistUri) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putParcelable(ArtistTracksFragment.DETAIL_URI, artistUri);

            ArtistTracksFragment fragment = new ArtistTracksFragment();
            fragment.setArguments(args);

            getFragmentManager().beginTransaction()
                    .replace(R.id.track_detail_container, fragment, Constants.TAG.DETAILFRAGMENT)
                    .commit();
        } else {
            Intent intent = new Intent(this, DetailActivity.class)
                    .setData(artistUri);
            startActivity(intent);
        }
    }


    @Override
    public void onTrackSelected(String trackRowId) {
        Utility.showPlaybackDialog(MainActivity.this, trackRowId, mIsLargeLayout);
    }
}
