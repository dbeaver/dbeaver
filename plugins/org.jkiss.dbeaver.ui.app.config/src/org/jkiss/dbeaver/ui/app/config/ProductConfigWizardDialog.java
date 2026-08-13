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
import org.eclipse.jface.util.Geometry;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPPlatformLanguage;
import org.jkiss.dbeaver.model.app.DBPPlatformLanguageManager;
import org.jkiss.dbeaver.model.config.ProductConfigFeatureDescriptor;
import org.jkiss.dbeaver.model.config.ProductConfigRegistry;
import org.jkiss.dbeaver.registry.language.PlatformLanguageRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.forms.UIObservable;
import org.jkiss.dbeaver.ui.forms.UIPanelBuilder;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class ProductConfigWizardDialog extends ActiveWizardDialog {
    private boolean seenLanguageChangeWarning = false;

    public ProductConfigWizardDialog(@NotNull IWorkbenchWindow window, @NotNull ProductConfigWizard.Origin origin) {
        super(
            window,
            new ProductConfigWizard(origin),
            null,
            origin == ProductConfigWizard.Origin.AUTOMATIC ? null : window.getShell()
        );
        setFinishButtonLabel("Apply");
        setMinimumPageSize(0, 0);
    }

    public boolean isRestartRequired() {
        return getWizard().isRestartRequired();
    }

    @NotNull
    public ProductConfigWizard.Origin getOrigin() {
        return getWizard().getOrigin();
    }

    @Override
    public int open() {
        beforeOpen();
        int result = super.open();
        afterClose(result);
        return result;
    }

    private void beforeOpen() {
        ProductConfigFeatures.WIZARD_SHOWN.use(Map.of(
            "origin", getOrigin()
        ));
    }

    private void afterClose(int result) {
        var registry = ProductConfigRegistry.getInstance();
        var features = registry.getFeatures().stream()
            .collect(Collectors.toMap(
                ProductConfigFeatureDescriptor::getId,
                registry::getFeatureEnablement
            ));

        ProductConfigFeatures.WIZARD_CLOSED.use(Map.of(
            "origin", getOrigin(),
            "status", result == IDialogConstants.OK_ID ? "finished" : "canceled",
            "features", features
        ));
    }

    @NotNull
    @Override
    protected ProductConfigWizard getWizard() {
        return (ProductConfigWizard) super.getWizard();
    }

    @Override
    protected void configureShell(@NotNull Shell newShell) {
        super.configureShell(newShell);
        newShell.setImages(Window.getDefaultImages());
    }

    @Override
    public int getShellStyle() {
        return SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.RESIZE;
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

    @NotNull
    @Override
    protected Point getInitialSize() {
        return new Point(600, 500);
    }

    @Override
    public void updateSize() {
        // don't update size - pages are adapted to the dialog size
    }

    @Override
    public boolean isHelpAvailable() {
        // Language change only supported when the wizard appears before the application is fully initialized.
        return DBWorkbench.getPlatform() instanceof DBPPlatformLanguageManager;
    }

    @NotNull
    @Override
    protected Control createHelpControl(@NotNull Composite parent) {
        ((GridLayout) parent.getLayout()).numColumns++;

        var language = UIObservable.of(
            DBPPlatformDesktop.getInstance().getPlatformLanguage(),
            DBPPlatformLanguage.class
        );
        language.addChangeListener((o, newLanguage) -> {
            if (DBWorkbench.getPlatform() instanceof DBPPlatformLanguageManager manager) {
                manager.setPlatformLanguage(newLanguage);
            }
            if (!seenLanguageChangeWarning) {
                seenLanguageChangeWarning = true;
                UIUtils.showMessageBox(
                    getShell(),
                    "Language change",
                    "Language change will be applied after restart.",
                    SWT.ICON_INFORMATION
                );
            }
        });

        var control = UIPanelBuilder.build(parent, buildLanguagePanel(language));
        control.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        addPageChangedListener(event -> {
            boolean firstPageSelected = event.getSelectedPage() == getWizard().getStartingPage();
            UIUtils.setControlVisible(control, firstPageSelected);
        });

        return control;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        super.createButtonsForButtonBar(parent);

        var cancelButton = getButton(IDialogConstants.CANCEL_ID);
        if (cancelButton != null && getOrigin() == ProductConfigWizard.Origin.AUTOMATIC) {
            UIUtils.setControlVisible(cancelButton, false);
            parent.layout(true, true);
        }
    }

    @Override
    public void updateButtons() {
        super.updateButtons();

        Button finishButton = getButton(IDialogConstants.FINISH_ID);
        if (finishButton != null && !finishButton.isDisposed() && finishButton.isEnabled()) {
            getShell().setDefaultButton(finishButton);
        }
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildLanguagePanel(@NotNull UIObservable<DBPPlatformLanguage> language) {
        return pb -> pb
            .row(rb -> rb.comboBox(
                PlatformLanguageRegistry.getInstance().getLanguages(),
                language,
                DBPPlatformLanguage::getLabel
            ));
    }
}
