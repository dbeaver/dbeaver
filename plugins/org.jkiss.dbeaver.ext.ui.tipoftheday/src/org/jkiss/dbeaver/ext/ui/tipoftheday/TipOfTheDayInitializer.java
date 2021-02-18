package org.jkiss.dbeaver.ext.ui.tipoftheday;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;
import org.jkiss.utils.CommonUtils;

public class TipOfTheDayInitializer implements IWorkbenchWindowInitializer {
    private static final String PROP_NOT_FIRST_RUN = "tipOfTheDayInitializer.notFirstRun";

    @Override
    public void initializeWorkbenchWindow(IWorkbenchWindow window) {
        if (!isTipsEnabled() || window.getWorkbench().getWorkbenchWindowCount() > 1) {
            return;
        }
        ShowTipOfTheDayHandler.showTipOfTheDay(window);
    }

    private static boolean isTipsEnabled() {
        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PROP_NOT_FIRST_RUN)) {
            DBWorkbench.getPlatform().getPreferenceStore().setValue(PROP_NOT_FIRST_RUN, true);
            return false;
        }
        String tipsEnabledStr = DBWorkbench.getPlatform().getPreferenceStore().getString(ShowTipOfTheDayHandler.UI_SHOW_TIP_OF_THE_DAY_ON_STARTUP);
        if (CommonUtils.isEmpty(tipsEnabledStr)) {
            return true;
        }
        return CommonUtils.toBoolean(tipsEnabledStr);
    }
}
