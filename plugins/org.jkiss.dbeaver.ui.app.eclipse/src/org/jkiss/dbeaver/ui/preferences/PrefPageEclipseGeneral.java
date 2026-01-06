/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.jkiss.code.NotNull;
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

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
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
        UIUtils.createPreferenceLink(
            composite,
            "<a>''{0}''</a> " + CoreMessages.pref_page_ui_general_label_settings,
            pageID,
            (IWorkbenchPreferenceContainer) getContainer(), null
        );
    }
}
