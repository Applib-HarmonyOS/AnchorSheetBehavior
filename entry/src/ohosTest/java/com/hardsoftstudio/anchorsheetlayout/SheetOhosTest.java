package com.hardsoftstudio.anchorsheetlayout;

import ohos.aafwk.ability.delegation.AbilityDelegatorRegistry;
import ohos.agp.components.Attr;
import ohos.agp.components.AttrSet;
import ohos.agp.components.Component;
import ohos.app.Context;
import org.junit.Test;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Testing Class for AnchorSheetLayout Library.
 */
public class SheetOhosTest {

    private final Context context = AbilityDelegatorRegistry.getAbilityDelegator().getAppContext();

    private final AttrSet attrSet = new AttrSet() {
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

    private final AnchorSheetLayout anchorSheetLayout = new AnchorSheetLayout(context,attrSet);

    @Test
    public void testBundleName() {
        final String actualBundleName = AbilityDelegatorRegistry.getArguments().getTestBundleName();
        assertEquals("com.hardsoftstudio.anchorsheetlayout", actualBundleName);
    }

    @Test
    public void testPeekHeight(){
        anchorSheetLayout.setPeekHeight(300);
        assertEquals(300,anchorSheetLayout.getPeekHeight());
    }

    @Test
    public void testMinOffSet(){
        anchorSheetLayout.setMinOffset(1);
        assertEquals(1,anchorSheetLayout.getMinOffset());
    }

    @Test
    public void testAnchorOffset(){
        anchorSheetLayout.setAnchorOffset(0.5f);
        assertEquals(0.5f,anchorSheetLayout.getAnchorThreshold(),0.001f);
        assertNotEquals(1,anchorSheetLayout.getAnchorOffset());
    }

    @Test
    public void testHideable(){
        assertTrue(anchorSheetLayout.isHideable());
        anchorSheetLayout.setHideable(false);
        assertFalse(anchorSheetLayout.isHideable());
    }

    @Test
    public void testSkipCollapse(){
        assertFalse(anchorSheetLayout.getSkipCollapsed());
        anchorSheetLayout.setSkipCollapsed(true);
        assertTrue(anchorSheetLayout.getSkipCollapsed());
    }

    @Test
    public void testState(){
        anchorSheetLayout.setState(AnchorSheetLayout.STATE_ANCHOR);
        assertEquals(AnchorSheetLayout.STATE_ANCHOR,anchorSheetLayout.getState());
    }

    private final DragHelper dragHelper = DragHelper.create(anchorSheetLayout, new DragHelper.Callback() {
        @Override
        public int getViewVerticalDragRange(Component child) {
            return 0;
        }

        @Override
        public boolean tryCaptureView(Component child, int pointerId) {
            return false;
        }

        @Override
        public int clampViewPositionHorizontal(Component child, int left, int dx) {
            return 0;
        }

        @Override
        public int clampViewPositionVertical(Component child, int top, int dy) {
            return 0;
        }
    });

    @Test
    public void testDragHelper(){
        assertNotNull(dragHelper);
    }

    @Test
    public void testDragHelperMaxVelocity(){
        assertEquals(DragHelper.DEFAULT_MAX_VELOCITY,dragHelper.getMaxVelocity(),0.01f);
    }

    @Test
    public void testDragHelperMinVelocity(){
        assertEquals(DragHelper.DEFAULT_MIN_VELOCITY,dragHelper.getMinVelocity(),0.01f);
    }

    @Test
    public void testDragHelperCallBack(){
        assertNotNull(dragHelper.getCallback());
    }

}