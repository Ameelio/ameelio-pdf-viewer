package org.ameelio.pdfviewer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PdfViewerActivity extends AppCompatActivity {

    private static final String TAG = "PdfViewerActivity";

    private Button selectFileButton;
    private HorizontalScrollView horizontalScrollView;
    private ScrollView scrollView;
    private LinearLayout pdfContainer;
    private TextView errorText;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    // Performance optimization variables
    private Map<Integer, ZoomableImageView> pageViews = new HashMap<>();
    private Map<Integer, Bitmap> bitmapCache = new HashMap<>();
    private static final int MAX_CACHED_PAGES = 5;
    private int currentVisiblePage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);
        
        selectFileButton = findViewById(R.id.selectFileButton);
        horizontalScrollView = findViewById(R.id.horizontalScrollView);
        scrollView = findViewById(R.id.scrollView);
        pdfContainer = findViewById(R.id.pdfContainer);
        errorText = findViewById(R.id.errorText);
        
        // Initialize file picker launcher (no persistence)
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            openPdf(uri);
                        }
                    }
                }
            }
        );
        
        selectFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });
        
        Intent intent = getIntent();
        Uri pdfUri = intent.getData();
        
        if (pdfUri != null) {
            openPdf(pdfUri);
        }
    }
    
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        // No persistence - don't save recent files or history
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        filePickerLauncher.launch(intent);
    }
    
    private void openPdf(Uri uri) {
        Log.i(TAG, "Attempting to open PDF: " + uri);
        logMemoryInfo("Before opening PDF");
        try {
            parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            if (parcelFileDescriptor != null) {
                long fileSize = parcelFileDescriptor.getStatSize();
                Log.i(TAG, "PDF file size: " + fileSize + " bytes (" + (fileSize / 1024 / 1024) + " MB)");

                pdfRenderer = new PdfRenderer(parcelFileDescriptor);
                int pageCount = pdfRenderer.getPageCount();
                Log.i(TAG, "PDF opened successfully. Pages: " + pageCount);
                logMemoryInfo("After opening PDF");

                hideFileSelector();
                renderPdfPages();
            } else {
                String errorMsg = "Failed to open file descriptor for PDF";
                Log.e(TAG, errorMsg);
                showError(errorMsg);
            }
        } catch (FileNotFoundException e) {
            String errorMsg = "PDF file not found: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            showError(errorMsg);
        } catch (SecurityException e) {
            String errorMsg = "Permission denied to access PDF: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            showError(errorMsg);
        } catch (IOException e) {
            String errorMsg = "Error reading PDF file: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            showError(errorMsg);
        } catch (OutOfMemoryError e) {
            String errorMsg = "Out of memory loading PDF. File may be too large.\nSize: " +
                (parcelFileDescriptor != null ? (parcelFileDescriptor.getStatSize() / 1024 / 1024) + " MB" : "unknown");
            Log.e(TAG, errorMsg, e);
            logMemoryInfo("After OOM");
            showError(errorMsg);

            // Clean up on OOM
            clearBitmapCache();
            System.gc();
        } catch (Exception e) {
            String errorMsg = "Unexpected error opening PDF: " + e.getClass().getSimpleName() + ": " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            showError(errorMsg);
        }
    }
    
    private void hideFileSelector() {
        selectFileButton.setVisibility(View.GONE);
        horizontalScrollView.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
    }
    
    private void renderPdfPages() {
        try {
            int pageCount = pdfRenderer.getPageCount();
            Log.i(TAG, "Rendering " + pageCount + " pages");

            // Clear any existing content (no persistence)
            pdfContainer.removeAllViews();
            pageViews.clear();
            clearBitmapCache();

            // Create placeholder views for all pages
            for (int i = 0; i < pageCount; i++) {
                ZoomableImageView imageView = new ZoomableImageView(this);
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setPadding(16, 8, 16, 8);

                // Set layout parameters to allow proper zoom behavior
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                imageView.setLayoutParams(params);

                // Set a minimum height to prevent layout jumping
                int minHeight = getResources().getDisplayMetrics().heightPixels / 2;
                imageView.setMinimumHeight(minHeight);

                pdfContainer.addView(imageView);
                pageViews.put(i, imageView);
            }

            // Set up scroll listener for lazy loading
            setupScrollListener();

            // Load first few pages immediately
            loadPagesInRange(0, Math.min(2, pageCount - 1));
        } catch (OutOfMemoryError e) {
            String errorMsg = "Out of memory while rendering PDF pages. File is too large.";
            Log.e(TAG, errorMsg, e);
            showError(errorMsg);
            clearBitmapCache();
            System.gc();
        } catch (Exception e) {
            String errorMsg = "Error rendering PDF pages: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            showError(errorMsg);
        }
    }
    
    private void showError(String errorMessage) {
        selectFileButton.setVisibility(View.GONE);
        horizontalScrollView.setVisibility(View.GONE);
        errorText.setVisibility(View.VISIBLE);
        errorText.setText("Error: " + errorMessage);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Do not save any state - security requirement
        // This prevents any user input or file selection from being persisted
        super.onSaveInstanceState(new Bundle());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // Do not restore any state - security requirement
        super.onRestoreInstanceState(new Bundle());
    }

    private void setupScrollListener() {
        scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                loadVisiblePages();
            }
        });
    }
    
    private void loadVisiblePages() {
        int scrollY = scrollView.getScrollY();
        int viewHeight = scrollView.getHeight();
        
        // Calculate which pages are visible
        int startPage = -1;
        int endPage = -1;
        
        for (int i = 0; i < pageViews.size(); i++) {
            ZoomableImageView pageView = pageViews.get(i);
            if (pageView != null) {
                int pageTop = pageView.getTop();
                int pageBottom = pageView.getBottom();
                
                // Check if page is visible or close to visible
                if (pageBottom > scrollY - viewHeight && pageTop < scrollY + viewHeight * 2) {
                    if (startPage == -1) startPage = i;
                    endPage = i;
                }
            }
        }
        
        if (startPage != -1 && endPage != -1) {
            loadPagesInRange(startPage, endPage);
        }
    }
    
    private void loadPagesInRange(int startPage, int endPage) {
        for (int i = startPage; i <= endPage; i++) {
            loadPageIfNeeded(i);
        }
        
        // Clean up pages that are far from visible area
        cleanupDistantPages(startPage, endPage);
    }
    
    private void loadPageIfNeeded(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= pageViews.size()) return;
        
        ZoomableImageView imageView = pageViews.get(pageIndex);
        if (imageView == null || imageView.getDrawable() != null) return;
        
        Bitmap bitmap = bitmapCache.get(pageIndex);
        if (bitmap == null) {
            bitmap = renderPage(pageIndex);
            if (bitmap != null) {
                bitmapCache.put(pageIndex, bitmap);
                // Limit cache size
                if (bitmapCache.size() > MAX_CACHED_PAGES) {
                    cleanupOldestCacheEntry();
                }
            }
        }
        
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        }
    }
    
    private Bitmap renderPage(int pageIndex) {
        try {
            PdfRenderer.Page page = pdfRenderer.openPage(pageIndex);

            int width = page.getWidth();
            int height = page.getHeight();

            // Use higher resolution for better zoom quality
            int targetWidth = Math.max(getResources().getDisplayMetrics().widthPixels - 32, width);
            float scale = (float) targetWidth / width;
            int targetHeight = (int) (height * scale);

            Log.d(TAG, "Rendering page " + pageIndex + " with dimensions: " + targetWidth + "x" + targetHeight +
                " (scale: " + scale + ")");

            Bitmap bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            page.close();
            return bitmap;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory rendering page " + pageIndex, e);
            // Don't show error to user, just skip this page
            clearBitmapCache();
            System.gc();
            return null;
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException rendering page " + pageIndex + ": " + e.getMessage(), e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error rendering page " + pageIndex + ": " + e.getMessage(), e);
            return null;
        }
    }
    
    private void cleanupDistantPages(int startPage, int endPage) {
        // Remove bitmaps for pages that are far from visible area
        for (int i = 0; i < pageViews.size(); i++) {
            if (i < startPage - 2 || i > endPage + 2) {
                ZoomableImageView imageView = pageViews.get(i);
                if (imageView != null && imageView.getDrawable() != null) {
                    imageView.setImageBitmap(null);
                }
                bitmapCache.remove(i);
            }
        }
    }
    
    private void cleanupOldestCacheEntry() {
        // Simple cleanup: remove first entry (could be improved with LRU)
        if (!bitmapCache.isEmpty()) {
            Integer firstKey = bitmapCache.keySet().iterator().next();
            Bitmap bitmap = bitmapCache.remove(firstKey);
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }
    
    private void clearBitmapCache() {
        for (Bitmap bitmap : bitmapCache.values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        bitmapCache.clear();
    }

    private void logMemoryInfo(String context) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        Log.i(TAG, String.format("[%s] Memory - Used: %d MB, Total: %d MB, Max: %d MB",
            context, usedMemory, totalMemory, maxMemory));
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity destroying, cleaning up resources");
        clearBitmapCache();
        try {
            if (pdfRenderer != null) {
                pdfRenderer.close();
                Log.d(TAG, "PDF renderer closed");
            }
            if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
                Log.d(TAG, "File descriptor closed");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing resources: " + e.getMessage(), e);
        }
    }
}