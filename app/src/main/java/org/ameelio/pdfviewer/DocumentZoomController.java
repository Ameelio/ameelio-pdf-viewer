package org.ameelio.pdfviewer;

import android.view.View;

/**
 * Applies document-level scaling by transforming the RecyclerView (or any target view)
 * so every PDF page scrolls and zooms as a single continuous surface.
 */
class DocumentZoomController implements ZoomCoordinator.ZoomListener, ZoomCoordinator.PanListener {

    private final View target;
    private final ZoomCoordinator coordinator;
    private float currentScale = 1f;
    private float translationX = 0f;
    private float translationY = 0f;

    DocumentZoomController(View target, ZoomCoordinator coordinator) {
        this.target = target;
        this.coordinator = coordinator;
        coordinator.register(this);
        coordinator.registerPanListener(this);
    }

    @Override
    public void onGlobalScaleChanged(ZoomableImageView source, float scale, float focusX, float focusY) {
        currentScale = scale;
        if (currentScale <= 1f) {
            translationX = 0f;
            translationY = 0f;
        }
        applyTransforms();
    }

    @Override
    public void onGlobalPanChanged(float dx, float dy) {
        if (currentScale <= 1f) {
            translationX = 0f;
            translationY = 0f;
            applyTransforms();
            return;
        }
        float desiredX = translationX + dx;
        float desiredY = translationY + dy;

        float extraWidth = (target.getWidth() * (currentScale - 1f)) / 2f;
        float extraHeight = (target.getHeight() * (currentScale - 1f)) / 2f;

        float clampedX = clamp(desiredX, extraWidth);
        float clampedY = clamp(desiredY, extraHeight);

        float overflowY = desiredY - clampedY;

        translationX = clampedX;
        translationY = clampedY;
        applyTransforms();

        if (Math.abs(overflowY) > 0.5f) {
            target.scrollBy(0, (int) -overflowY);
        }
    }

    void detach() {
        coordinator.unregister(this);
        coordinator.unregisterPanListener(this);
    }

    private void applyTransforms() {
        if (target.getWidth() == 0 || target.getHeight() == 0) {
            target.post(this::applyTransforms);
            return;
        }
        clampTranslations();
        target.setPivotX(target.getWidth() / 2f);
        target.setPivotY(target.getHeight() / 2f);
        target.setScaleX(currentScale);
        target.setScaleY(currentScale);
        target.setTranslationX(translationX);
        target.setTranslationY(translationY);
    }

    private void clampTranslations() {
        if (currentScale <= 1f) {
            translationX = 0f;
            translationY = 0f;
            return;
        }
        float extraWidth = (target.getWidth() * (currentScale - 1f)) / 2f;
        float extraHeight = (target.getHeight() * (currentScale - 1f)) / 2f;
        translationX = clamp(translationX, extraWidth);
        translationY = clamp(translationY, extraHeight);
    }

    private float clamp(float value, float limit) {
        if (limit <= 0f) {
            return 0f;
        }
        if (value > limit) return limit;
        if (value < -limit) return -limit;
        return value;
    }
}
