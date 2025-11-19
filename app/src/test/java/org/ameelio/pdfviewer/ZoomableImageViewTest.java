package org.ameelio.pdfviewer;

import android.content.Context;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowView;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ZoomableImageViewTest {

    @Test
    public void pinchGestureImmediatelyBlocksParentIntercept() {
        Context context = RuntimeEnvironment.getApplication();
        TestViewGroup parent = new TestViewGroup(context);
        ZoomableImageView imageView = new ZoomableImageView(context);
        imageView.setZoomCoordinator(new ZoomCoordinator());

        parent.addView(imageView,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        parent.layout(0, 0, 200, 400);
        imageView.layout(0, 0, 200, 400);
        ShadowView shadowView = Shadows.shadowOf(imageView);
        shadowView.callOnAttachedToWindow();

        long downTime = SystemClock.uptimeMillis();
        MotionEvent down = obtainMotionEvent(downTime, downTime, MotionEvent.ACTION_DOWN,
                new float[][]{{50f, 50f}});
        assertFalse(imageView.onTouch(imageView, down));

        MotionEvent pointerDown = obtainMotionEvent(
                downTime,
                downTime + 10,
                MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                new float[][]{{50f, 50f}, {150f, 150f}});
        assertTrue("Multi-touch pointer down should be consumed", imageView.onTouch(imageView, pointerDown));
        assertTrue("Parent intercept should be blocked when pinch starts", parent.isInterceptDisallowed());

        MotionEvent pointerUp = obtainMotionEvent(
                downTime,
                downTime + 20,
                MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                new float[][]{{50f, 50f}, {150f, 150f}});
        imageView.onTouch(imageView, pointerUp);

        MotionEvent up = obtainMotionEvent(downTime, downTime + 30, MotionEvent.ACTION_UP,
                new float[][]{{50f, 50f}});
        imageView.onTouch(imageView, up);

        assertFalse("Parent intercept should be restored after pinch ends", parent.isInterceptDisallowed());

        down.recycle();
        pointerDown.recycle();
        pointerUp.recycle();
        up.recycle();
    }

    private MotionEvent obtainMotionEvent(long downTime, long eventTime, int action, float[][] positions) {
        int pointerCount = positions.length;
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[pointerCount];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[pointerCount];
        for (int i = 0; i < pointerCount; i++) {
            MotionEvent.PointerProperties prop = new MotionEvent.PointerProperties();
            prop.id = i;
            prop.toolType = MotionEvent.TOOL_TYPE_FINGER;
            properties[i] = prop;

            MotionEvent.PointerCoords coord = new MotionEvent.PointerCoords();
            coord.x = positions[i][0];
            coord.y = positions[i][1];
            coord.pressure = 1f;
            coord.size = 1f;
            coords[i] = coord;
        }

        return MotionEvent.obtain(
                downTime,
                eventTime,
                action,
                pointerCount,
                properties,
                coords,
                0,
                0,
                1f,
                1f,
                0,
                0,
                InputDevice.SOURCE_TOUCHSCREEN,
                0);
    }

    private static class TestViewGroup extends ViewGroup {
        private boolean disallowIntercept;

        TestViewGroup(Context context) {
            super(context);
        }

        @Override
        public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            this.disallowIntercept = disallowIntercept;
            super.requestDisallowInterceptTouchEvent(disallowIntercept);
        }

        boolean isInterceptDisallowed() {
            return disallowIntercept;
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                child.layout(0, 0, r - l, b - t);
            }
        }
    }
}
