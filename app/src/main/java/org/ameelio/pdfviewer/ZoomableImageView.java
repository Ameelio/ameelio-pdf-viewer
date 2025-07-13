package org.ameelio.pdfviewer;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * Custom ImageView that supports pinch-to-zoom and pan gestures
 * Designed for PDF page display with security considerations
 */
public class ZoomableImageView extends AppCompatImageView implements View.OnTouchListener, ScaleGestureDetector.OnScaleGestureListener {
    
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 5.0f;
    
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    
    private ScaleGestureDetector scaleGestureDetector;
    private float currentScale = 1.0f;
    private PointF start = new PointF();
    private PointF mid = new PointF();
    
    private enum Mode {
        NONE, DRAG, ZOOM
    }
    
    private Mode mode = Mode.NONE;
    
    public ZoomableImageView(Context context) {
        super(context);
        init();
    }
    
    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public ZoomableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        super.setClickable(true);
        scaleGestureDetector = new ScaleGestureDetector(getContext(), this);
        matrix.setTranslate(1f, 1f);
        setImageMatrix(matrix);
        setScaleType(ScaleType.MATRIX);
        setOnTouchListener(this);
    }
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        
        PointF curr = new PointF(event.getX(), event.getY());
        
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                start.set(curr);
                mode = Mode.DRAG;
                break;
                
            case MotionEvent.ACTION_POINTER_DOWN:
                // Second finger down - don't interfere with scale gesture
                mode = Mode.NONE;
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (mode == Mode.DRAG) {
                    matrix.set(savedMatrix);
                    float dx = curr.x - start.x;
                    float dy = curr.y - start.y;
                    matrix.postTranslate(dx, dy);
                    
                    // Apply bounds checking
                    checkBounds();
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = Mode.NONE;
                break;
        }
        
        setImageMatrix(matrix);
        return true;
    }
    
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scaleFactor = detector.getScaleFactor();
        float newScale = currentScale * scaleFactor;
        
        // Limit scale between MIN_SCALE and MAX_SCALE
        if (newScale < MIN_SCALE) {
            scaleFactor = MIN_SCALE / currentScale;
        } else if (newScale > MAX_SCALE) {
            scaleFactor = MAX_SCALE / currentScale;
        }
        
        currentScale *= scaleFactor;
        
        matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
        checkBounds();
        
        return true;
    }
    
    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mode = Mode.ZOOM;
        return true;
    }
    
    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        // Scale gesture ended
    }
    
    private void checkBounds() {
        float[] values = new float[9];
        matrix.getValues(values);
        
        float translateX = values[Matrix.MTRANS_X];
        float translateY = values[Matrix.MTRANS_Y];
        float scaleX = values[Matrix.MSCALE_X];
        float scaleY = values[Matrix.MSCALE_Y];
        
        if (getDrawable() != null) {
            float imageWidth = getDrawable().getIntrinsicWidth() * scaleX;
            float imageHeight = getDrawable().getIntrinsicHeight() * scaleY;
            
            float viewWidth = getWidth();
            float viewHeight = getHeight();
            
            float deltaX = 0;
            float deltaY = 0;
            
            // Check horizontal bounds
            if (imageWidth <= viewWidth) {
                deltaX = (viewWidth - imageWidth) / 2 - translateX;
            } else {
                if (translateX > 0) {
                    deltaX = -translateX;
                } else if (translateX < viewWidth - imageWidth) {
                    deltaX = viewWidth - imageWidth - translateX;
                }
            }
            
            // Check vertical bounds
            if (imageHeight <= viewHeight) {
                deltaY = (viewHeight - imageHeight) / 2 - translateY;
            } else {
                if (translateY > 0) {
                    deltaY = -translateY;
                } else if (translateY < viewHeight - imageHeight) {
                    deltaY = viewHeight - imageHeight - translateY;
                }
            }
            
            matrix.postTranslate(deltaX, deltaY);
        }
    }
    
    /**
     * Reset zoom and pan to default state
     */
    public void resetZoom() {
        currentScale = 1.0f;
        matrix.reset();
        matrix.setTranslate(1f, 1f);
        setImageMatrix(matrix);
    }
    
    /**
     * Get current zoom scale
     */
    public float getCurrentScale() {
        return currentScale;
    }
}