/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

/**
 * PrefPageDatabaseGeneral
 */
public class PrefPageDatabaseGeneral extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.common"; //$NON-NLS-1$

    private Button automaticUpdateCheck;

    private Button longOperationsCheck;
    private Spinner longOperationsTimeout;

    private Button keepEditorsOnRestart;
    private Button refreshEditorOnOpen;
    private Button syncEditorDataSourceWithNavigator;
    private Combo defaultResourceEncoding;

    public PrefPageDatabaseGeneral()
    {
        super();
        setPreferenceStore(new PreferenceStoreDelegate(DBeaverCore.getGlobalPreferenceStore()));
    }

    @Override
    public void init(IWorkbench workbench)
    {

    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        {
            Group groupObjects = UIUtils.createControlGroup(composite, CoreMessages.pref_page_ui_general_group_general, 1, GridData.VERTICAL_ALIGN_BEGINNING, 300);
            automaticUpdateCheck = UIUtils.createCheckbox(groupObjects, CoreMessages.pref_page_ui_general_checkbox_automatic_updates, false);
            automaticUpdateCheck.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, true, false, 2, 1));
        }

        // Agent settings
        {
            Group agentGroup = UIUtils.createControlGroup(composite, "Task Bar", 2, SWT.NONE, 0);

            longOperationsCheck = UIUtils.createCheckbox(agentGroup, "Enable long-time operations notification", false);
            longOperationsCheck.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, true, false, 2, 1));
            longOperationsCheck.setToolTipText("Shows special notification in system taskbar after long-time operation (e.g. SQL query) finish.");

            longOperationsTimeout = UIUtils.createLabelSpinner(agentGroup, "Long-time operation timeout", 0, 0, Integer.MAX_VALUE);

            if (RuntimeUtils.isPlatformMacOS()) {
                ControlEnableState.disable(agentGroup);
            }
        }

        // Editors settings
        {
            Group groupEditors = UIUtils.createControlGroup(composite, CoreMessages.pref_page_ui_general_group_editors, 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            keepEditorsOnRestart = UIUtils.createCheckbox(groupEditors, CoreMessages.pref_page_ui_general_keep_database_editors, false);
            keepEditorsOnRestart.setToolTipText("Remembers open editors (e.g. table editors) and reopens them after DBeaver restart.");

            refreshEditorOnOpen = UIUtils.createCheckbox(groupEditors, CoreMessages.pref_page_ui_general_refresh_editor_on_open, false);
            refreshEditorOnOpen.setToolTipText("Refreshes object from database every time you open this object's editor.\nYou may need this option if your database structure changes frequently (e.g. by SQL scripts).");

            syncEditorDataSourceWithNavigator = UIUtils.createCheckbox(groupEditors, "Auto-sync editor connection with navigator selection", false);
            syncEditorDataSourceWithNavigator.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, true, false, 2, 1));
            syncEditorDataSourceWithNavigator.setToolTipText("Automatically sets editor (e.g. SQL editor) connection from selected navigator node.\nMakes sense if you need to change active connection/schema frequently.");
        }
        {
            // Resources
            Group groupResources = UIUtils.createControlGroup(composite, "Resources", 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            UIUtils.createControlLabel(groupResources, "Default resource encoding");
            defaultResourceEncoding = UIUtils.createEncodingCombo(groupResources, GeneralUtils.DEFAULT_ENCODING);
            defaultResourceEncoding.setToolTipText("Default encoding for scripts and text files. Change requires restart");

        }

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        automaticUpdateCheck.setSelection(store.getBoolean(DBeaverPreferences.UI_AUTO_UPDATE_CHECK));
        longOperationsCheck.setSelection(store.getBoolean(DBeaverPreferences.AGENT_LONG_OPERATION_NOTIFY));
        longOperationsTimeout.setSelection(store.getInt(DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT));

        keepEditorsOnRestart.setSelection(store.getBoolean(DBeaverPreferences.UI_KEEP_DATABASE_EDITORS));
        refreshEditorOnOpen.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_REFRESH_EDITORS_ON_OPEN));
        syncEditorDataSourceWithNavigator.setSelection(store.getBoolean(DBeaverPreferences.NAVIGATOR_SYNC_EDITOR_DATASOURCE));
        defaultResourceEncoding.setText(store.getString(DBeaverPreferences.DEFAULT_RESOURCE_ENCODING));
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        store.setValue(DBeaverPreferences.UI_AUTO_UPDATE_CHECK, automaticUpdateCheck.getSelection());
        //store.setValue(DBeaverPreferences.AGENT_ENABLED, agentEnabledCheck.getSelection());
        store.setValue(DBeaverPreferences.AGENT_LONG_OPERATION_NOTIFY, longOperationsCheck.getSelection());
        store.setValue(DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT, longOperationsTimeout.getSelection());

        store.setValue(DBeaverPreferences.UI_KEEP_DATABASE_EDITORS, keepEditorsOnRestart.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_REFRESH_EDITORS_ON_OPEN, refreshEditorOnOpen.getSelection());
        store.setValue(DBeaverPreferences.NAVIGATOR_SYNC_EDITOR_DATASOURCE, syncEditorDataSourceWithNavigator.getSelection());
        store.setValue(DBeaverPreferences.DEFAULT_RESOURCE_ENCODING, defaultResourceEncoding.getText());

        PrefUtils.savePreferenceStore(store);

        return true;
    }

    @Nullable
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