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
package org.jkiss.dbeaver.ui.app.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPPlatformLanguage;
import org.jkiss.dbeaver.model.app.DBPPlatformLanguageManager;
import org.jkiss.dbeaver.registry.language.PlatformLanguageRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.app.config.nls.ProductConfigMessages;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.forms.UIObservable;
import org.jkiss.dbeaver.ui.forms.UIPanelBuilder;
import org.jkiss.dbeaver.ui.forms.util.UIReloadableNLS;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class ProductConfigWizardDialog extends ActiveWizardDialog {
    private final UIObservable<DBPPlatformLanguage> language =
        UIObservable.of(DBPPlatformDesktop.getInstance().getPlatformLanguage(), DBPPlatformLanguage.class);
    private Control languagePicker;

    public ProductConfigWizardDialog(@NotNull IWorkbenchWindow window) {
        super(window, new ProductConfigWizard());

        // We're reusing the help system for language selection
        setHelpAvailable(true);

        language.addChangeListener((ignored, language) -> {
            setLanguage(language);
            UIReloadableNLS.reloadMessages();
            getShell().layout(true, true);
        });

        addPageChangedListener(event -> {
            boolean firstPageSelected = event.getSelectedPage() == getWizard().getStartingPage();
            UIUtils.setControlVisible(languagePicker, firstPageSelected);
        });
    }

    @NotNull
    @Override
    protected Point getInitialSize() {
        return new Point(600, 450);
    }

    @Override
    public int getShellStyle() {
        return SWT.TITLE | SWT.BORDER | SWT.RESIZE;
    }

    @Override
    public void updateSize() {
        // don't update size - pages are adapted to the dialog size
    }

    @Override
    public void showPage(@Nullable IWizardPage page) {
        super.showPage(page);
        if (page instanceof WizardPage page1) {
            page1.setPageComplete(true);
        }
    }

    @NotNull
    @Override
    protected Control createHelpControl(@NotNull Composite parent) {
        ((GridLayout) parent.getLayout()).numColumns++;
        languagePicker = UIPanelBuilder.build(parent, buildLanguagePanel(language));
        languagePicker.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        return languagePicker;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        super.createButtonsForButtonBar(parent);

        var cancelButton = getButton(IDialogConstants.CANCEL_ID);
        if (cancelButton != null) {
            UIUtils.setControlVisible(cancelButton, false);
        }

        bindButtonText(IDialogConstants.BACK_ID, ProductConfigMessages.wizard_buttons_back);
        bindButtonText(IDialogConstants.NEXT_ID, ProductConfigMessages.wizard_buttons_next);
        bindButtonText(IDialogConstants.FINISH_ID, ProductConfigMessages.wizard_buttons_finish);
    }

    private void bindButtonText(int id, @NotNull UIObservable<String> text) {
        var button = getButton(id);
        if (button != null) {
            BiConsumer<String, String> listener = (s, s2) -> button.setText(s2);
            button.setText(text.get());
            button.addDisposeListener(e -> text.removeChangeListener(listener));
            text.addChangeListener(listener);
        }
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildLanguagePanel(@NotNull UIObservable<DBPPlatformLanguage> language) {
        return pb -> pb.row(rb -> rb.comboBox(
            PlatformLanguageRegistry.getInstance().getLanguages(),
            language,
            DBPPlatformLanguage::getLabel
        ));
    }

    private static void setLanguage(@NotNull DBPPlatformLanguage language) {
        if (DBWorkbench.getPlatform() instanceof DBPPlatformLanguageManager languageManager) {
            languageManager.setPlatformLanguage(language);
        }
        Locale.setDefault(Locale.of(language.getCode()));
    }
}
