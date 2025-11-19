package org.ameelio.pdfviewer;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Coordinates zoom scale across all PDF page views so pinch-to-zoom applies to the entire document.
 */
class ZoomCoordinator {

    interface ZoomListener {
        void onGlobalScaleChanged(ZoomableImageView source, float scale, float focusX, float focusY);
    }

    interface PanListener {
        void onGlobalPanChanged(float dx, float dy);
    }

    private final Set<ZoomListener> zoomListeners =
            Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<PanListener> panListeners =
            Collections.newSetFromMap(new WeakHashMap<>());
    private float currentScale = 1f;

    synchronized void register(ZoomListener listener) {
        zoomListeners.add(listener);
        listener.onGlobalScaleChanged(null, currentScale, Float.NaN, Float.NaN);
    }

    synchronized void unregister(ZoomListener listener) {
        zoomListeners.remove(listener);
    }

    synchronized void registerPanListener(PanListener listener) {
        panListeners.add(listener);
    }

    synchronized void unregisterPanListener(PanListener listener) {
        panListeners.remove(listener);
    }

    synchronized void propagateScale(ZoomableImageView source, float scale, float focusX, float focusY) {
        currentScale = scale;
        for (ZoomListener listener : zoomListeners) {
            if (listener != null && listener != source) {
                listener.onGlobalScaleChanged(source, currentScale, focusX, focusY);
            }
        }
    }

    synchronized void propagatePan(float dx, float dy) {
        if (dx == 0 && dy == 0) {
            return;
        }
        for (PanListener listener : panListeners) {
            if (listener != null) {
                listener.onGlobalPanChanged(dx, dy);
            }
        }
    }

    synchronized float getCurrentScale() {
        return currentScale;
    }
}
