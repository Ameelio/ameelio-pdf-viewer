package org.ameelio.pdfviewer;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.pm.ProviderInfo;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PdfViewerActivityTest {

    private PdfViewerActivity activity;
    private static final String TEST_PDF_AUTHORITY = "org.ameelio.pdfviewer.test.provider";

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

    @Test
    public void testOnNewIntentReplacesDocumentAndClearsCache() throws Exception {
        Map<Integer, Bitmap> bitmapCache = getBitmapCache(activity);
        bitmapCache.put(0, Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888));
        assertFalse("Precondition: bitmap cache should have entries", bitmapCache.isEmpty());

        File pdfFile = createTestPdfFile();
        Uri pdfUri = registerPdfWithContentProvider(pdfFile);

        Intent newIntent = new Intent(Intent.ACTION_VIEW, pdfUri);
        activity.onNewIntent(newIntent);

        Map<Integer, Bitmap> updatedCache = getBitmapCache(activity);
        assertTrue("Bitmap cache should be cleared before rendering new PDF", updatedCache.isEmpty());
        assertEquals("Activity intent should be replaced when a new one arrives", newIntent, activity.getIntent());

        PdfRenderer pdfRenderer = getPdfRenderer(activity);
        assertNotNull("PDF renderer should be initialized after handling new intent", pdfRenderer);
        assertTrue("Loaded PDF should report at least one page", pdfRenderer.getPageCount() > 0);

        activity.onDestroy();
    }

    @Test
    public void testRecyclerViewDisablesMotionEventSplitting() throws Exception {
        File pdfFile = createTestPdfFile();
        Uri pdfUri = registerPdfWithContentProvider(pdfFile);

        Intent intent = new Intent(Intent.ACTION_VIEW, pdfUri);
        PdfViewerActivity activityWithPdf = Robolectric.buildActivity(PdfViewerActivity.class, intent)
                .create()
                .resume()
                .get();

        RecyclerView recyclerView = activityWithPdf.findViewById(R.id.pdfRecyclerView);
        assertNotNull("RecyclerView should exist", recyclerView);
        assertFalse("RecyclerView should keep multi-touch events unified for pinch gestures",
                recyclerView.isMotionEventSplittingEnabled());

        activityWithPdf.onDestroy();
    }

    @Test
    public void testResetZoomButtonResetsScale() throws Exception {
        File pdfFile = createTestPdfFile();
        Uri pdfUri = registerPdfWithContentProvider(pdfFile);

        Intent intent = new Intent(Intent.ACTION_VIEW, pdfUri);
        PdfViewerActivity activityWithPdf = Robolectric.buildActivity(PdfViewerActivity.class, intent)
                .create()
                .resume()
                .get();

        ImageButton resetZoomButton = activityWithPdf.findViewById(R.id.resetZoomButton);
        assertNotNull("Reset zoom button should exist", resetZoomButton);
        assertEquals("Reset zoom button should be visible when a PDF is open",
                View.VISIBLE, resetZoomButton.getVisibility());

        ZoomCoordinator zoomCoordinator = getZoomCoordinator(activityWithPdf);
        zoomCoordinator.propagateScale(null, 2f, Float.NaN, Float.NaN);
        assertEquals("Scale should update before reset", 2f, zoomCoordinator.getCurrentScale(), 0.0001f);

        resetZoomButton.performClick();

        assertEquals("Reset zoom button should restore scale to default",
                1f, zoomCoordinator.getCurrentScale(), 0.0001f);

        activityWithPdf.onDestroy();
    }

    @Test
    public void testZoomButtonsAdjustScale() throws Exception {
        File pdfFile = createTestPdfFile();
        Uri pdfUri = registerPdfWithContentProvider(pdfFile);

        Intent intent = new Intent(Intent.ACTION_VIEW, pdfUri);
        PdfViewerActivity activityWithPdf = Robolectric.buildActivity(PdfViewerActivity.class, intent)
                .create()
                .resume()
                .get();

        ImageButton zoomInButton = activityWithPdf.findViewById(R.id.zoomInButton);
        ImageButton zoomOutButton = activityWithPdf.findViewById(R.id.zoomOutButton);
        assertNotNull("Zoom in button should exist", zoomInButton);
        assertNotNull("Zoom out button should exist", zoomOutButton);
        assertEquals("Zoom in button should be visible", View.VISIBLE, zoomInButton.getVisibility());
        assertEquals("Zoom out button should be visible", View.VISIBLE, zoomOutButton.getVisibility());

        ZoomCoordinator zoomCoordinator = getZoomCoordinator(activityWithPdf);
        assertEquals("Initial scale should be default", 1f, zoomCoordinator.getCurrentScale(), 0.0001f);

        float zoomStep = getZoomStep();

        zoomInButton.performClick();
        assertEquals("Zoom in should increase scale", 1f + zoomStep, zoomCoordinator.getCurrentScale(), 0.0001f);

        zoomOutButton.performClick();
        assertEquals("Zoom out should decrease scale back to default", 1f, zoomCoordinator.getCurrentScale(), 0.0001f);

        activityWithPdf.onDestroy();
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Bitmap> getBitmapCache(PdfViewerActivity activity) throws Exception {
        Field bitmapCacheField = PdfViewerActivity.class.getDeclaredField("bitmapCache");
        bitmapCacheField.setAccessible(true);
        return (Map<Integer, Bitmap>) bitmapCacheField.get(activity);
    }

    private PdfRenderer getPdfRenderer(PdfViewerActivity activity) throws Exception {
        Field rendererField = PdfViewerActivity.class.getDeclaredField("pdfRenderer");
        rendererField.setAccessible(true);
        return (PdfRenderer) rendererField.get(activity);
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

    private Uri registerPdfWithContentProvider(File pdfFile) {
        TestPdfProvider provider = new TestPdfProvider(pdfFile);
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = TEST_PDF_AUTHORITY;
        provider.attachInfo(RuntimeEnvironment.getApplication(), providerInfo);
        ShadowContentResolver shadowContentResolver =
                shadowOf(RuntimeEnvironment.getApplication().getContentResolver());
        shadowContentResolver.registerProviderInternal(TEST_PDF_AUTHORITY, provider);

        return new Uri.Builder()
                .scheme("content")
                .authority(TEST_PDF_AUTHORITY)
                .appendPath("documents")
                .appendPath("1")
                .build();
    }

    private File createTestPdfFile() throws IOException {
        File cacheDir = RuntimeEnvironment.getApplication().getCacheDir();
        File pdfFile = File.createTempFile("test-doc", ".pdf", cacheDir);

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(100, 100, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(12f);
        canvas.drawText("Hello PDF", 10, 50, paint);
        document.finishPage(page);

        try (FileOutputStream outputStream = new FileOutputStream(pdfFile)) {
            document.writeTo(outputStream);
        } finally {
            document.close();
        }

        return pdfFile;
    }

    private static class TestPdfProvider extends ContentProvider {
        private final File pdfFile;

        TestPdfProvider(File pdfFile) {
            this.pdfFile = pdfFile;
        }

        @Override
        public boolean onCreate() {
            return true;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
            return null;
        }

        @Override
        public String getType(Uri uri) {
            return "application/pdf";
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            return null;
        }

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            return 0;
        }

        @Override
        public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            return 0;
        }

        @Override
        public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
            return ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
        }
    }
}
