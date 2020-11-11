package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;

import java.io.IOException;
import java.util.Map;

public class SQLEditorHandlerEnableDisableFolding extends AbstractHandler implements IElementUpdater {
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        boolean previousValue = preferenceStore.getBoolean(SQLPreferenceConstants.FOLDING_ENABLED);
        preferenceStore.setValue(SQLPreferenceConstants.FOLDING_ENABLED, !previousValue);
        try {
            preferenceStore.save();
        } catch (IOException e) {
            throw new ExecutionException("Error saving folding preference", e);
        }
        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        element.setChecked(DBWorkbench.getPlatform().getPreferenceStore().getBoolean(SQLPreferenceConstants.FOLDING_ENABLED));
    }
}
