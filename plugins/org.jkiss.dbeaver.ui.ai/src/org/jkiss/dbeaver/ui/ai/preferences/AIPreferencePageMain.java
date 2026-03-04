/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.ai.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

public class AIPreferencePageMain extends AbstractPrefPage implements IWorkbenchPreferencePage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.ai";

    @Override
    public void init(IWorkbench workbench) {

    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 1);

        Composite groupObjects = UIUtils.createComposite(
            composite,
            1);
        Label descLabel = new Label(groupObjects, SWT.WRAP);
        descLabel.setText("AI settings");

        // Link to secure storage config
        addLinkToSettings(groupObjects, AIPreferencePageEngines.PAGE_ID);
        addLinkToSettings(groupObjects, AIPreferencePageConfiguration.PAGE_ID);
        addLinkToSettings(groupObjects, AIPreferencePagePrompts.PAGE_ID);

        return composite;
    }

    private void addLinkToSettings(Composite composite, String pageID) {
        if (getContainer() instanceof IWorkbenchPreferenceContainer wpc) {
            UIUtils.createPreferenceLink(
                composite,
                "<a>''{0}''</a> " + CoreMessages.pref_page_ui_general_label_settings,
                pageID,
                wpc,
                null
            );
        }
    }

}
