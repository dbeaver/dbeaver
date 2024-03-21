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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIMessages;

/**
 * AbstractPrefPage
 */
public abstract class AbstractPrefPage extends PreferencePage {
    @Override
    protected Control createContents(Composite parent) {
        if (!hasAccessToPage()) {
            return UIUtils.createLabel(parent, UIMessages.preference_page_no_access);
        }
        final Control content = createPreferenceContent(parent);

        Dialog.applyDialogFont(content);

        return content;
    }

    void disableButtons() {
        noDefaultAndApplyButton();
    }

    @NotNull
    protected abstract Control createPreferenceContent(@NotNull Composite parent);

    protected boolean hasAccessToPage() {
        return true;
    }
}
