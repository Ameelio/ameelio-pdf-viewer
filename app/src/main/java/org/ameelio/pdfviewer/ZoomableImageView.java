package org.ameelio.pdfviewer;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewParent;
import androidx.appcompat.widget.AppCompatImageView;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight ImageView that reports pinch gestures to a shared ZoomCoordinator.
 * The actual document scaling is applied elsewhere so every page moves in unison.
 */
public class ZoomableImageView extends AppCompatImageView implements
        View.OnTouchListener,
        ScaleGestureDetector.OnScaleGestureListener,
        ZoomCoordinator.ZoomListener {

    private static final String TAG = "ZoomableImageView";
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 5.0f;
    private static final AtomicInteger NEXT_DEBUG_ID = new AtomicInteger(1);

    private final int debugId = NEXT_DEBUG_ID.getAndIncrement();
    private final int[] tmpScreenLocation = new int[2];
    private final float[] tmpFocus = new float[2];

    private ScaleGestureDetector scaleDetector;
    private ZoomCoordinator zoomCoordinator;
    private float currentScale = 1f;
    private boolean parentInterceptDisabled = false;
    private float lastTouchRawX;
    private float lastTouchRawY;
    private boolean multiTouchActive = false;

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
        setClickable(true);
        setScaleType(ScaleType.FIT_CENTER);
        scaleDetector = new ScaleGestureDetector(getContext(), this);
        setOnTouchListener(this);
    }

    public void setZoomCoordinator(ZoomCoordinator coordinator) {
        if (zoomCoordinator == coordinator) {
            return;
        }
        if (zoomCoordinator != null) {
            zoomCoordinator.unregister(this);
        }
        zoomCoordinator = coordinator;
        if (zoomCoordinator != null && isAttachedToWindow()) {
            zoomCoordinator.register(this);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (zoomCoordinator != null) {
            zoomCoordinator.register(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (zoomCoordinator != null) {
            zoomCoordinator.unregister(this);
        }
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                multiTouchActive = false;
                requestParentDisallowIntercept(false);
                lastTouchRawX = event.getRawX();
                lastTouchRawY = event.getRawY();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                multiTouchActive = event.getPointerCount() > 1;
                requestParentDisallowIntercept(true);
                lastTouchRawX = event.getRawX();
                lastTouchRawY = event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress()
                        && event.getPointerCount() == 1
                        && zoomCoordinator != null
                        && currentScale > 1f) {
                    float dx = event.getRawX() - lastTouchRawX;
                    float dy = event.getRawY() - lastTouchRawY;
                    lastTouchRawX = event.getRawX();
                    lastTouchRawY = event.getRawY();
                    if (Math.abs(dx) > 0 || Math.abs(dy) > 0) {
                        zoomCoordinator.propagatePan(dx, dy);
                    }
                } else {
                    lastTouchRawX = event.getRawX();
                    lastTouchRawY = event.getRawY();
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                int remainingPointers = event.getPointerCount() - 1;
                multiTouchActive = remainingPointers > 1;
                if (!multiTouchActive && !scaleDetector.isInProgress()) {
                    requestParentDisallowIntercept(false);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                multiTouchActive = false;
                requestParentDisallowIntercept(false);
                break;
        }

        return scaleDetector.isInProgress() || multiTouchActive;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scaleFactor = detector.getScaleFactor();
        float newScale = currentScale * scaleFactor;
        if (newScale < MIN_SCALE) {
            newScale = MIN_SCALE;
        } else if (newScale > MAX_SCALE) {
            newScale = MAX_SCALE;
        }

        if (Math.abs(newScale - currentScale) < 0.0001f) {
            return true;
        }

        currentScale = newScale;
        debug(String.format(Locale.US,
                "onScale -> scale=%.3f span=%.1f focus=(%.1f,%.1f)",
                currentScale, detector.getCurrentSpan(), detector.getFocusX(), detector.getFocusY()));

        if (zoomCoordinator != null) {
            computeFocusOnScreen(detector, tmpFocus);
            zoomCoordinator.propagateScale(this, currentScale, tmpFocus[0], tmpFocus[1]);
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        requestParentDisallowIntercept(true);
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        if (!multiTouchActive) {
            requestParentDisallowIntercept(false);
        }
    }

    @Override
    public void onGlobalScaleChanged(ZoomableImageView source, float scale, float focusX, float focusY) {
        if (source == this) {
            return; // already applied by originator
        }
        currentScale = scale;
    }

    private void computeFocusOnScreen(ScaleGestureDetector detector, float[] out) {
        getLocationOnScreen(tmpScreenLocation);
        out[0] = tmpScreenLocation[0] + detector.getFocusX();
        out[1] = tmpScreenLocation[1] + detector.getFocusY();
    }

    private void requestParentDisallowIntercept(boolean disallow) {
        if (parentInterceptDisabled == disallow) {
            return;
        }
        ViewParent parent = getParent();
        while (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
            parent = parent.getParent();
        }
        parentInterceptDisabled = disallow;
    }

    private void debug(String message) {
        Log.d(TAG, "#" + debugId + " " + message);
    }
}
