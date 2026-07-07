package org.ameelio.pdfviewer;

import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Detects pinch gestures at the RecyclerView level and reports them to the shared
 * ZoomCoordinator.
 *
 * Pinch detection cannot live only in the per-page views: once a single finger drags
 * past the touch slop, RecyclerView intercepts the gesture and the child views receive
 * ACTION_CANCEL, so a second finger landing mid-scroll is never seen by any page view.
 * An OnItemTouchListener sees every event before RecyclerView consumes it, so pinches
 * work no matter how the gesture started.
 */
class PinchZoomItemTouchListener implements RecyclerView.OnItemTouchListener,
        ScaleGestureDetector.OnScaleGestureListener,
        ZoomCoordinator.ZoomListener {

    private final RecyclerView recyclerView;
    private final ZoomCoordinator zoomCoordinator;
    private final ScaleGestureDetector scaleDetector;
    private final int[] tmpScreenLocation = new int[2];
    private float currentScale = 1f;

    PinchZoomItemTouchListener(RecyclerView recyclerView, ZoomCoordinator zoomCoordinator) {
        this.recyclerView = recyclerView;
        this.zoomCoordinator = zoomCoordinator;
        this.scaleDetector = new ScaleGestureDetector(recyclerView.getContext(), this);
        // Quick scale (double-tap drag) is single-finger, so it would fight RecyclerView
        // scrolling; only real two-finger pinches are claimed below.
        this.scaleDetector.setQuickScaleEnabled(false);
        zoomCoordinator.register(this);
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        scaleDetector.onTouchEvent(e);
        // Claim multi-touch gestures so RecyclerView stops scrolling and routes the
        // rest of the gesture to onTouchEvent below, even if a scroll was in progress.
        return e.getPointerCount() > 1;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        scaleDetector.onTouchEvent(e);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        // Zoom must keep working even when a child disallows interception.
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float newScale = currentScale * detector.getScaleFactor();
        if (newScale < ZoomCoordinator.MIN_SCALE) {
            newScale = ZoomCoordinator.MIN_SCALE;
        } else if (newScale > ZoomCoordinator.MAX_SCALE) {
            newScale = ZoomCoordinator.MAX_SCALE;
        }

        if (Math.abs(newScale - currentScale) < 0.0001f) {
            return true;
        }

        currentScale = newScale;
        recyclerView.getLocationOnScreen(tmpScreenLocation);
        zoomCoordinator.propagateScale(null, currentScale,
                tmpScreenLocation[0] + detector.getFocusX(),
                tmpScreenLocation[1] + detector.getFocusY());
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        // Nothing to release; RecyclerView resumes normal handling after the gesture.
    }

    @Override
    public void onGlobalScaleChanged(ZoomableImageView source, float scale, float focusX, float focusY) {
        currentScale = scale;
    }
}
