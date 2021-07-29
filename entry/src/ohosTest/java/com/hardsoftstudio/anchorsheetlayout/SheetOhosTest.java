package com.hardsoftstudio.anchorsheetlayout;

import ohos.aafwk.ability.delegation.AbilityDelegatorRegistry;
import ohos.agp.components.Attr;
import ohos.agp.components.AttrSet;
import ohos.agp.components.Component;
import ohos.app.Context;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Testing Class for AnchorSheetLayout Library.
 */
public class SheetOhosTest {

    private AnchorSheetLayout anchorSheetLayout;
    private DragHelper dragHelper;

    @Before
    public void setUp() {
        Context context = AbilityDelegatorRegistry.getAbilityDelegator().getAppContext();
        AttrSet attrSet = new AttrSet() {
            @Override
            public Optional<String> getStyle() {
                return Optional.empty();
            }

            @Override
            public int getLength() {
                return 0;
            }

            @Override
            public Optional<Attr> getAttr(int i) {
                return Optional.empty();
            }

            @Override
            public Optional<Attr> getAttr(String s) {
                return Optional.empty();
            }
        };
        anchorSheetLayout = new AnchorSheetLayout(context, attrSet);
        dragHelper = DragHelper.create(anchorSheetLayout, new DragHelper.Callback() {
            @Override
            public int getViewVerticalDragRange(@NotNull Component child) {
                return 0;
            }

            @Override
            public boolean tryCaptureView(@NotNull Component child, int pointerId) {
                return false;
            }

            @Override
            public int clampViewPositionHorizontal(@NotNull Component child, int left, int dx) {
                return 0;
            }

            @Override
            public int clampViewPositionVertical(@NotNull Component child, int top, int dy) {
                return 0;
            }
        });
    }

    @Test
    public void testPeekHeight() {
        anchorSheetLayout.setPeekHeight(300);
        assertEquals(300, anchorSheetLayout.getPeekHeight());
    }

    @Test
    public void testMinOffset() {
        anchorSheetLayout.setMinOffset(1);
        assertEquals(1, anchorSheetLayout.getMinOffset());
    }

    @Test
    public void testAnchorThreshold() {
        anchorSheetLayout.setAnchorOffset(0.5f);
        assertEquals(0.5f, anchorSheetLayout.getAnchorThreshold(),0.001f);
        assertNotEquals(1, anchorSheetLayout.getAnchorOffset());
    }

    @Test
    public void testHideableTrue() {
        anchorSheetLayout.setCanHide(true);
        assertTrue(anchorSheetLayout.isCanHide());
    }

    @Test
    public void testHideableFalse() {
        anchorSheetLayout.setCanHide(false);
        assertFalse(anchorSheetLayout.isCanHide());
    }

    @Test
    public void testSkipCollapseTrue() {
        anchorSheetLayout.setSkipCollapsed(true);
        assertTrue(anchorSheetLayout.getSkipCollapsed());
    }

    @Test
    public void testSkipCollapseFalse() {
        anchorSheetLayout.setSkipCollapsed(false);
        assertFalse(anchorSheetLayout.getSkipCollapsed());
    }

    @Test
    public void testStateAnchor() {
        anchorSheetLayout.setState(AnchorSheetLayout.STATE_ANCHOR);
        assertEquals(AnchorSheetLayout.STATE_ANCHOR, anchorSheetLayout.getState());
    }

    @Test
    public void testStateExpanded() {
        anchorSheetLayout.setState(AnchorSheetLayout.STATE_EXPANDED);
        assertEquals(AnchorSheetLayout.STATE_EXPANDED, anchorSheetLayout.getState());
    }

    @Test
    public void testStateHidden() {
        anchorSheetLayout.setState(AnchorSheetLayout.STATE_HIDDEN);
        assertEquals(AnchorSheetLayout.STATE_HIDDEN, anchorSheetLayout.getState());
    }

    @Test
    public void testStateForceHidden() {
        anchorSheetLayout.setState(AnchorSheetLayout.STATE_FORCE_HIDDEN);
        assertEquals(AnchorSheetLayout.STATE_FORCE_HIDDEN, anchorSheetLayout.getState());
    }

    @Test
    public void testStateCollapsed() {
        anchorSheetLayout.setState(AnchorSheetLayout.STATE_COLLAPSED);
        assertEquals(AnchorSheetLayout.STATE_COLLAPSED,anchorSheetLayout.getState());
    }

    @Test
    public void testDragHelper() {
        assertNotNull(dragHelper);
    }

    @Test
    public void testDragHelperMaxVelocity() {
        double delta = 0.0001f;
        assertEquals(DragHelper.DEFAULT_MAX_VELOCITY, dragHelper.getMaxVelocity(), delta);
    }

    @Test
    public void testDragHelperMinVelocity() {
        double difference = 0.00001f;
        assertEquals(DragHelper.DEFAULT_MIN_VELOCITY, dragHelper.getMinVelocity(), difference);
    }

    @Test
    public void testDragHelperCallBack() {
        assertNotNull(dragHelper.getCallback());
    }

}