package org.jkiss.dbeaver.ext.ui.tipoftheday;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;
import org.jkiss.utils.CommonUtils;

public class TipOfTheDayInitializer implements IWorkbenchWindowInitializer {

    @Override
    public void initializeWorkbenchWindow(IWorkbenchWindow window) {
        if (!isTipsEnabled()) {
            return;
        }
        ShowTipOfTheDayHandler.showTipOfTheDay(window);
    }

    private static boolean isTipsEnabled() {
        if (DataSourceRegistry.getAllDataSources().isEmpty()) {
            return false;
        }
        String tipsEnabledStr = DBWorkbench.getPlatform().getPreferenceStore().getString(ShowTipOfTheDayHandler.UI_SHOW_TIP_OF_THE_DAY_ON_STARTUP);
        if (CommonUtils.isEmpty(tipsEnabledStr)) {
            return true;
        }
        return CommonUtils.toBoolean(tipsEnabledStr);
    }

}
