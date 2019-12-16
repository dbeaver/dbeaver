/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.postgresql.ui;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.dbeaver.ui.preferences.PreferenceStoreDelegate;

/**
 * PrefPagePostgreSQL
 */
public class PrefPagePostgreSQL extends AbstractPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.postgresql.general"; //$NON-NLS-1$

    private Button showNonDefault;
    private Button showTemplates;

    public PrefPagePostgreSQL()
    {
        super();
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite cfgGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        cfgGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        cfgGroup.setLayoutData(gd);

        {
            Group secureGroup = new Group(cfgGroup, SWT.NONE);
            secureGroup.setText(PostgreMessages.dialog_setting_connection_settings);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 4;
            secureGroup.setLayoutData(gd);
            secureGroup.setLayout(new GridLayout(2, false));

            showNonDefault = UIUtils.createCheckbox(secureGroup,
                PostgreMessages.dialog_setting_connection_nondefaultDatabase,
                PostgreMessages.dialog_setting_connection_nondefaultDatabase_tip,
                getPreferenceStore().getBoolean(PostgreConstants.PROP_SHOW_NON_DEFAULT_DB),
                2);
            showNonDefault.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    showTemplates.setEnabled(showNonDefault.getSelection());
                }
            });
            showTemplates = UIUtils.createCheckbox(secureGroup,
                PostgreMessages.dialog_setting_connection_show_templates,
                PostgreMessages.dialog_setting_connection_show_templates_tip,
                getPreferenceStore().getBoolean(PostgreConstants.PROP_SHOW_TEMPLATES_DB),
                2);
        }


        setControl(cfgGroup);

        return cfgGroup;
    }

    @Override
    public boolean performOk() {
        IPreferenceStore preferenceStore = getPreferenceStore();
        preferenceStore.setValue(PostgreConstants.PROP_SHOW_NON_DEFAULT_DB, String.valueOf(showNonDefault.getSelection()));
        preferenceStore.setValue(PostgreConstants.PROP_SHOW_TEMPLATES_DB, String.valueOf(showTemplates.getSelection()));
        return super.performOk();
    }
}
