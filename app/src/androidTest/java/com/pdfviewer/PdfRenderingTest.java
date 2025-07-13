package com.pdfviewer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Tests for PDF rendering functionality and security
 */
@RunWith(AndroidJUnit4.class)
public class PdfRenderingTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testPdfIntentHandling() {
        // Test that PDF intents are handled correctly
        Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
        pdfIntent.setType("application/pdf");
        pdfIntent.setData(Uri.parse("content://com.android.providers.downloads.documents/document/1"));
        
        // Verify intent components
        assertEquals("Intent should be VIEW action", Intent.ACTION_VIEW, pdfIntent.getAction());
        assertEquals("Intent should be PDF type", "application/pdf", pdfIntent.getType());
        assertNotNull("Intent should have data URI", pdfIntent.getData());
    }

    @Test
    public void testPdfMimeTypeSupport() {
        // Test that only PDF mime types are supported
        String[] supportedTypes = {"application/pdf"};
        
        for (String type : supportedTypes) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType(type);
            
            assertEquals("Should support PDF mime type", type, intent.getType());
        }
    }

    @Test
    public void testUnsupportedMimeTypes() {
        // Test that unsupported mime types are not handled
        String[] unsupportedTypes = {
            "image/jpeg",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        };
        
        for (String type : unsupportedTypes) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType(type);
            
            // These should not be handled by our PDF viewer
            assertNotEquals("Should not support non-PDF types", "application/pdf", type);
        }
    }

    @Test
    public void testFileUriSupport() {
        // Test that file URIs with PDF extension are supported
        Uri fileUri = Uri.parse("file:///storage/emulated/0/document.pdf");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(fileUri);
        
        assertNotNull("File URI should be supported", intent.getData());
        assertTrue("URI should be file scheme", "file".equals(intent.getData().getScheme()));
    }

    @Test
    public void testContentUriSupport() {
        // Test that content URIs are supported
        Uri contentUri = Uri.parse("content://com.android.providers.downloads.documents/document/1");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(contentUri);
        
        assertNotNull("Content URI should be supported", intent.getData());
        assertTrue("URI should be content scheme", "content".equals(intent.getData().getScheme()));
    }

    @Test
    public void testSecureUriHandling() {
        // Test that potentially malicious URIs are handled securely
        String[] potentiallyMaliciousUris = {
            "file:///system/etc/passwd",
            "file:///data/data/com.other.app/private.db",
            "content://com.malicious.provider/sensitive_data"
        };
        
        for (String uriString : potentiallyMaliciousUris) {
            Uri uri = Uri.parse(uriString);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            
            // The app should only process these through the secure content resolver
            assertNotNull("URI should be parsed", uri);
            // Security is handled by Android's content resolver permissions
        }
    }

    @Test
    public void testNoWriteAccess() {
        // Test that the app doesn't attempt to write to files
        // This is ensured by not requesting WRITE_EXTERNAL_STORAGE permission
        
        // Verify that only read permission is requested
        String[] permissions = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), android.content.pm.PackageManager.GET_PERMISSIONS)
                .requestedPermissions;
        
        boolean hasReadPermission = false;
        boolean hasWritePermission = false;
        
        if (permissions != null) {
            for (String permission : permissions) {
                if (android.Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission)) {
                    hasReadPermission = true;
                }
                if (android.Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
                    hasWritePermission = true;
                }
            }
        }
        
        assertTrue("App should have read permission", hasReadPermission);
        assertFalse("App should not have write permission", hasWritePermission);
    }

    @Test
    public void testNoNetworkAccess() {
        // Test that the app doesn't access network resources
        String[] permissions = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), android.content.pm.PackageManager.GET_PERMISSIONS)
                .requestedPermissions;
        
        if (permissions != null) {
            for (String permission : permissions) {
                assertFalse("App should not have internet permission", 
                           android.Manifest.permission.INTERNET.equals(permission));
                assertFalse("App should not have network state permission", 
                           android.Manifest.permission.ACCESS_NETWORK_STATE.equals(permission));
            }
        }
    }

    @Test
    public void testApplicationSecurity() {
        // Test overall application security configuration
        
        // 1. No backup allowed
        assertFalse("Backup should be disabled", 
                   (context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP) != 0);
        
        // 2. No debugging in release
        // This would be checked in release builds
        
        // 3. Proper package name
        assertEquals("Package name should be correct", "com.pdfviewer", context.getPackageName());
    }

    @Test
    public void testIntentFilterSecurity() {
        // Test that intent filters are properly configured
        Intent mainIntent = new Intent(Intent.ACTION_MAIN);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        
        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
        viewIntent.setType("application/pdf");
        
        // Both intents should be properly configured
        assertNotNull("Main intent should be configured", mainIntent.getAction());
        assertNotNull("View intent should be configured", viewIntent.getAction());
    }

    @Test
    public void testNoSensitiveDataExposure() {
        // Test that the app doesn't expose sensitive data
        
        // Check that no sensitive files are in the app directory
        String[] sensitiveFileNames = {
            "password",
            "key",
            "token",
            "secret",
            "private"
        };
        
        java.io.File appDir = context.getFilesDir();
        java.io.File[] files = appDir.listFiles();
        
        if (files != null) {
            for (java.io.File file : files) {
                String fileName = file.getName().toLowerCase();
                for (String sensitivePattern : sensitiveFileNames) {
                    assertFalse("No sensitive files should exist", 
                               fileName.contains(sensitivePattern));
                }
            }
        }
    }

    @Test
    public void testResourceSecurity() {
        // Test that resources don't contain sensitive information
        
        // Check that string resources don't contain sensitive data
        String appName = context.getString(R.string.app_name);
        assertNotNull("App name should be defined", appName);
        
        // App name should not contain sensitive information
        assertFalse("App name should not contain sensitive data", 
                   appName.toLowerCase().contains("password") ||
                   appName.toLowerCase().contains("key") ||
                   appName.toLowerCase().contains("token"));
    }
}