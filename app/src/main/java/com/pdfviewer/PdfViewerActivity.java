package com.pdfviewer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.FileNotFoundException;
import java.io.IOException;

public class PdfViewerActivity extends AppCompatActivity {
    
    private LinearLayout pdfContainer;
    private TextView errorText;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);
        
        pdfContainer = findViewById(R.id.pdfContainer);
        errorText = findViewById(R.id.errorText);
        
        Intent intent = getIntent();
        Uri pdfUri = intent.getData();
        
        if (pdfUri != null) {
            openPdf(pdfUri);
        } else {
            showError();
        }
    }
    
    private void openPdf(Uri uri) {
        try {
            parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            if (parcelFileDescriptor != null) {
                pdfRenderer = new PdfRenderer(parcelFileDescriptor);
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
    
    private void renderPdfPages() {
        int pageCount = pdfRenderer.getPageCount();
        
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
        pdfContainer.setVisibility(View.GONE);
        errorText.setVisibility(View.VISIBLE);
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