package org.ameelio.pdfviewer;

import android.content.Context;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PinchZoomItemTouchListenerTest {

    private RecyclerView recyclerView;
    private ZoomCoordinator coordinator;
    private PinchZoomItemTouchListener listener;
    private long downTime;
    private long eventTime;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.layout(0, 0, 600, 1000);
        coordinator = new ZoomCoordinator();
        listener = new PinchZoomItemTouchListener(recyclerView, coordinator);
        downTime = SystemClock.uptimeMillis();
        eventTime = downTime;
    }

    @Test
    public void singleFingerGesturesAreNotClaimed() {
        MotionEvent down = obtainEvent(MotionEvent.ACTION_DOWN, new float[][]{{200f, 600f}});
        assertFalse("Single-finger events should stay with RecyclerView scrolling",
                listener.onInterceptTouchEvent(recyclerView, down));

        MotionEvent move = obtainEvent(MotionEvent.ACTION_MOVE, new float[][]{{200f, 400f}});
        assertFalse("Single-finger moves should stay with RecyclerView scrolling",
                listener.onInterceptTouchEvent(recyclerView, move));

        down.recycle();
        move.recycle();
    }

    @Test
    public void multiTouchGesturesAreClaimed() {
        MotionEvent down = obtainEvent(MotionEvent.ACTION_DOWN, new float[][]{{200f, 600f}});
        listener.onInterceptTouchEvent(recyclerView, down);

        MotionEvent pointerDown = obtainEvent(
                MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                new float[][]{{200f, 600f}, {400f, 800f}});
        assertTrue("Multi-touch events must be claimed so pinch zoom wins over scrolling",
                listener.onInterceptTouchEvent(recyclerView, pointerDown));

        down.recycle();
        pointerDown.recycle();
    }

    @Test
    public void pinchThatStartsAfterScrollStillZooms() {
        // Finger 1 lands and drags as if scrolling (RecyclerView would have intercepted
        // by now and cancelled the child views).
        dispatchIntercept(MotionEvent.ACTION_DOWN, new float[][]{{200f, 600f}});
        dispatchIntercept(MotionEvent.ACTION_MOVE, new float[][]{{200f, 500f}});
        dispatchIntercept(MotionEvent.ACTION_MOVE, new float[][]{{200f, 400f}});

        // Finger 2 lands mid-gesture; the listener claims the gesture...
        MotionEvent pointerDown = obtainEvent(
                MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                new float[][]{{200f, 400f}, {400f, 600f}});
        assertTrue(listener.onInterceptTouchEvent(recyclerView, pointerDown));
        pointerDown.recycle();

        // ...and the remaining events arrive via onTouchEvent while the fingers spread.
        float[][][] frames = {
                {{175f, 375f}, {425f, 625f}},
                {{150f, 350f}, {450f, 650f}},
                {{125f, 325f}, {475f, 675f}},
                {{100f, 300f}, {500f, 700f}},
        };
        for (float[][] positions : frames) {
            dispatchTouch(MotionEvent.ACTION_MOVE, positions);
        }
        dispatchTouch(MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                frames[frames.length - 1]);
        dispatchTouch(MotionEvent.ACTION_UP, new float[][]{{100f, 300f}});

        assertTrue("Pinch that begins after a scroll must still zoom the document",
                coordinator.getCurrentScale() > 1f);
    }

    @Test
    public void pinchZoomIsClampedToMaxScale() {
        coordinator.propagateScale(null, ZoomCoordinator.MAX_SCALE, Float.NaN, Float.NaN);

        dispatchIntercept(MotionEvent.ACTION_DOWN, new float[][]{{250f, 450f}});
        MotionEvent pointerDown = obtainEvent(
                MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                new float[][]{{250f, 450f}, {350f, 550f}});
        listener.onInterceptTouchEvent(recyclerView, pointerDown);
        pointerDown.recycle();

        dispatchTouch(MotionEvent.ACTION_MOVE, new float[][]{{100f, 300f}, {500f, 700f}});
        dispatchTouch(MotionEvent.ACTION_UP, new float[][]{{100f, 300f}});

        assertEquals("Zoom must not exceed the maximum scale",
                ZoomCoordinator.MAX_SCALE, coordinator.getCurrentScale(), 0.0001f);
    }

    private void dispatchIntercept(int action, float[][] positions) {
        MotionEvent event = obtainEvent(action, positions);
        listener.onInterceptTouchEvent(recyclerView, event);
        event.recycle();
    }

    private void dispatchTouch(int action, float[][] positions) {
        MotionEvent event = obtainEvent(action, positions);
        listener.onTouchEvent(recyclerView, event);
        event.recycle();
    }

    private MotionEvent obtainEvent(int action, float[][] positions) {
        eventTime += 20;
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
}
