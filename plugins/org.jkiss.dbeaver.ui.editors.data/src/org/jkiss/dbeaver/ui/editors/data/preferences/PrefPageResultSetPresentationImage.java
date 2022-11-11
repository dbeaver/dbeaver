/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.data.preferences;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.editors.data.internal.DataEditorsMessages;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * PrefPageResultSetPlainText
 */
public class PrefPageResultSetPresentationImage extends TargetPrefPage {
    private static final Log log = Log.getLog(PrefPageResultSetPresentationImage.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.resultset.image"; //$NON-NLS-1$
    private Button useBrowserCheckbox;


    public PrefPageResultSetPresentationImage() {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor) {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return store.contains(ResultSetPreferences.RESULT_IMAGE_BROWSER);
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions() {
        return true;
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 2, 5);

        {
            Group uiGroup = UIUtils.createControlGroup(
                composite,
                DataEditorsMessages.pref_page_database_resultsets_group_image,
                2,
                GridData.VERTICAL_ALIGN_BEGINNING,
                0
            );
            useBrowserCheckbox = UIUtils.createCheckbox(uiGroup,
                                                     DataEditorsMessages.pref_page_database_resultsets_label_image_browser,
                                                     false
            );
            useBrowserCheckbox.setToolTipText(DataEditorsMessages.pref_page_database_resultsets_label_image_browser_tip);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store) {
        try {
            useBrowserCheckbox.setSelection(store.getBoolean(ResultSetPreferences.RESULT_IMAGE_BROWSER));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store) {
        try {
            store.setValue(ResultSetPreferences.RESULT_IMAGE_BROWSER, useBrowserCheckbox.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store) {
        store.setToDefault(ResultSetPreferences.RESULT_IMAGE_BROWSER);
    }

    @Override
    protected String getPropertyPageID() {
        return PAGE_ID;
    }

}