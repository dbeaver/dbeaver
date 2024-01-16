/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.screenreaders.ScreenReader;
import org.jkiss.dbeaver.ui.screenreaders.ScreenReaderPreferences;

public class PrefPageAccessibility extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.user.interface.accessibility";
    private final DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
    private Combo cmbScreenReaderSupport;
    private IAdaptable element;

    @Override
    public void init(IWorkbench workbench) {
        // no implementation
    }

    @Override
    protected Control createPreferenceContent(Composite parent) {
        Composite composite = createControls(parent);
        initControls();
        return composite;
    }

    private Composite createControls(Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);
        Group accessibilityGroup = UIUtils.createControlGroup(
            composite,
            CoreMessages.pref_page_accessibility_screen_reader_group_lbl,
            2,
            GridData.GRAB_HORIZONTAL,
            0);
        cmbScreenReaderSupport = UIUtils.createLabelCombo(
            accessibilityGroup,
            CoreMessages.pref_page_accessibility_screen_reader_msg,
            CoreMessages.pref_page_accessibility_screen_reader_description,
            SWT.DROP_DOWN | SWT.READ_ONLY);
        return composite;
    }

    private void initControls() {
        for (ScreenReader reader : ScreenReader.values()) {
            cmbScreenReaderSupport.add(reader.getScreenReaderName());
        }
        String storedScreenReader = store.getString(ScreenReaderPreferences.PREF_SCREEN_READER_ACCESSIBILITY);
        ScreenReader screenReader = ScreenReader.getScreenReader(storedScreenReader);
        cmbScreenReaderSupport.select(screenReader.ordinal());
    }

    @Override
    public IAdaptable getElement() {
        return element;
    }

    @Override
    public void setElement(IAdaptable element) {
        this.element = element;
    }

    @Override
    protected void performDefaults() {
        cmbScreenReaderSupport.select(ScreenReader.DEFAULT.ordinal());
    }

    @Override
    public boolean performOk() {
        ScreenReader screenReader = ScreenReader.getScreenReader(cmbScreenReaderSupport.getText());
        store.setValue(ScreenReaderPreferences.PREF_SCREEN_READER_ACCESSIBILITY, screenReader.name());
        return true;
    }

}
