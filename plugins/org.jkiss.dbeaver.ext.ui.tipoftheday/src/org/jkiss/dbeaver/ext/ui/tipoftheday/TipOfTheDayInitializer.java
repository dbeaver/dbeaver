package org.jkiss.dbeaver.ext.ui.tipoftheday;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;

public class TipOfTheDayInitializer implements IWorkbenchWindowInitializer {

    @Override
    public void initializeWorkbenchWindow(IWorkbenchWindow window) {
        if (doNotShowTips()) {
            return;
        }
        ShowTipOfTheDayHandler.showTipOfTheDay(window);
    }

    private static boolean doNotShowTips() {
        boolean enabled = DBeaverCore.getGlobalPreferenceStore().getBoolean(ShowTipOfTheDayHandler.UI_SHOW_TIP_OF_THE_DAY_ON_STARTUP);
        boolean emptyDatasource = DataSourceRegistry.getAllDataSources().isEmpty();
        return !enabled || emptyDatasource;
    }

}
