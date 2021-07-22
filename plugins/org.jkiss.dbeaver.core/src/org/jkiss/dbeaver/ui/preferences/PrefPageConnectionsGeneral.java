package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.registry.DataSourceNavigatorSettings;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CSmartCombo;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageGeneral;
import org.jkiss.dbeaver.ui.dialogs.connection.NavigatorSettingsStorage;

public class PrefPageConnectionsGeneral extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage, NavigatorSettingsStorage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.connections";

    private CSmartCombo<DBPConnectionType> connectionTypeCombo;
    private Combo navigatorSettingsCombo;

    private DBPConnectionType defaultConnectionType;
    private DBNBrowseSettings defaultNavigatorSettings;

    public PrefPageConnectionsGeneral() {
        super();
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));

        defaultNavigatorSettings = DataSourceNavigatorSettings.PRESET_FULL.getSettings();
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        {
            Group groupDefaults = UIUtils.createControlGroup(composite, "Default settings for new connections", 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            connectionTypeCombo = ConnectionPageGeneral.createConnectionTypeCombo(groupDefaults);
            connectionTypeCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    defaultConnectionType = connectionTypeCombo.getSelectedItem();
                }
            });
            navigatorSettingsCombo = ConnectionPageGeneral.createNavigatorSettingsCombo(groupDefaults, this, null);
        }

        {
            Group groupObjects = UIUtils.createControlGroup(composite, CoreMessages.pref_page_eclipse_ui_general_group_general, 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);
            Label descLabel = new Label(groupObjects, SWT.WRAP);
            descLabel.setText(CoreMessages.pref_page_eclipse_ui_general_connections_group_label);

            // Link to secure storage config
            addLinkToSettings(groupObjects, PrefPageDrivers.PAGE_ID);
            addLinkToSettings(groupObjects, PrefPageErrorHandle.PAGE_ID);
            addLinkToSettings(groupObjects, PrefPageMetaData.PAGE_ID);
            addLinkToSettings(groupObjects, PrefPageTransactions.PAGE_ID);
        }

        performDefaults();

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

    @Override
    public DBNBrowseSettings getNavigatorSettings() {
        return defaultNavigatorSettings;
    }

    @Override
    public void setNavigatorSettings(DBNBrowseSettings settings) {
        this.defaultNavigatorSettings = settings;
    }

    @Override
    protected void performDefaults()
    {
        defaultConnectionType = DBPConnectionType.getDefaultConnectionType();
        connectionTypeCombo.select(defaultConnectionType);

        defaultNavigatorSettings = DataSourceNavigatorSettings.getDefaultSettings();
        ConnectionPageGeneral.updateNavigatorSettingsPreset(navigatorSettingsCombo, defaultNavigatorSettings);

        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        if (defaultConnectionType != DBPConnectionType.getDefaultConnectionType()) {
            DBPConnectionType.setDefaultConnectionType(defaultConnectionType);
        }
        if (!defaultNavigatorSettings.equals(DataSourceNavigatorSettings.getDefaultSettings())) {
            DataSourceNavigatorSettings.setDefaultSettings(defaultNavigatorSettings);
        }

        return super.performOk();
    }

}
