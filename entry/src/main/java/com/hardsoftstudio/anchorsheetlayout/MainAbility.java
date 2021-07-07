package com.hardsoftstudio.anchorsheetlayout;

import com.hardsoftstudio.anchorsheetlayout.slice.MainAbilitySlice;
import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;

/**
 * MainAbility Class for testing AnchorSheetLayout Library.
 */
public class MainAbility extends Ability {
    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setMainRoute(MainAbilitySlice.class.getName());
    }
}
