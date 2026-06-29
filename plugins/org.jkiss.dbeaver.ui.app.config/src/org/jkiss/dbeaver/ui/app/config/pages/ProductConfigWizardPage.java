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
package org.jkiss.dbeaver.ui.app.config.pages;

import org.eclipse.jface.wizard.WizardPage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.app.config.ProductConfigWizard;
import org.jkiss.dbeaver.ui.forms.UIObservable;

import java.util.function.BiConsumer;

public abstract class ProductConfigWizardPage extends WizardPage {
    private final UIObservable<String> title;
    private final UIObservable<String> description;

    private final BiConsumer<String, String> titleChangeListener = (s, s2) -> setTitle(s2);
    private final BiConsumer<String, String> descriptionChangeListener = (s, s2) -> setDescription(s2);

    public ProductConfigWizardPage(@NotNull UIObservable<String> title, @NotNull UIObservable<String> description) {
        super(title.get());
        setTitle(title.get());
        setDescription(description.get());
        setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.DBEAVER_LOGO));
        setPageComplete(false); // set to true by EasyConfigWizardDialog#showPage

        this.title = title;
        this.description = description;

        title.addChangeListener(titleChangeListener);
        description.addChangeListener(descriptionChangeListener);
    }

    @Override
    public void dispose() {
        super.dispose();

        // This is required because bindings may come from a message bundle where they live forever,
        // so listeners must be removed manually to avoid memory leaks.
        title.removeChangeListener(titleChangeListener);
        description.removeChangeListener(descriptionChangeListener);
    }

    @NotNull
    @Override
    public ProductConfigWizard getWizard() {
        return (ProductConfigWizard) super.getWizard();
    }

    /**
     * Determines whether this page should be shown in the current context.
     *
     * @return {@code true} if the page is applicable and should be shown, {@code false} otherwise
     */
    public boolean isPageApplicable() {
        return true;
    }

    public void loadSettings() {
        // do nothing by default
    }

    public void applySettings() {
        // do nothing by default
    }
}
