package com.hardsoftstudio.anchorsheetlayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ohos.agp.animation.Animator;
import ohos.agp.animation.AnimatorValue;
import ohos.agp.components.Attr;
import ohos.agp.components.AttrSet;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.app.Context;
import ohos.multimodalinput.event.TouchEvent;

/**
 * AnchorSheetLayout is a custom layout where it can support only one child. So, for adding multiple
 * components add a single layout as child and add remaining components as children to that layout.
 */
public class AnchorSheetLayout extends ComponentContainer {
    /**
     * Callback for monitoring events about bottom sheets.
     */
    public abstract static class AnchorSheetCallback {
        /**
         * Called when the bottom sheet changes its state.
         *
         * @param bottomSheet The bottom sheet view.
         * @param newState    The new state. This will be one of {@link #STATE_DRAGGING},
         *                    {@link #STATE_SETTLING}, {@link #STATE_EXPANDED},
         *                    {@link #STATE_COLLAPSED}, or {@link #STATE_HIDDEN}.
         */
        public abstract void onStateChanged(Component bottomSheet, @State int newState);

        /**
         * Called when the bottom sheet is being dragged.
         *
         * @param bottomSheet The bottom sheet view.
         * @param slideOffset The new offset of this bottom sheet within [-1,1] range. Offset
         *                    increases as this bottom sheet is moving upward. From 0 to 1 the sheet
         *                    is between collapsed and expanded states and from -1 to 0 it is
         *                    between hidden and collapsed states.
         */
        public abstract void onSlide(Component bottomSheet, float slideOffset);
    }

    /**
     * The Child of AnchorSheetLayout. There can be only one child component,
     * in case need to add more components, add a component container and place everything in it
     */
    private ComponentContainer child;

    /**
     * The bottom sheet is dragging.
     */
    public static final int STATE_DRAGGING = 1;

    /**
     * The bottom sheet is settling.
     */
    public static final int STATE_SETTLING = 2;

    /**
     * The bottom sheet is expanded.
     */
    public static final int STATE_EXPANDED = 3;

    /**
     * The bottom sheet is collapsed.
     */
    public static final int STATE_COLLAPSED = 4;

    /**
     * The bottom sheet is hidden.
     */
    public static final int STATE_HIDDEN = 5;

    /**
     * The bottom sheet is anchor.
     */
    public static final int STATE_ANCHOR = 6;

    /**
     * The bottom sheet is forced to be hidden programmatically.
     */
    public static final int STATE_FORCE_HIDDEN = 7;

    /**
     * The possible states of the sheet.
     */
    @IntDef({
        STATE_EXPANDED,
        STATE_COLLAPSED,
        STATE_DRAGGING,
        STATE_SETTLING,
        STATE_HIDDEN,
        STATE_ANCHOR,
        STATE_FORCE_HIDDEN
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
    }
    // Threshold to make sheet hide
    private static final float HIDE_THRESHOLD = 0.25f;

    // Resistance given to vertical velocity
    private static final float HIDE_FRICTION = 0.1f;

    // Default values
    private static final float ANCHOR_THRESHOLD = 0.50f;
    private static final int DEFAULT_PEEK_HEIGHT = 217;
    private static final int DEFAULT_MIN_OFFSET = 0;
    private static final boolean CAN_HIDE = true;
    private static final boolean SKIP_COLLAPSE = false;

    // Decides the height of the Sheet in Anchor State
    private float mAnchorThreshold = ANCHOR_THRESHOLD;

    // Distance between Layout Top and Child Top in Hidden State
    private int mMinOffset;

    // Distance between Layout Top and Child Top in Expanded State
    private int mMaxOffset;

    // Distance between Layout Top and Child Top in Anchor State
    private int mAnchorOffset;

    // Says whether Anchor Sheet can go to Hidden State
    private boolean mHideable;

    // Whether to avoid Collapse State when Sheet is moving down
    private boolean mSkipCollapsed;

    @State
    private int mState = STATE_COLLAPSED;

    private DragHelper mDragHelper;

    private int mParentHeight;

    // reference to one and only child
    private WeakReference<ComponentContainer> mViewRef;

    private AnchorSheetCallback mCallback;

    // Touched scrollable component
    private boolean mIsTouchOnScroll = false;

    // First move action in a touch event
    private boolean mFirstMove = true;

    // Scrollable component receiving the touch event
    private Component mScrollView;

    // last touch point
    private float lastY = 0;

    // list for storing all the scrollable children references
    private final List<WeakReference<Component>> mChildrenList = new ArrayList<>();

    // XML attribute
    private static final String ATTR_PEEK_HEIGHT = "peekHeight";
    // Height of the Sheet when in Collapsed State
    private int peekHeight;

    /**
     * Constructor of the AnchorSheetLayout.
     *
     * @param context The {@link Context}.
     * @param attrs   The {@link AttrSet}.
     */
    public AnchorSheetLayout(Context context, AttrSet attrs) {
        super(context, attrs);
        if (attrs != null) {
            Optional<Attr> value = attrs.getAttr(ATTR_PEEK_HEIGHT);
            this.peekHeight = value.map(Attr::getIntegerValue).orElse(DEFAULT_PEEK_HEIGHT);
        }
        setMinOffset(DEFAULT_MIN_OFFSET);
        setHideable(CAN_HIDE);
        setSkipCollapsed(SKIP_COLLAPSE);
        this.setBindStateChangedListener(new BindStateChangedListener() {
            @Override
            public void onComponentBoundToWindow(Component component) {
                mDragHelper = DragHelper.create((ComponentContainer) component, mDragCallback);
                mParentHeight = component.getHeight();
                setAnchorOffset(mAnchorThreshold);
                setPeekHeight(peekHeight);
            }

            @Override
            public void onComponentUnboundFromWindow(Component component) {
                // Do nothing
            }
        });
    }

    private final DragHelper.Callback mDragCallback = new DragHelper.Callback() {

        // called whenever dragHelper is trying to capture the view
        @Override
        public boolean tryCaptureView(Component child, int pointerId) {
            if (mState == STATE_DRAGGING) {
                return false;
            }
            return mViewRef != null && mViewRef.get() == child;
        }

        // called when the position of the view is changed
        @Override
        public void onViewPositionChanged(Component changedView, int left, int top, int dx, int dy) {
            dispatchOnSlide(top);
        }

        // provides CallBack call
        private void dispatchOnSlide(int top) {
            Component bottomSheet = mViewRef.get();
            if (bottomSheet != null && mCallback != null) {
                if (top > mMaxOffset) {
                    mCallback.onSlide(bottomSheet, (float) (mMaxOffset - top)
                            / (mParentHeight - mMaxOffset));
                } else {
                    mCallback.onSlide(bottomSheet,
                            (float) (mMaxOffset - top) / (mMaxOffset - mMinOffset));
                }
            }
        }

        // called when the state of the captured view is changed
        @Override
        public void onViewDragStateChanged(int state) {
            if (state == DragHelper.STATE_DRAGGING) {
                setStateInternal(STATE_DRAGGING);
            }
        }

        // called when the captured view is released
        @Override
        public void onViewReleased(Component releasedChild, float xvel, float yvel, float dx, float dy) {
            int currentTop = (int) releasedChild.getContentPositionY();
            @State int targetState;

            if (yvel == 0.f) { // velocity is zero
                if (Math.abs(currentTop - mMinOffset) < Math.abs(currentTop - mAnchorOffset)) {
                    targetState = STATE_EXPANDED;
                } else if (Math.abs(currentTop - mAnchorOffset) < Math.abs(currentTop - mMaxOffset)) {
                    targetState = STATE_ANCHOR;
                } else {
                    targetState = STATE_COLLAPSED;
                }
            } else if (dy < 0) { // moving up
                if (currentTop < mAnchorOffset) {
                    targetState = STATE_EXPANDED;
                } else {
                    targetState = STATE_ANCHOR;
                }
            } else if (dy > 0) { // moving down
                if ((isHideable() && shouldHide(releasedChild, yvel)) || getSkipCollapsed()) {
                    targetState = STATE_HIDDEN;
                } else {
                    targetState = STATE_COLLAPSED;
                }
            } else { // just a click
                if (currentTop == mAnchorOffset) {
                    targetState = STATE_ANCHOR;
                } else if (currentTop == mMinOffset) {
                    targetState = STATE_EXPANDED;
                } else if (currentTop == mMaxOffset) {
                    targetState = STATE_COLLAPSED;
                } else {
                    targetState = mState;
                }
            }
            if (targetState != mState) {
                startSettlingAnimation(releasedChild, targetState, (int) yvel);
            }
        }

        // returns the vertical position of the captured view when it's been dragged
        @Override
        public int clampViewPositionVertical(Component child, int top, int dy) {
            return Math.min(mHideable ? mParentHeight : mMaxOffset, Math.max(mMinOffset, top));
        }

        // returns the horizontal position of the captured view when it's been dragged
        @Override
        public int clampViewPositionHorizontal(Component child, int left, int dx) {
            return child.getLeft();
        }

        // possible vertical drag
        @Override
        public int getViewVerticalDragRange(Component child) {
            if (mHideable) {
                return mParentHeight - mMinOffset;
            } else {
                return mMaxOffset - mMinOffset;
            }
        }
    };

    /**
     * Called When parent is trying to lay out the child.
     *
     * @param comChild One and Only Child
     */
    @Override
    public void addComponent(Component comChild) {
        if (getChildCount() > 0) {
            throw new IllegalArgumentException("You cannot declare more then one child");
        }
        super.addComponent(comChild);
        this.child = (ComponentContainer) comChild;
        this.child.setTouchEventListener(touchEventListener);
        mViewRef = new WeakReference<>(this.child);
        child.setBindStateChangedListener(new BindStateChangedListener() {
            @Override
            public void onComponentBoundToWindow(Component component) {
                mChildrenList.clear();
                // find all the scrollable children
                findScrollingChild(child);
                // set the child position
                switch (mState) {
                    case STATE_EXPANDED:
                        child.setContentPositionY(mMinOffset);
                        break;
                    case STATE_COLLAPSED:
                        child.setContentPositionY(mMaxOffset);
                        break;
                    case STATE_ANCHOR:
                        child.setContentPositionY(mAnchorOffset);
                        break;
                    case STATE_HIDDEN:
                    case STATE_FORCE_HIDDEN:
                        child.setContentPositionY(mParentHeight);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onComponentUnboundFromWindow(Component component) {
                // Do nothing
            }
        });
    }

    private final TouchEventListener touchEventListener = (component, event) -> {
        int action = event.getAction();
        float currentY = event.getPointerScreenPosition(0).getY();

        switch (action) {
            case TouchEvent.PRIMARY_POINT_DOWN:
                mFirstMove = true;
                lastY = currentY;
                if (mState == STATE_EXPANDED) {
                    // traverse through all the children and see whether touch given to any scrollable child
                    for (WeakReference<Component> componentWeakReference : mChildrenList) {
                        float x = getTouchX(event, 0);
                        float y = getTouchY(event, 0);
                        Component listView = componentWeakReference.get();
                        float listX1 = listView.getContentPositionX();
                        float listX2 = listView.getContentPositionX() + listView.getWidth();
                        float listY1 = listView.getContentPositionY();
                        float listY2 = listView.getContentPositionY() + listView.getHeight();
                        if (listX1 <= x && listX2 >= x && listY1 <= y && listY2 >= y) {
                            mScrollView = componentWeakReference.get();
                            if (listView.canScroll(DRAG_DOWN)) {
                                mIsTouchOnScroll = true;
                                return true;
                            }
                        }
                    }
                }
                break;
            case TouchEvent.PRIMARY_POINT_UP:
                mScrollView = null;
                mIsTouchOnScroll = false;
                break;
            case TouchEvent.POINT_MOVE:
                float deltaY = currentY - lastY;
                if (mFirstMove && mScrollView != null && deltaY <= 0) {
                    mIsTouchOnScroll = true;
                }
                mFirstMove = false;
                if (mIsTouchOnScroll) {
                    return true;
                }
                lastY  = currentY;
                break;
            default:
                break;
        }
        if (mDragHelper != null) {
            mDragHelper.captureChildView(child);
            mDragHelper.processTouchEvent(event);
        } else {
            throw new IllegalArgumentException("ViewDragHelper may not be null");
        }
        return true;
    };

    /**
     * Stores references of all the scrollable components present in the child.
     *
     * @param component One and Only Child
     */
    private void findScrollingChild(Component component) {
        if (component.canScroll(DRAG_DOWN) || component.canScroll(DRAG_UP)) {
            mChildrenList.add(new WeakReference<>(component));
            return;
        }
        if (component instanceof ComponentContainer) {
            ComponentContainer group = (ComponentContainer) component;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                Component scrollingChild = group.getComponentAt(i);
                findScrollingChild(scrollingChild);
            }
        }
    }

    /**
     * Sets the height of the bottom sheet when it is collapsed.
     *
     * @param peekHeight The height of the collapsed bottom sheet in pixels.
     */
    public final void setPeekHeight(int peekHeight) {
        this.peekHeight = Math.max(0, peekHeight);
        mMaxOffset = mParentHeight - peekHeight;
    }

    /**
     * Gets the height of the bottom sheet when it is collapsed.
     *
     * @return The height of the collapsed bottom sheet in pixels.
     */
    public final int getPeekHeight() {
        return peekHeight;
    }

    /**
     * Gets the distance between parent top and child top when sheet is expanded.
     *
     * @return The possible minimum distance between parent top and child top
     */
    public int getMinOffset() {
        return mMinOffset;
    }

    /**
     * Sets the distance between parent top and child top when sheet is expanded.
     *
     * @param minOffset The possible minimum distance between parent top and child top
     */
    public void setMinOffset(int minOffset) {
        this.mMinOffset = minOffset;
    }

    /**
     * Get the size in pixels from the anchor state to the top of the parent (Expanded state).
     *
     * @return pixel size of the anchor state
     */
    public int getAnchorOffset() {
        return mAnchorOffset;
    }

    /**
     * The multiplier between 0..1 to calculate the Anchor offset.
     *
     * @return float between 0..1
     */
    public float getAnchorThreshold() {
        return mAnchorThreshold;
    }

    /**
     * Set the offset for the anchor state. Number between 0..1
     * i.e: Anchor the panel at 1/3 of the screen: setAnchorOffset(0.25).
     *
     * @param threshold {@link Float} from 0..1
     */
    public void setAnchorOffset(float threshold) {
        this.mAnchorThreshold = threshold;
        this.mAnchorOffset = (int) Math.max(mParentHeight * mAnchorThreshold, mMinOffset);
    }

    /**
     * Sets whether this bottom sheet can hide when it is swiped down.
     *
     * @param hideable {@code true} to make this bottom sheet hideable.
     */
    public void setHideable(boolean hideable) {
        mHideable = hideable;
    }

    /**
     * Gets whether this bottom sheet can hide when it is swiped down.
     *
     * @return {@code true} if this bottom sheet can hide.
     */
    public boolean isHideable() {
        return mHideable;
    }

    /**
     * Sets whether this bottom sheet should skip the collapsed state when it is being hidden
     * after it is expanded once. Setting this to true has no effect unless the sheet is hideable.
     *
     * @param skipCollapsed True if the bottom sheet should skip the collapsed state.
     */
    public void setSkipCollapsed(boolean skipCollapsed) {
        mSkipCollapsed = skipCollapsed;
    }

    /**
     * Sets whether this bottom sheet should skip the collapsed state when it is being hidden
     * after it is expanded once.
     *
     * @return Whether the bottom sheet should skip the collapsed state.
     */
    public boolean getSkipCollapsed() {
        return mSkipCollapsed;
    }

    /**
     * Sets a callback to be notified of bottom sheet events.
     *
     * @param callback The callback to notify when bottom sheet events occur.
     */
    public void setAnchorSheetCallback(AnchorSheetCallback callback) {
        mCallback = callback;
    }

    /**
     * Returns the current state of the Sheet.
     *
     * @return Current State of the Sheet
     */
    @State
    public final int getState() {
        return mState;
    }

    /**
     * Provides callback to the section using AnchorSheetLayout.
     *
     * @param state State of the Sheet
     */
    private void setStateInternal(@State int state) {
        if (mState == state) {
            return;
        }
        mState = state;
        Component bottomSheet = mViewRef.get();
        if (bottomSheet != null && mCallback != null) {
            mCallback.onStateChanged(bottomSheet, state);
        }
    }

    /**
     * Checks whether to hide the sheet or not, depending upon the component
     * position and velocity at which it is thrown.
     *
     * @param child Captured component
     * @param yvel Y velocity
     * @return Whether to hide the sheet or not
     */
    boolean shouldHide(Component child, float yvel) {
        if (mSkipCollapsed) {
            return true;
        }
        if (child.getContentPositionY() < mMaxOffset) {
            // It should not hide, but collapse.
            return false;
        }
        final float newTop = child.getContentPositionY() + yvel * HIDE_FRICTION;
        return Math.abs(newTop - mMaxOffset) / (float) peekHeight > HIDE_THRESHOLD;
    }



    /**
     * Returns X coordinate of the Touch Event.
     *
     * @param touchEvent The dispatched touch event
     * @param index The index of the pointer
     * @return X coordinate of touch point
     */
    public static float getTouchX(TouchEvent touchEvent, int index) {
        float x = 0;
        if (touchEvent.getPointerCount() > index) {
            x = touchEvent.getPointerPosition(index).getX();
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
    public static float getTouchY(TouchEvent touchEvent, int index) {
        float y = 0;
        if (touchEvent.getPointerCount() > index) {
            y = touchEvent.getPointerPosition(index).getY();
        }
        return y;
    }

    /**
     * Sets the state of the bottom sheet. The bottom sheet will transition to that state with
     * animation.
     *
     * @param state One of {@link #STATE_COLLAPSED}, {@link #STATE_EXPANDED}, or
     *              {@link #STATE_HIDDEN}.
     */
    public final void setState(@State int state) {
        if (mState == state) {
            return;
        }
        if (mViewRef == null) {
            // The view is not laid out yet; modify mState and let addComponent handle it later
            if (state == STATE_COLLAPSED || state == STATE_EXPANDED || state == STATE_ANCHOR
                    || ((mHideable && state == STATE_HIDDEN) || state == STATE_FORCE_HIDDEN)) {
                mState = state;
            }
            return;
        }
        ComponentContainer viewChild = mViewRef.get();
        if (viewChild == null) {
            return;
        }
        startSettlingAnimation(viewChild, state, 0);
    }

    /**
     * Provides animation for settling state of the AnchorSheet.
     *
     * @param child The Captured Child
     * @param state The State of the AnchorSheet
     * @param yvel The Vertical Velocity at which view is released
     */
    private void startSettlingAnimation(Component child, int state, int yvel) {
        int top;
        int currentTop = (int) child.getContentPositionY();
        if (state == STATE_ANCHOR) {
            top = mAnchorOffset;
        } else if (state == STATE_COLLAPSED) {
            top = mMaxOffset;
        } else if (state == STATE_EXPANDED) {
            top = mMinOffset;
        } else if ((mHideable && state == STATE_HIDDEN) || state == STATE_FORCE_HIDDEN) {
            top = mParentHeight;
        } else {
            throw new IllegalArgumentException("Illegal state argument: " + state);
        }
        AnimatorValue animatorValue = new AnimatorValue();
        animatorValue.setDuration(computeSettleDuration(child, currentTop - top, yvel));
        animatorValue.setLoopedCount(0);
        animatorValue.setCurveType(Animator.CurveType.LINEAR);
        animatorValue.setValueUpdateListener((animatorValue1, v) -> {
            child.setContentPositionY(v * (top - currentTop) + currentTop);
            if (v > 0.999999f) {
                setStateInternal(state);
            } else {
                setStateInternal(STATE_SETTLING);
            }
        });
        animatorValue.start();
    }

    /**
     * Called by {@link #startSettlingAnimation(Component, int, int) } to find
     * the duration of the animation.
     *
     * @param child component on which animation is going
     * @param dy Y distance
     * @param yvel Y Velocity
     * @return time in milliseconds
     */
    private int computeSettleDuration(Component child, int dy, int yvel) {
        yvel = this.clampMag(yvel, (int) mDragHelper.getMinVelocity(), (int) mDragHelper.getMaxVelocity());
        return this.computeAxisDuration(dy, yvel, mDragHelper.getCallback().getViewVerticalDragRange(child));
    }

    /**
     * Time taken for settling.
     *
     * @param delta distance
     * @param velocity velocity with it travels
     * @param motionRange possible vertical drag range
     * @return Time taken for settling
     */
    private int computeAxisDuration(int delta, int velocity, int motionRange) {
        if (delta == 0) {
            return 0;
        } else {
            int width = this.getWidth();
            int halfWidth = width / 2;
            float distanceRatio = Math.min(1.0F, (float) Math.abs(delta) / (float) width);
            float distance = (float) halfWidth + (float) halfWidth
                    * this.distanceInfluenceForSnapDuration(distanceRatio);
            velocity = Math.abs(velocity);
            int duration;
            if (velocity > 0) {
                duration = 4 * Math.round(1000.0F * Math.abs(distance / (float) velocity));
            } else {
                float range = (float) Math.abs(delta) / (float) motionRange;
                duration = (int) ((range + 1.0F) * 256.0F);
            }
            return Math.min(duration, 200);
        }
    }

    // helper function for computeAxisDuration
    private float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5F; // center the values about 0.
        f *= 0.47123894F;
        return (float) Math.sin(f);
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
    private int clampMag(int value, int absMin, int absMax) {
        int absValue = Math.abs(value);
        if (absValue < absMin) {
            return 0;
        } else if (absValue > absMax) {
            return value > 0 ? absMax : -absMax;
        } else {
            return value;
        }
    }
}
