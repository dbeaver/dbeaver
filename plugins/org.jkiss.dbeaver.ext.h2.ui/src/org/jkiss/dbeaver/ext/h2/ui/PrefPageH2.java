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
package org.jkiss.dbeaver.ext.h2.ui;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.h2.ui.internal.H2Messages;
import org.jkiss.dbeaver.ext.h2.util.H2Utils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.List;
import java.util.function.Predicate;

public final class PrefPageH2 extends AbstractPrefPage implements IWorkbenchPreferencePage {
    private List<String> actualAllowedClasses;
    private List<String> currentAllowedClasses;

    private Text allowedClassesText;
    private Control restartHint;

    @Override
    public void init(@NotNull IWorkbench workbench) {
        actualAllowedClasses = H2Utils.getSystemAllowedClasses();
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        var host = UIUtils.createComposite(parent, 1);

        var group = UIUtils.createTitledComposite(host, H2Messages.pref_security_title, 1, GridData.FILL_BOTH);
        UIUtils.createControlLabel(group, H2Messages.pref_security_allowed_classes_label);
        allowedClassesText = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);

        allowedClassesText.setText(String.join("\n", H2Utils.getUserAllowedClasses()));
        allowedClassesText.addModifyListener(e -> {
            currentAllowedClasses = parseClasses(allowedClassesText.getText());
            UIUtils.setControlVisible(restartHint, !currentAllowedClasses.equals(actualAllowedClasses));
        });
        GridDataFactory.fillDefaults()
            .grab(true, false)
            .hint(SWT.DEFAULT, allowedClassesText.getLineHeight() * 10)
            .applyTo(allowedClassesText);

        UIUtils.createInfoLabel(group, H2Messages.pref_security_allowed_classes_hint);

        restartHint = UIUtils.createWarningLabel(group, H2Messages.pref_unsaved_changes_hint, SWT.NONE, 1);
        UIUtils.setControlVisible(restartHint, false);

        return host;
    }

    @Override
    protected void performDefaults() {
        H2Utils.resetUserAllowedClasses();
        allowedClassesText.setText(String.join("\n", H2Utils.getUserAllowedClasses()));
        allowedClassesText.notifyListeners(SWT.Modify, new Event());

        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        H2Utils.setUserAllowedClasses(currentAllowedClasses);

        if (!currentAllowedClasses.equals(actualAllowedClasses)) {
            if (UIUtils.confirmAction(
                getShell(),
                NLS.bind(H2Messages.pref_restart_title, GeneralUtils.getProductName()),
                NLS.bind(H2Messages.pref_restart_message, GeneralUtils.getProductName())
            )) {
                restartWorkbenchOnPrefChange();
            }
        }

        return super.performOk();
    }

    @NotNull
    private static List<String> parseClasses(@NotNull String text) {
        return text.lines()
            .map(String::strip)
            .filter(Predicate.not(String::isEmpty))
            .toList();
    }
}
