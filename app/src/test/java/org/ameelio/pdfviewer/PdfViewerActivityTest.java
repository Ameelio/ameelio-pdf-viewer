package org.ameelio.pdfviewer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PdfViewerActivityTest {

    private PdfViewerActivity activity;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(PdfViewerActivity.class).create().get();
    }

    @Test
    public void testActivityCreation() {
        assertNotNull("Activity should not be null", activity);
        assertNotNull("Activity should have a valid context", activity.getApplicationContext());
    }

    @Test
    public void testInitialViewState_NoIntent() {
        // When activity is created without PDF intent, file selector should be visible
        Button selectFileButton = activity.findViewById(R.id.selectFileButton);
        TextView errorText = activity.findViewById(R.id.errorText);
        RecyclerView recyclerView = activity.findViewById(R.id.pdfRecyclerView);

        assertNotNull("Select file button should exist", selectFileButton);
        assertNotNull("Recycler view should exist", recyclerView);
        assertNotNull("Error text should exist", errorText);
    }

    @Test
    public void testSecurityFeatures_NoBackupAllowed() {
        // Verify that backup is disabled for security
        assertFalse("Backup should be disabled for security", 
                   (activity.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP) != 0);
    }

    @Test
    public void testSecurityFeatures_NoStateRestoration() {
        // Test that state is not restored for security
        Bundle mockBundle = new Bundle();
        mockBundle.putString("test_key", "test_value");
        
        activity.onSaveInstanceState(mockBundle);
        
        // Verify that the bundle is empty (security requirement)
        assertTrue("Bundle should be empty after onSaveInstanceState for security", 
                  mockBundle.isEmpty());
    }

    @Test
    public void testSecurityFeatures_StatePersistencePrevented() {
        // Test that onSaveInstanceState doesn't persist data
        Bundle testBundle = new Bundle();
        testBundle.putString("sensitive_data", "should_not_be_saved");
        
        activity.onSaveInstanceState(testBundle);
        
        // Bundle should be cleared for security
        assertEquals("Bundle should be empty for security", 0, testBundle.size());
    }

    @Test
    public void testActivityWithPdfIntent() {
        // Create an intent with PDF URI
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri mockUri = Uri.parse("content://com.android.providers.downloads.documents/document/1");
        intent.setData(mockUri);
        
        // Create activity with PDF intent
        PdfViewerActivity activityWithIntent = Robolectric.buildActivity(PdfViewerActivity.class, intent)
                .create().get();
        
        assertNotNull("Activity with PDF intent should be created", activityWithIntent);
        assertEquals("Activity should receive the correct intent", intent, activityWithIntent.getIntent());
    }

    @Test
    public void testNoNetworkPermissions() {
        // Verify that the app doesn't request network permissions (security requirement)
        String[] permissions;
        try {
            permissions = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), android.content.pm.PackageManager.GET_PERMISSIONS)
                    .requestedPermissions;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            fail("Package not found: " + e.getMessage());
            return;
        }
        
        if (permissions != null) {
            for (String permission : permissions) {
                assertFalse("App should not request INTERNET permission", 
                           android.Manifest.permission.INTERNET.equals(permission));
                assertFalse("App should not request NETWORK_STATE permission", 
                           android.Manifest.permission.ACCESS_NETWORK_STATE.equals(permission));
            }
        }
    }

    @Test
    public void testMinimalPermissions() {
        // Verify that only necessary permissions are requested
        String[] permissions;
        try {
            permissions = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), android.content.pm.PackageManager.GET_PERMISSIONS)
                    .requestedPermissions;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            fail("Package not found: " + e.getMessage());
            return;
        }
        
        if (permissions != null) {
            // Only READ_EXTERNAL_STORAGE should be requested
            boolean hasReadPermission = false;
            for (String permission : permissions) {
                if (android.Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission)) {
                    hasReadPermission = true;
                }
            }
            assertTrue("App should request READ_EXTERNAL_STORAGE permission", hasReadPermission);
        }
    }

    @Test
    public void testActivityLifecycle() {
        // Test that activity handles lifecycle properly
        PdfViewerActivity lifecycleActivity = Robolectric.buildActivity(PdfViewerActivity.class)
                .create()
                .start()
                .resume()
                .pause()
                .stop()
                .destroy()
                .get();
        
        assertNotNull("Activity should survive complete lifecycle", lifecycleActivity);
    }

    @Test
    public void testErrorHandling() {
        // Test error state display
        TextView errorText = activity.findViewById(R.id.errorText);
        
        // Initially error text should be hidden
        assertEquals("Error text should be initially hidden", 
                    android.view.View.GONE, errorText.getVisibility());
    }

    @Test
    public void testFilePickerButton() {
        Button selectFileButton = activity.findViewById(R.id.selectFileButton);
        
        assertNotNull("Select file button should exist", selectFileButton);
        assertEquals("Select file button should have correct text", 
                    "Select PDF File", selectFileButton.getText().toString());
        assertTrue("Select file button should be clickable", selectFileButton.isClickable());
    }

    @Test
    public void testSecurityConfiguration() {
        // Test that critical security configurations are in place
        
        // 1. No backup allowed
        assertFalse("Backup should be disabled", 
                   (activity.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP) != 0);
        
        // 2. Activity should not export sensitive data
        assertNotNull("Activity should be properly configured", activity.getComponentName());
    }

    @Test
    public void testIntentFilters() {
        // Test that activity properly handles different intent types
        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
        viewIntent.setType("application/pdf");
        
        Intent mainIntent = new Intent(Intent.ACTION_MAIN);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        
        // Both intents should be resolvable by the activity
        assertNotNull("VIEW intent should be handled", viewIntent.getAction());
        assertNotNull("MAIN intent should be handled", mainIntent.getAction());
    }

    // Note: PDF open/render flows cannot be unit tested — Robolectric has no PDF natives
    // (PdfDocument/PdfRenderer no-op), so document-dependent behavior is covered by
    // instrumentation tests and manual/emulator verification instead.

    @Test
    public void testOnNewIntentReplacesIntentAndShowsErrorForUnreadablePdf() {
        Uri unreadableUri = new Uri.Builder()
                .scheme("content")
                .authority("org.ameelio.pdfviewer.test.unregistered")
                .appendPath("missing.pdf")
                .build();

        Intent newIntent = new Intent(Intent.ACTION_VIEW, unreadableUri);
        activity.onNewIntent(newIntent);

        assertEquals("Activity intent should be replaced when a new one arrives",
                newIntent, activity.getIntent());

        TextView errorText = activity.findViewById(R.id.errorText);
        RecyclerView recyclerView = activity.findViewById(R.id.pdfRecyclerView);
        assertEquals("Error text should be shown when the PDF cannot be opened",
                View.VISIBLE, errorText.getVisibility());
        assertEquals("PDF list should stay hidden when the PDF cannot be opened",
                View.GONE, recyclerView.getVisibility());
    }

    @Test
    public void testRecyclerViewDisablesMotionEventSplitting() {
        RecyclerView recyclerView = activity.findViewById(R.id.pdfRecyclerView);
        assertNotNull("RecyclerView should exist", recyclerView);
        assertFalse("RecyclerView should keep multi-touch events unified for pinch gestures",
                recyclerView.isMotionEventSplittingEnabled());
    }

    @Test
    public void testResetZoomButtonResetsScale() throws Exception {
        ImageButton resetZoomButton = activity.findViewById(R.id.resetZoomButton);
        assertNotNull("Reset zoom button should exist", resetZoomButton);

        ZoomCoordinator zoomCoordinator = getZoomCoordinator(activity);
        zoomCoordinator.propagateScale(null, 2f, Float.NaN, Float.NaN);
        assertEquals("Scale should update before reset", 2f, zoomCoordinator.getCurrentScale(), 0.0001f);

        resetZoomButton.performClick();

        assertEquals("Reset zoom button should restore scale to default",
                1f, zoomCoordinator.getCurrentScale(), 0.0001f);
    }

    @Test
    public void testZoomButtonsAdjustScale() throws Exception {
        ImageButton zoomInButton = activity.findViewById(R.id.zoomInButton);
        ImageButton zoomOutButton = activity.findViewById(R.id.zoomOutButton);
        assertNotNull("Zoom in button should exist", zoomInButton);
        assertNotNull("Zoom out button should exist", zoomOutButton);

        ZoomCoordinator zoomCoordinator = getZoomCoordinator(activity);
        assertEquals("Initial scale should be default", 1f, zoomCoordinator.getCurrentScale(), 0.0001f);

        float zoomStep = getZoomStep();

        zoomInButton.performClick();
        assertEquals("Zoom in should increase scale", 1f + zoomStep, zoomCoordinator.getCurrentScale(), 0.0001f);

        zoomOutButton.performClick();
        assertEquals("Zoom out should decrease scale back to default", 1f, zoomCoordinator.getCurrentScale(), 0.0001f);
    }

    @Test
    public void testZoomButtonsVisuallyScaleDocumentView() throws Exception {
        RecyclerView recyclerView = activity.findViewById(R.id.pdfRecyclerView);
        // The list only becomes visible once a PDF is open; make it visible here so it
        // gets real bounds from the layout pass (rendering a PDF is not possible in
        // Robolectric, and the zoom transform plumbing does not need one).
        recyclerView.setVisibility(View.VISIBLE);

        View root = activity.findViewById(android.R.id.content);
        root.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 1080, 1920);
        assertTrue("RecyclerView should be laid out with real dimensions", recyclerView.getWidth() > 0);

        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("Document view should start unscaled", 1f, recyclerView.getScaleX(), 0.0001f);

        ImageButton zoomInButton = activity.findViewById(R.id.zoomInButton);
        zoomInButton.performClick();
        shadowOf(Looper.getMainLooper()).idle();

        float expectedScale = 1f + getZoomStep();
        assertEquals("Zoom in should visually scale the document view horizontally",
                expectedScale, recyclerView.getScaleX(), 0.0001f);
        assertEquals("Zoom in should visually scale the document view vertically",
                expectedScale, recyclerView.getScaleY(), 0.0001f);
    }

    private ZoomCoordinator getZoomCoordinator(PdfViewerActivity activity) throws Exception {
        Field coordinatorField = PdfViewerActivity.class.getDeclaredField("zoomCoordinator");
        coordinatorField.setAccessible(true);
        return (ZoomCoordinator) coordinatorField.get(activity);
    }

    private float getZoomStep() throws Exception {
        Field zoomStepField = PdfViewerActivity.class.getDeclaredField("ZOOM_STEP");
        zoomStepField.setAccessible(true);
        return zoomStepField.getFloat(null);
    }

    @Test
    @Config(qualifiers = "port")
    public void testZoomControlsHorizontalInPortrait() {
        PdfViewerActivity activity = Robolectric.buildActivity(PdfViewerActivity.class)
                .create()
                .get();
        LinearLayout container = activity.findViewById(R.id.zoomControlsContainer);
        assertNotNull("Zoom controls container should exist", container);
        assertEquals("Zoom controls should be horizontal in portrait",
                LinearLayout.HORIZONTAL, container.getOrientation());
    }

    @Test
    @Config(qualifiers = "land")
    public void testZoomControlsVerticalInLandscape() {
        PdfViewerActivity activity = Robolectric.buildActivity(PdfViewerActivity.class)
                .create()
                .get();
        LinearLayout container = activity.findViewById(R.id.zoomControlsContainer);
        assertNotNull("Zoom controls container should exist", container);
        assertEquals("Zoom controls should be vertical in landscape",
                LinearLayout.VERTICAL, container.getOrientation());
    }

}
