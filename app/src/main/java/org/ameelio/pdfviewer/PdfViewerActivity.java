package org.ameelio.pdfviewer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PdfViewerActivity extends AppCompatActivity {

    private static final String TAG = "PdfViewerActivity";

    private Button selectFileButton;
    private ImageButton resetZoomButton;
    private ImageButton zoomInButton;
    private ImageButton zoomOutButton;
    private RecyclerView recyclerView;
    private TextView errorText;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private PdfPageAdapter adapter;
    private final ZoomCoordinator zoomCoordinator = new ZoomCoordinator();
    private DocumentZoomController documentZoomController;

    // Performance optimization variables
    private Map<Integer, Bitmap> bitmapCache = new HashMap<>();
    private static final int MAX_CACHED_PAGES = 3;
    private static final int MAX_RENDER_DIMENSION = 2048; // Prevent huge bitmaps
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 5.0f;
    private static final float ZOOM_STEP = 0.25f;
    private View zoomControlsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        setupActionBar();

        selectFileButton = findViewById(R.id.selectFileButton);
        resetZoomButton = findViewById(R.id.resetZoomButton);
        zoomInButton = findViewById(R.id.zoomInButton);
        zoomOutButton = findViewById(R.id.zoomOutButton);
        zoomControlsContainer = findViewById(R.id.zoomControlsContainer);
        recyclerView = findViewById(R.id.pdfRecyclerView);
        errorText = findViewById(R.id.errorText);
        documentZoomController = new DocumentZoomController(recyclerView, zoomCoordinator);
        // Keep multi-touch streams unified so pinch gestures spanning multiple pages are detected
        recyclerView.setMotionEventSplittingEnabled(false);

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(false);

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

        resetZoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetZoomToDefault();
            }
        });

        if (zoomInButton != null) {
            zoomInButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyZoomDelta(ZOOM_STEP);
                }
            });
        }

        if (zoomOutButton != null) {
            zoomOutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyZoomDelta(-ZOOM_STEP);
                }
            });
        }

        handleIncomingIntent(getIntent());
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return;
        }

        View customView = LayoutInflater.from(this).inflate(R.layout.view_action_bar_header, null, false);
        TextView titleView = customView.findViewById(R.id.actionBarTitle);
        ImageView logoView = customView.findViewById(R.id.actionBarLogo);
        titleView.setText(R.string.app_name);
        logoView.setImageResource(R.mipmap.ic_launcher);

        ActionBar.LayoutParams params = new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.START | Gravity.CENTER_VERTICAL;

        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setCustomView(customView, params);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        // No persistence - don't save recent files or history
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        filePickerLauncher.launch(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        Uri pdfUri = intent.getData();
        if (pdfUri != null) {
            openPdf(pdfUri);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            setIntent(intent);
            handleIncomingIntent(intent);
        }
    }

    private void openPdf(Uri uri) {
        Log.i(TAG, "Attempting to open PDF: " + uri);
        logMemoryInfo("Before opening PDF");

        ParcelFileDescriptor newParcelFileDescriptor = null;
        PdfRenderer newPdfRenderer = null;
        long openedFileSizeBytes = -1;

        try {
            newParcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            if (newParcelFileDescriptor == null) {
                String errorMsg = "Failed to open file descriptor for PDF";
                Log.e(TAG, errorMsg);
                showError(errorMsg);
                return;
            }

            openedFileSizeBytes = newParcelFileDescriptor.getStatSize();
            Log.i(TAG, "PDF file size: " + openedFileSizeBytes + " bytes (" +
                    (openedFileSizeBytes / 1024 / 1024) + " MB)");

            newPdfRenderer = new PdfRenderer(newParcelFileDescriptor);
            int pageCount = newPdfRenderer.getPageCount();
            Log.i(TAG, "PDF opened successfully. Pages: " + pageCount);

            replaceRenderer(newPdfRenderer, newParcelFileDescriptor);
            newPdfRenderer = null;
            newParcelFileDescriptor = null;

            logMemoryInfo("After opening PDF");
            hideFileSelector();
            setupRecyclerView();
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
                    (openedFileSizeBytes > 0 ? (openedFileSizeBytes / 1024 / 1024) + " MB" : "unknown");
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
        } finally {
            closeQuietly(newPdfRenderer);
            closeQuietly(newParcelFileDescriptor);
        }
    }

    private void hideFileSelector() {
        selectFileButton.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        resetZoomButton.setVisibility(View.VISIBLE);
        if (zoomControlsContainer != null) {
            zoomControlsContainer.setVisibility(View.VISIBLE);
        }
    }

    private void setupRecyclerView() {
        try {
            int pageCount = pdfRenderer.getPageCount();
            Log.i(TAG, "Setting up RecyclerView for " + pageCount + " pages");

            adapter = new PdfPageAdapter(zoomCoordinator);
            recyclerView.setAdapter(adapter);
            recyclerView.scrollToPosition(0);
            zoomCoordinator.propagateScale(null, 1f, Float.NaN, Float.NaN);

            recyclerView.clearOnScrollListeners();
            // Add scroll listener for cache management
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    cleanupDistantPages();
                }
            });

            Log.i(TAG, "RecyclerView setup complete");
        } catch (Exception e) {
            String errorMsg = "Error setting up PDF viewer: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            showError(errorMsg);
        }
    }

    private void showError(String errorMessage) {
        selectFileButton.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        errorText.setVisibility(View.VISIBLE);
        errorText.setText("Error: " + errorMessage);
        resetZoomButton.setVisibility(View.GONE);
        if (zoomControlsContainer != null) {
            zoomControlsContainer.setVisibility(View.GONE);
        }
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

    /**
     * RecyclerView Adapter for PDF pages
     */
    private class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.PageViewHolder> {

        private final ZoomCoordinator zoomCoordinator;

        PdfPageAdapter(ZoomCoordinator zoomCoordinator) {
            this.zoomCoordinator = zoomCoordinator;
        }

        @Override
        public int getItemCount() {
            return pdfRenderer != null ? pdfRenderer.getPageCount() : 0;
        }

        @Override
        public PageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Log.d(TAG, "Creating new ViewHolder");

            ZoomableImageView imageView = new ZoomableImageView(parent.getContext());
            imageView.setZoomCoordinator(zoomCoordinator);

            // Set layout params for proper sizing
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(16, 8, 16, 8);
            imageView.setLayoutParams(params);

            return new PageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(PageViewHolder holder, int position) {
            Log.d(TAG, "Binding page " + position);
            logMemoryInfo("Before binding page " + position);

            // Check cache first
            Bitmap bitmap = bitmapCache.get(position);

            if (bitmap == null || bitmap.isRecycled()) {
                // Render the page
                bitmap = renderPage(position);

                if (bitmap != null) {
                    bitmapCache.put(position, bitmap);

                    // Limit cache size
                    if (bitmapCache.size() > MAX_CACHED_PAGES) {
                        cleanupOldestCacheEntry();
                    }
                }
            }

            if (bitmap != null) {
                holder.imageView.setImageBitmap(bitmap);
                Log.d(TAG, "Page " + position + " bound successfully. Cache size: " + bitmapCache.size());
            } else {
                holder.imageView.setImageDrawable(null);
                Log.w(TAG, "Failed to render page " + position);
            }

            logMemoryInfo("After binding page " + position);
        }

        @Override
        public void onViewRecycled(PageViewHolder holder) {
            super.onViewRecycled(holder);
            // Clear the image to free memory when view is recycled
            holder.imageView.setImageDrawable(null);
            Log.d(TAG, "ViewHolder recycled");
        }

        class PageViewHolder extends RecyclerView.ViewHolder {
            ZoomableImageView imageView;

            PageViewHolder(ZoomableImageView itemView) {
                super(itemView);
                this.imageView = itemView;
            }
        }
    }

    private Bitmap renderPage(int pageIndex) {
        if (pdfRenderer == null) {
            Log.e(TAG, "PDF renderer is null");
            return null;
        }

        try {
            PdfRenderer.Page page = pdfRenderer.openPage(pageIndex);

            int width = page.getWidth();
            int height = page.getHeight();

            Log.d(TAG, String.format("Page %d native size: %dx%d (%.1f MP)",
                    pageIndex, width, height, (width * height) / 1000000.0));

            // Calculate target dimensions with aggressive downsampling for large pages
            int screenWidth = getResources().getDisplayMetrics().widthPixels - 32;

            // Start with screen width
            int targetWidth = screenWidth;
            float scale = (float) targetWidth / width;
            int targetHeight = (int) (height * scale);

            // Apply maximum dimension limit to prevent OOM
            if (targetWidth > MAX_RENDER_DIMENSION || targetHeight > MAX_RENDER_DIMENSION) {
                float maxScale = Math.min(
                        (float) MAX_RENDER_DIMENSION / targetWidth,
                        (float) MAX_RENDER_DIMENSION / targetHeight
                );
                targetWidth = (int) (targetWidth * maxScale);
                targetHeight = (int) (targetHeight * maxScale);
                Log.w(TAG, "Page " + pageIndex + " downsampled to fit max dimension limit");
            }

            long bitmapBytes = (long) targetWidth * targetHeight * 4;
            Log.d(TAG, String.format("Page %d rendering at %dx%d = %.1f MB (scale: %.2f)",
                    pageIndex, targetWidth, targetHeight, bitmapBytes / 1024.0 / 1024.0,
                    (float) targetWidth / width));

            Bitmap bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            page.close();
            return bitmap;

        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory rendering page " + pageIndex, e);
            logMemoryInfo("OOM during render");
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

    private void cleanupDistantPages() {
        if (recyclerView == null || recyclerView.getLayoutManager() == null || pdfRenderer == null) return;

        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        int firstVisible = layoutManager.findFirstVisibleItemPosition();
        int lastVisible = layoutManager.findLastVisibleItemPosition();

        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) {
            return;
        }

        // Keep a buffer of 2 pages on each side
        int keepStart = Math.max(0, firstVisible - 2);
        int keepEnd = Math.min(pdfRenderer.getPageCount() - 1, lastVisible + 2);

        // Remove cached pages outside the keep range
        // BUT DON'T RECYCLE - just remove from cache
        // Android will handle bitmap memory automatically
        for (Iterator<Map.Entry<Integer, Bitmap>> iterator = bitmapCache.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<Integer, Bitmap> entry = iterator.next();
            int pageIndex = entry.getKey();
            if (pageIndex < keepStart || pageIndex > keepEnd) {
                Log.d(TAG, "Removing page " + pageIndex + " from cache (outside keep range " +
                        keepStart + "-" + keepEnd + ")");
                iterator.remove();
                // DO NOT call bitmap.recycle() here - it may still be displayed
            }
        }

        logMemoryInfo("After cleanup");
    }

    private void cleanupOldestCacheEntry() {
        if (bitmapCache.isEmpty()) return;

        // Simple cleanup: remove first entry
        // DO NOT recycle - bitmap may still be displayed
        Integer firstKey = bitmapCache.keySet().iterator().next();
        bitmapCache.remove(firstKey);
        Log.d(TAG, "Removed page " + firstKey + " from cache (cache full)");
    }

    private void clearBitmapCache() {
        Log.d(TAG, "Clearing entire bitmap cache (" + bitmapCache.size() + " entries)");
        // Just clear the cache - don't recycle bitmaps as they may still be displayed
        // Android's garbage collector will handle bitmap cleanup when they're no longer referenced
        bitmapCache.clear();
    }

    private void closeQuietly(PdfRenderer renderer) {
        if (renderer != null) {
            try {
                renderer.close();
            } catch (Exception e) {
                Log.w(TAG, "Error closing PdfRenderer: " + e.getMessage(), e);
            }
        }
    }

    private void closeQuietly(ParcelFileDescriptor descriptor) {
        if (descriptor != null) {
            try {
                descriptor.close();
            } catch (IOException e) {
                Log.w(TAG, "Error closing ParcelFileDescriptor: " + e.getMessage(), e);
            }
        }
    }

    private void replaceRenderer(PdfRenderer newRenderer, ParcelFileDescriptor newDescriptor) {
        clearBitmapCache();
        closeCurrentRenderer();
        pdfRenderer = newRenderer;
        parcelFileDescriptor = newDescriptor;
    }

    private void closeCurrentRenderer() {
        if (pdfRenderer != null) {
            try {
                pdfRenderer.close();
            } catch (Exception e) {
                Log.w(TAG, "Error closing current PdfRenderer: " + e.getMessage(), e);
            }
            pdfRenderer = null;
            Log.d(TAG, "PDF renderer closed");
        }

        if (parcelFileDescriptor != null) {
            try {
                parcelFileDescriptor.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing file descriptor: " + e.getMessage(), e);
            }
            parcelFileDescriptor = null;
            Log.d(TAG, "File descriptor closed");
        }
    }

    private void logMemoryInfo(String context) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        Log.i(TAG, String.format("[%s] Memory - Used: %d MB, Free: %d MB, Total: %d MB, Max: %d MB, Cache: %d pages",
                context, usedMemory, freeMemory, totalMemory, maxMemory, bitmapCache.size()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity destroying, cleaning up resources");

        if (documentZoomController != null) {
            documentZoomController.detach();
        }
        clearBitmapCache();

        closeCurrentRenderer();
    }

    private void resetZoomToDefault() {
        if (zoomCoordinator != null) {
            zoomCoordinator.propagateScale(null, 1f, Float.NaN, Float.NaN);
        }
    }

    private void applyZoomDelta(float delta) {
        if (zoomCoordinator == null) {
            return;
        }
        float currentScale = zoomCoordinator.getCurrentScale();
        float targetScale = currentScale + delta;
        if (targetScale < MIN_SCALE) {
            targetScale = MIN_SCALE;
        } else if (targetScale > MAX_SCALE) {
            targetScale = MAX_SCALE;
        }
        if (Math.abs(targetScale - currentScale) < 0.0001f) {
            return;
        }
        zoomCoordinator.propagateScale(null, targetScale, Float.NaN, Float.NaN);
    }
}
