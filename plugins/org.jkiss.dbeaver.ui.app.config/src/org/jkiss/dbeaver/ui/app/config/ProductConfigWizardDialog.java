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

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.util.Geometry;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPPlatformLanguage;
import org.jkiss.dbeaver.model.app.DBPPlatformLanguageManager;
import org.jkiss.dbeaver.registry.language.PlatformLanguageRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.forms.UIObservable;
import org.jkiss.dbeaver.ui.forms.UIPanelBuilder;

import java.util.function.Consumer;

public final class ProductConfigWizardDialog extends ActiveWizardDialog {
    private final UIObservable<DBPPlatformLanguage> language =
        UIObservable.of(DBPPlatformDesktop.getInstance().getPlatformLanguage(), DBPPlatformLanguage.class);
    private Control languagePicker;

    public ProductConfigWizardDialog(@NotNull IWorkbenchWindow window) {
        super(window, new ProductConfigWizard());

        // We're reusing the help system for language selection
        setHelpAvailable(true);

        language.addChangeListener((ignored, language) -> setLanguage(language));

        addPageChangedListener(event -> {
            boolean firstPageSelected = event.getSelectedPage() == getWizard().getStartingPage();
            UIUtils.setControlVisible(languagePicker, firstPageSelected);
        });
    }

    public boolean isRestartRequired() {
        return ((ProductConfigWizard) getWizard()).isRestartRequired();
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
    protected int getDialogBoundsStrategy() {
        return 0;
    }

    @NotNull
    @Override
    protected Point getInitialLocation(@NotNull Point initialSize) {
        // NOTE: This method is almost identical to its base implementation,
        // but it also checks whether [parent] is visible. This dialog
        // might appear before the main window is even visible, and centering
        // it around the main window would make it appear in the wrong place.

        Shell shell = getShell();
        Composite parent = shell.getParent();

        Monitor monitor = shell.getDisplay().getPrimaryMonitor();
        if (parent != null && parent.isVisible()) {
            monitor = parent.getMonitor();
        }

        Rectangle monitorBounds = monitor.getClientArea();
        Point centerPoint;
        if (parent != null && parent.isVisible()) {
            centerPoint = Geometry.centerPoint(parent.getBounds());
        } else {
            centerPoint = Geometry.centerPoint(monitorBounds);
        }

        return new Point(
            centerPoint.x - (initialSize.x / 2),
            Math.clamp(
                centerPoint.y - (initialSize.y * 2L / 3),
                monitorBounds.y, monitorBounds.y + monitorBounds.height - initialSize.y
            )
        );
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
        if (cancelButton != null && !Platform.inDevelopmentMode()) {
            UIUtils.setControlVisible(cancelButton, false);
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
        DBWorkbench.getPlatformUI().showMessageBox("Language change", "Change will take effect after restart", false);
    }
}
