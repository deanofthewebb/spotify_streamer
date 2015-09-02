package app.com.deanofthewebb.spotifystreamer.activity;


import android.content.Intent;
import android.support.v7.app.ActionBarActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import app.com.deanofthewebb.spotifystreamer.R;
import app.com.deanofthewebb.spotifystreamer.fragment.ArtistTracksFragment;
import app.com.deanofthewebb.spotifystreamer.fragment.PlaybackFragment;
import app.com.deanofthewebb.spotifystreamer.model.ParceableTrack;

public class PlaybackActivity extends ActionBarActivity implements PlaybackFragment.Callback{
    private final String LOG_TAG = PlaybackActivity.class.getSimpleName();
    private final String PLAYBACKFRAGMENT_TAG = "PFTAG";
    private boolean mTwoPane = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.playback_activity_container, new PlaybackFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_playback, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNewTrackSelected(String trackId, Bundle bundle) {


        if (mTwoPane) {
            Bundle args = new Bundle();
            args.putString(PlaybackFragment.TRACK_DATA, trackId);

            PlaybackFragment fragment = new PlaybackFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.playback_activity_container, fragment, PLAYBACKFRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, PlaybackActivity.class)
                    .putExtras(bundle)
                    .setAction(trackId)
                    .putExtra(ArtistTracksFragment.TRACK_ID_EXTRA, trackId)
                    .putParcelableArrayListExtra(ArtistTracksFragment.TRACK_LIST_EXTRA,
                            getIntent().getParcelableArrayListExtra(ArtistTracksFragment.TRACK_LIST_EXTRA));
            startActivity(intent);

        }
    }
}

