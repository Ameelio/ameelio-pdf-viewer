package com.pdfviewer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;
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

public class PdfViewerActivity extends AppCompatActivity {
    
    private Button selectFileButton;
    private ScrollView scrollView;
    private LinearLayout pdfContainer;
    private TextView errorText;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);
        
        selectFileButton = findViewById(R.id.selectFileButton);
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
        try {
            parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            if (parcelFileDescriptor != null) {
                pdfRenderer = new PdfRenderer(parcelFileDescriptor);
                hideFileSelector();
                renderPdfPages();
            } else {
                showError();
            }
        } catch (FileNotFoundException e) {
            showError();
        } catch (IOException e) {
            showError();
        }
    }
    
    private void hideFileSelector() {
        selectFileButton.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
    }
    
    private void renderPdfPages() {
        int pageCount = pdfRenderer.getPageCount();
        
        // Clear any existing content (no persistence)
        pdfContainer.removeAllViews();
        
        for (int i = 0; i < pageCount; i++) {
            PdfRenderer.Page page = pdfRenderer.openPage(i);
            
            int width = page.getWidth();
            int height = page.getHeight();
            
            int targetWidth = getResources().getDisplayMetrics().widthPixels - 32;
            float scale = (float) targetWidth / width;
            int targetHeight = (int) (height * scale);
            
            Bitmap bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setPadding(16, 8, 16, 8);
            
            pdfContainer.addView(imageView);
            page.close();
        }
    }
    
    private void showError() {
        selectFileButton.setVisibility(View.GONE);
        scrollView.setVisibility(View.GONE);
        errorText.setVisibility(View.VISIBLE);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (pdfRenderer != null) {
                pdfRenderer.close();
            }
            if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}