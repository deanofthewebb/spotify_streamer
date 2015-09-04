package app.com.deanofthewebb.spotifystreamer.activity;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import app.com.deanofthewebb.spotifystreamer.fragment.ArtistTracksFragment;
import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.helpers.Utility;


public class DetailActivity extends AppCompatActivity
                                    implements ArtistTracksFragment.Callback {
    private boolean mIsLargeLayout = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {

            mIsLargeLayout = getResources().getBoolean(R.bool.large_layout);

            // Create the detail fragment and add it to the activity
            // using a fragment transaction.

            Bundle arguments = new Bundle();
            arguments.putParcelable(ArtistTracksFragment.DETAIL_URI, getIntent().getData());

            ArtistTracksFragment fragment = new ArtistTracksFragment();
            fragment.setArguments(arguments);

            getFragmentManager().beginTransaction()
                    .add(R.id.track_detail_container, fragment)
                            .commit();
        }
    }


    public void setActionBarSubTitle (String subTitle) {
        ActionBar supportActionBar =  getSupportActionBar();

        if (supportActionBar !=null ) {
            supportActionBar.setSubtitle(subTitle);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_artist_detail, menu);
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
    public void onTrackSelected(String TrackRowId) {
        Utility.showPlaybackDialog(DetailActivity.this, TrackRowId, mIsLargeLayout);
    }
}
