/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.com.deanofthewebb.spotifystreamer.data;

import android.net.Uri;
import android.test.AndroidTestCase;

public class TestSpotifyStreamerContract extends AndroidTestCase {

    // intentionally includes a slash to make sure Uri is getting quoted correctly

    public void testBuildTrackArtist() {
        Uri locationUri = SpotifyStreamerContract.TrackEntry.buildTrackArtist(TestUtilities.TEST_ARTIST_API_ID);
        assertNotNull("Error: Null Uri returned.  You must fill-in buildTRACKARTIST in " +
                        "SpotifyStreamerContract.",
                locationUri);
        assertEquals("Error: Track Artist not properly appended to the end of the Uri",
                TestUtilities.TEST_ARTIST_API_ID, locationUri.getLastPathSegment());
        assertEquals("Error: Weather location Uri doesn't match our expected result",
                locationUri.toString(),
                "content://app.com.deanofthewebb.spotifystreamer/track/3fMbdgg4jU18AjLCKBhRSm");
    }


}
