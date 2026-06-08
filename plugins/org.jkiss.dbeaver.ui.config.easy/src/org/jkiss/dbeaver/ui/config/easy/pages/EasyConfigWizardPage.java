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
package org.jkiss.dbeaver.ui.config.easy.pages;

import org.eclipse.jface.wizard.WizardPage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.forms.UIObservable;

public abstract class EasyConfigWizardPage extends WizardPage {
    public EasyConfigWizardPage(@NotNull String title, @NotNull String description) {
        super(title);
        setTitle(title);
        setDescription(description);
        setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.DBEAVER_LOGO));
    }

    public EasyConfigWizardPage(@NotNull UIObservable<String> title, @NotNull UIObservable<String> description) {
        this(title.get(), description.get());
        title.addChangeListener((s, s2) -> setTitle(s2));
        description.addChangeListener((s, s2) -> setDescription(s2));
    }
}
