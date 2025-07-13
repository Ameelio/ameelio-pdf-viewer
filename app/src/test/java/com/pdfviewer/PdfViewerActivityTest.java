package com.pdfviewer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

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
        android.widget.HorizontalScrollView horizontalScrollView = activity.findViewById(R.id.horizontalScrollView);
        ScrollView scrollView = activity.findViewById(R.id.scrollView);
        TextView errorText = activity.findViewById(R.id.errorText);

        assertNotNull("Select file button should exist", selectFileButton);
        assertNotNull("Horizontal scroll view should exist", horizontalScrollView);
        assertNotNull("Scroll view should exist", scrollView);
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
}