package org.ameelio.pdfviewer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.appcompat.app.ActionBar;
import android.widget.ImageView;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Verifies that the activity configures the system action bar with both logo and title.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ActionBarTest {

    @Test
    public void actionBarShowsLogoAndTitle() {
        PdfViewerActivity activity = Robolectric.buildActivity(PdfViewerActivity.class)
                .setup()
                .get();

        ActionBar actionBar = activity.getSupportActionBar();
        assertNotNull("Support action bar should be available", actionBar);
        assertTrue("Action bar should be displaying a custom view",
                (actionBar.getDisplayOptions() & ActionBar.DISPLAY_SHOW_CUSTOM) != 0);

        android.view.View customView = actionBar.getCustomView();
        assertNotNull("Custom action bar view should exist", customView);

        ImageView logo = customView.findViewById(R.id.actionBarLogo);
        TextView title = customView.findViewById(R.id.actionBarTitle);

        assertNotNull("Custom view should contain a logo ImageView", logo);
        assertNotNull("Custom view should contain a title TextView", title);
        assertEquals("Ameelio PDF Viewer title should be visible",
                activity.getString(R.string.app_name),
                title.getText().toString());
    }
}
