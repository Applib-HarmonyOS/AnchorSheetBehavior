package com.hardsoftstudio.anchorsheetlayout;

import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.VelocityDetector;
import ohos.multimodalinput.event.ManipulationEvent;
import ohos.multimodalinput.event.TouchEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;

/**
 * DragHelper is a utility class for writing AnchorSheetLayout. It helps in
 * dragging the components within the component container
 */
public class DragHelper {

    /**
     * A component is not currently being dragged or animating as a result of a fling/snap.
     */
    public static final int STATE_IDLE = 0;
    /**
     * A component is currently being dragged. The position is currently changing as a result
     * of user input or simulated user input.
     */
    public static final int STATE_DRAGGING = 1;

    /** Current drag state; idle, dragging or settling. */
    private int mDragState;

    public static final int DEFAULT_MIN_VELOCITY = 100;
    public static final int DEFAULT_MAX_VELOCITY = 3000;

    private float[] mInitialMotionX;
    private float[] mInitialMotionY;
    private float[] mLastMotionX;
    private float[] mLastMotionY;
    private int mPointersDown;
    // recent pointer offset values
    private float mDeltaX = 0.0f;
    private float mDeltaY = 0.0f;

    private VelocityDetector mVelocityDetector;
    private final float mMaxVelocity;
    private final float mMinVelocity;
    private final DragHelper.Callback mCallback;
    private Component mCapturedView;
    private final ComponentContainer mParentView;

    /**
     * Factory method to create a new ViewDragHelper.
     *
     * @param forParent Parent view to monitor
     * @param cb Callback to provide information and receive events
     * @return a new ViewDragHelper instance
     */
    public static DragHelper create(@NotNull ComponentContainer forParent, @NotNull DragHelper.Callback cb) {
        return new DragHelper(forParent, cb);
    }

    /**
     * Use ViewDragHelper.create() to get a new instance.
     *
     * @param forParent Parent view to monitor
     */
    private DragHelper(@NotNull ComponentContainer forParent, @NotNull DragHelper.Callback cb) {
        this.mParentView = forParent;
        this.mCallback = cb;
        this.mMaxVelocity = DEFAULT_MAX_VELOCITY;
        this.mMinVelocity = DEFAULT_MIN_VELOCITY;
    }

    /**
     * Return the currently configured minimum velocity. Callback methods accepting a velocity will receive
     * zero as a velocity value if the real detected velocity was below this threshold.
     *
     * @return the minimum velocity that will be detected
     */
    public float getMinVelocity() {
        return this.mMinVelocity;
    }

    /**
     * Return the currently configured maximum velocity. Callback methods accepting a velocity will receive
     * this value as a velocity value if the real detected velocity was above this threshold.
     *
     * @return the minimum velocity that will be detected
     */
    public float getMaxVelocity() {
        return this.mMaxVelocity;

    }

    /**
     * Returns the Callback of the current DragHelper Object.
     *
     * @return CallBack object
     */
    public Callback getCallback() {
        return this.mCallback;
    }

    /**
     * Capture a specific child view for dragging within the parent.
     *
     * @param childView Child view to capture
     */
    public void captureChildView(@NotNull Component childView) {
        if (childView.getComponentParent() != this.mParentView) {
            throw new IllegalArgumentException("captureChildView: parameter must"
                    + " be a descendant of the ViewDragHelper's tracked parent view (" + this.mParentView + ")");
        } else {
            this.mCapturedView = childView;
            this.setDragState(STATE_DRAGGING);
        }
    }

    /**
     * The result of a call to this method is equivalent to
     * {@link #processTouchEvent(TouchEvent)} receiving an ACTION_CANCEL event.
     */
    private void cancel() {
        this.clearMotionHistory();
        if (this.mVelocityDetector != null) {
            this.mVelocityDetector.clear();
            this.mVelocityDetector = null;
        }

    }

    /**
     * Clamp the magnitude of value for absMin and absMax.
     * If the value is below the minimum, it will be clamped to zero.
     * If the value is above the maximum, it will be clamped to the maximum.
     *
     * @param value Value to clamp
     * @param absMin Absolute value of the minimum significant value to return
     * @param absMax Absolute value of the maximum value to return
     * @return The clamped value with the same sign as <code>value</code>
     */
    private float clampMag(float value, float absMin, float absMax) {
        float absValue = Math.abs(value);
        if (absValue < absMin) {
            return 0.0F;
        } else if (absValue > absMax) {
            return value > 0.0F ? absMax : -absMax;
        } else {
            return value;
        }
    }

    /**
     * Invokes Callback and sets the Drag State to Idle.
     */
    private void dispatchViewReleased(float xvel, float yvel) {
        this.mCallback.onViewReleased(this.mCapturedView, xvel, yvel, this.mDeltaX, this.mDeltaY);
        if (this.mDragState == 1) {
            this.setDragState(STATE_IDLE);
        }
    }

    // clears the motion history
    private void clearMotionHistory() {
        if (this.mInitialMotionX != null) {
            Arrays.fill(this.mInitialMotionX, 0.0F);
            Arrays.fill(this.mInitialMotionY, 0.0F);
            Arrays.fill(this.mLastMotionX, 0.0F);
            Arrays.fill(this.mLastMotionY, 0.0F);
            this.mPointersDown = 0;
        }
    }

    private void ensureMotionHistorySizeForId(int pointerId) {
        if (this.mInitialMotionX == null || this.mInitialMotionX.length <= pointerId) {
            float[] imx = new float[pointerId + 1];
            float[] imy = new float[pointerId + 1];
            float[] lmx = new float[pointerId + 1];
            float[] lmy = new float[pointerId + 1];
            if (this.mInitialMotionX != null) {
                System.arraycopy(this.mInitialMotionX, 0, imx, 0, this.mInitialMotionX.length);
                System.arraycopy(this.mInitialMotionY, 0, imy, 0, this.mInitialMotionY.length);
                System.arraycopy(this.mLastMotionX, 0, lmx, 0, this.mLastMotionX.length);
                System.arraycopy(this.mLastMotionY, 0, lmy, 0, this.mLastMotionY.length);
            }
            this.mInitialMotionX = imx;
            this.mInitialMotionY = imy;
            this.mLastMotionX = lmx;
            this.mLastMotionY = lmy;
        }
    }

    // saves the initial motion
    private void saveInitialMotion(float x, float y, int pointerId) {
        this.ensureMotionHistorySizeForId(pointerId);
        this.mInitialMotionX[pointerId] = this.mLastMotionX[pointerId] = x;
        this.mInitialMotionY[pointerId] = this.mLastMotionY[pointerId] = y;
        this.mPointersDown |= 1 << pointerId;
    }

    // saves the last motion
    private void saveLastMotion(ManipulationEvent ev) {
        int pointerCount = ev.getPointerCount();
        for (int i = 0; i < pointerCount; ++i) {
            int pointerId = ev.getPointerId(i);
            if (this.isValidPointerForActionMove(pointerId)) {
                float x = getTouchX((TouchEvent) ev, i);
                float y = getTouchY((TouchEvent) ev, i);
                this.mLastMotionX[pointerId] = x;
                this.mLastMotionY[pointerId] = y;
            }
        }
    }

    /**
     * Check if the given pointer ID represents a pointer that is currently down (to the best
     * of the DragHelper's knowledge).
     *
     * <p>The state used to report this information is populated by the method
     * {@link #processTouchEvent(TouchEvent)}. If one of these methods has not
     * been called for all relevant TouchEvents to track, the information reported
     * by this method may be stale or incorrect.</p>
     *
     * @param pointerId pointer ID to check; corresponds to IDs provided by TouchEvent
     * @return true if the pointer with the given ID is still down
     */
    private boolean isPointerDown(int pointerId) {
        return (this.mPointersDown & 1 << pointerId) != 0;
    }

    // sets the Drag State
    private void setDragState(int state) {
        if (this.mDragState != state) {
            this.mDragState = state;
            this.mCallback.onViewDragStateChanged(state);
            if (this.mDragState == 0) {
                this.mCapturedView = null;
            }
        }

    }

    /**
     * Attempt to capture the view with the given pointer ID. The callback will be involved.
     * This will put us into the "dragging" state.
     *
     * @param toCapture View to capture
     * @param pointerId Pointer to capture with
     */
    private void tryCaptureViewForDrag(Component toCapture, int pointerId) {
        if (toCapture != null && this.mCallback.tryCaptureView(toCapture, pointerId)) {
            this.captureChildView(toCapture);
        }
    }

    // saves latest distance travelled by the pointer
    private void saveDeltaXnY(float dx, float dy) {
        this.mDeltaX = dx;
        this.mDeltaY = dy;
    }

    /**
     * Process a touch event. This method will dispatch callback events
     * as needed before returning.
     *
     * @param ev The touch event received by the parent view.
     */
    public void processTouchEvent(@NotNull TouchEvent ev) {
        int action = ev.getAction();
        final int actionIndex = ev.getIndex();
        if (action == TouchEvent.PRIMARY_POINT_DOWN) {
            // Reset things for a new event stream
            this.cancel();
        }
        if (this.mVelocityDetector == null) {
            this.mVelocityDetector = VelocityDetector.obtainInstance();
        }
        this.mVelocityDetector.addEvent(ev);
        switch (action) {
            case TouchEvent.PRIMARY_POINT_DOWN: {
                final float x = getTouchX(ev, 0);
                final float y = getTouchY(ev, 0);
                final int pointerId = ev.getPointerId(0);
                final Component toCapture = this.findTopChildUnder((int) x, (int) y);
                this.saveInitialMotion(x, y, pointerId);
                this.tryCaptureViewForDrag(toCapture, pointerId);
                break;
            }
            case TouchEvent.PRIMARY_POINT_UP: {
                if (this.mDragState == 1) {
                    this.releaseViewForPointerUp();
                }
                this.cancel();
                break;
            }
            case TouchEvent.POINT_MOVE: {
                if (this.mDragState == STATE_DRAGGING) {
                    final float x = getTouchX(ev, actionIndex);
                    final float y = getTouchY(ev, actionIndex);
                    final int idx = (int) (x - this.mLastMotionX[actionIndex]);
                    final int idy = (int) (y - this.mLastMotionY[actionIndex]);
                    this.dragTo((int) (this.mCapturedView.getContentPositionX() + idx),
                            (int) (this.mCapturedView.getContentPositionY() + idy), idx, idy);
                    this.saveLastMotion(ev);
                }
                break;
            }
            case TouchEvent.CANCEL:
                if (this.mDragState == 1) {
                    this.dispatchViewReleased(0.0F, 0.0F);
                }
                this.cancel();
                break;
            default:
                break;
        }
    }

    private void releaseViewForPointerUp() {
        this.mVelocityDetector.calculateCurrentVelocity(1000);
        float xvel = this.clampMag(this.mVelocityDetector.getHorizontalVelocity(),
                this.mMinVelocity, this.mMaxVelocity);
        float yvel = this.clampMag(this.mVelocityDetector.getVerticalVelocity(), this.mMinVelocity, this.mMaxVelocity);
        this.dispatchViewReleased(xvel, yvel);
    }

    // moves the captured view
    private void dragTo(int left, int top, int dx, int dy) {
        int clampedX = left;
        int clampedY = top;
        int oldLeft = (int) this.mCapturedView.getContentPositionX();
        int oldTop = (int) this.mCapturedView.getContentPositionY();
        if (dx != 0) {
            clampedX = this.mCallback.clampViewPositionHorizontal(this.mCapturedView, left, dx);
            this.mCapturedView.setContentPositionX(clampedX);
        }
        if (dy != 0) {
            clampedY = this.mCallback.clampViewPositionVertical(this.mCapturedView, top, dy);
            this.mCapturedView.setContentPositionY(clampedY);
        }
        if (dx != 0 || dy != 0) {
            int clampedDx = clampedX - oldLeft;
            int clampedDy = clampedY - oldTop;
            saveDeltaXnY(clampedDx, clampedDy);
            this.mCallback.onViewPositionChanged(this.mCapturedView, clampedX, clampedY, clampedDx, clampedDy);
        }

    }

    /**
     * Find the topmost child under the given point within the parent view's coordinate system.
     * The child order is determined using {@link Callback#getOrderedChildIndex(int)}.
     *
     * @param x X position to test in the parent's coordinate system
     * @param y Y position to test in the parent's coordinate system
     * @return The topmost child view under (x, y) or null if none found.
     */
    @Nullable
    private Component findTopChildUnder(int x, int y) {
        int childCount = this.mParentView.getChildCount();
        for (int i = childCount - 1; i >= 0; --i) {
            Component child = this.mParentView.getComponentAt(this.mCallback.getOrderedChildIndex(i));
            if (x >= child.getContentPositionX() && x < child.getRight() && y >= child.getContentPositionY()
                    && y < child.getBottom()) {
                return child;
            }
        }
        return null;
    }

    private boolean isValidPointerForActionMove(int pointerId) {
        return this.isPointerDown(pointerId);
    }

    /**
     * A Callback is used as a communication channel with the ViewDragHelper back to the
     * parent view using it. <code>on*</code>methods are invoked on significant events and several
     * accessor methods are expected to provide the ViewDragHelper with more information
     * about the state of the parent view upon request. The callback also makes decisions
     * governing the range and drag ability of child views.
     */
    protected abstract static class Callback {
        protected Callback() {
        }

        /**
         * Called when the drag state changes. See the <code>STATE_*</code> constants
         * for more information.
         *
         * @param state The new drag state
         *
         * @see #STATE_IDLE
         * @see #STATE_DRAGGING
         */
        public void onViewDragStateChanged(int state) {
        }

        /**
         * Called when the captured view's position changes as the result of a drag or settle.
         *
         * @param changedView View whose position changed
         * @param left New X coordinate of the left edge of the view
         * @param top New Y coordinate of the top edge of the view
         * @param dx Change in X position from the last call
         * @param dy Change in Y position from the last call
         */
        public void onViewPositionChanged(@NotNull Component changedView, int left, int top, int dx, int dy) {
        }

        /**
         * Called when the child view is no longer being actively dragged.
         * The fling velocity is also supplied, if relevant. The velocity values may
         * be clamped to system minimums or maximums.
         *
         * @param releasedChild The captured child view now being released
         * @param xvel X velocity of the pointer as it left the screen in pixels per second.
         * @param yvel Y velocity of the pointer as it left the screen in pixels per second.
         * @param dx Recent X offset of the pointer
         * @param dy Recent Y offset of the pointer
         */
        public void onViewReleased(@NotNull Component releasedChild, float xvel, float yvel, float dx, float dy) {
        }

        /**
         * Called to determine the Z-order of child views.
         *
         * @param index the ordered position to query for
         * @return index of the view that should be ordered at position <code>index</code>
         */
        public int getOrderedChildIndex(int index) {
            return index;
        }

        /**
         * Return the magnitude of a draggable child view's vertical range of motion in pixels.
         * This method should return 0 for views that cannot move vertically.
         *
         * @param child Child view to check
         * @return range of vertical motion in pixels
         */
        public abstract int getViewVerticalDragRange(@NotNull Component child);

        /**
         * Called when the user's input indicates that they want to capture the given child view
         * with the pointer indicated by pointerId. The callback should return true if the user
         * is permitted to drag the given view with the indicated pointer.
         *
         * @param child Child the user is attempting to capture
         * @param pointerId ID of the pointer attempting the capture
         * @return true if capture should be allowed, false otherwise
         */
        public abstract boolean tryCaptureView(@NotNull Component child, int pointerId);

        /**
         * Restrict the motion of the dragged child view along the horizontal axis.
         * The default implementation does not allow horizontal motion; the extending
         * class must override this method and provide the desired clamping.
         *
         *
         * @param child Child view being dragged
         * @param left Attempted motion along the X axis
         * @param dx Proposed change in position for left
         * @return The new clamped position for left
         */
        public abstract int clampViewPositionHorizontal(@NotNull Component child, int left, int dx);

        /**
         * Restrict the motion of the dragged child view along the vertical axis.
         * The default implementation does not allow vertical motion; the extending
         * class must override this method and provide the desired clamping.
         *
         *
         * @param child Child view being dragged
         * @param top Attempted motion along the Y axis
         * @param dy Proposed change in position for top
         * @return The new clamped position for top
         */
        public abstract int clampViewPositionVertical(@NotNull Component child, int top, int dy);
    }

    /**
     * Returns X coordinate of the Touch Event.
     *
     * @param touchEvent The dispatched touch event
     * @param index The index of the pointer
     * @return X coordinate of touch point
     */
    private float getTouchX(TouchEvent touchEvent, int index) {
        float x = 0;
        if (touchEvent.getPointerCount() > index) {
            x = touchEvent.getPointerScreenPosition(index).getX();
        }
        return x;
    }

    /**
     * Returns Y coordinate of the Touch Event.
     *
     * @param touchEvent The dispatched touch event
     * @param index The index of the pointer
     * @return Y coordinate of touch point
     */
    private float getTouchY(TouchEvent touchEvent, int index) {
        float y = 0;
        if (touchEvent.getPointerCount() > index) {
            y = touchEvent.getPointerScreenPosition(index).getY();
        }
        return y;
    }

}