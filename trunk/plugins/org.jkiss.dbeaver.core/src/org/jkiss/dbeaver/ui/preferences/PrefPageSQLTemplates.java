package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.texteditor.templates.TemplatePreferencePage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplateStore;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplatesRegistry;

/**
 * Templates preference page
 */
public class PrefPageSQLTemplates extends TemplatePreferencePage implements IWorkbenchPropertyPage {
    public PrefPageSQLTemplates()
    {
        setPreferenceStore(DBeaverCore.getInstance().getGlobalPreferenceStore());
        setTemplateStore(SQLTemplatesRegistry.getInstance().getTemplateStore());
        setContextTypeRegistry(SQLTemplatesRegistry.getInstance().getTemplateContextRegistry());
    }

    protected String getFormatterPreferenceKey() {
        return SQLTemplateStore.PREF_STORE_KEY;
    }

    @Override
    public IAdaptable getElement()
    {
        return null;
    }

    @Override
    public void setElement(IAdaptable element)
    {
    }
}
