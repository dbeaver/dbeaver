package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.data.preferences.PrefPageResultSetMain;
import org.jkiss.dbeaver.ui.editors.sql.preferences.PrefPageSQLEditor;

public class PrefPageEclipseGeneral extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.eclipse.main";

    public PrefPageEclipseGeneral() {
        super();
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        Group groupObjects = UIUtils.createControlGroup(composite, CoreMessages.pref_page_eclipse_ui_general_group_general, 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);
        Label descLabel = new Label(groupObjects, SWT.WRAP);
        descLabel.setText(CoreMessages.pref_page_eclipse_ui_general_group_label);

        performDefaults();

        {
            // Link to secure storage config
            addLinkToSettings(composite, PrefPageResultSetMain.PAGE_ID);
            addLinkToSettings(composite, PrefPageSQLEditor.PAGE_ID);
            addLinkToSettings(composite, PrefPageDatabaseNavigator.PAGE_ID);
            addLinkToSettings(composite, PrefPageErrorHandle.PAGE_ID);
        }

        return composite;
    }

    @Override
    public void init(IWorkbench iWorkbench) {

    }

    @Override
    public IAdaptable getElement() {
        return null;
    }

    @Override
    public void setElement(IAdaptable iAdaptable) {

    }

    private void addLinkToSettings(Composite composite, String pageID) {
        new PreferenceLinkArea(composite, SWT.NONE,
                pageID,
                "<a>''{0}''</a> " + CoreMessages.pref_page_ui_general_label_settings,
                (IWorkbenchPreferenceContainer) getContainer(), null); //$NON-NLS-1$
    }
}
