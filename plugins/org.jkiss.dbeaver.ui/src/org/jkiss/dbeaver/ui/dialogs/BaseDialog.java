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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Base dialog with title and image
 */
public class BaseDialog extends Dialog {

    private String title;
    private DBPImage icon;

    public BaseDialog(@Nullable Shell parentShell, @Nullable String title, @Nullable DBPImage icon) {
        super(parentShell);
        this.title = title;
        this.icon = icon;
    }

    @NotNull
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Nullable
    public DBPImage getImage() {
        return icon;
    }

    public void setImage(@NotNull DBPImage image)
    {
        this.icon = image;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createContents(@NotNull Composite parent) {
        Control contents = super.createContents(parent);
        applyDialogFont(dialogArea);
        return contents;
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        dialogArea = createDialogPanelWithMargins(parent, false);
        return (Composite) dialogArea;
    }

    @NotNull
    protected Composite createDialogPanelWithMargins(@NotNull Composite parent, boolean compact) {
        GridLayout layout = new GridLayout();
        layout.marginHeight = convertVerticalDLUsToPixels(compact ? IDialogConstants.VERTICAL_MARGIN / 2 : IDialogConstants.VERTICAL_MARGIN);
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        layout.verticalSpacing = convertVerticalDLUsToPixels(compact ? IDialogConstants.VERTICAL_SPACING / 2 : IDialogConstants.VERTICAL_SPACING);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        Composite composite = WidgetFactory.composite(SWT.NONE).layout(layout)
            .layoutData(new GridData(GridData.FILL_BOTH)).create(parent);
        applyDialogFont(composite);
        return composite;
    }

    @Nullable
    protected Composite getDialogArea() {
        return (Composite) dialogArea;
    }

    @Override
    public void create() {
        super.create();
        if (title != null) {
            getShell().setText(title);
        }
        if (icon != null) {
            getShell().setImage(DBeaverIcons.getImage(icon));
        }

    }

    @NotNull
    @Override
    protected Control createButtonBar(@NotNull Composite parent) {
        final Composite composite = UIUtils.createPlaceholder(parent, 2, 0);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        final Composite leadingButtonsComposite = createButtonBarComposite(composite, SWT.LEAD);
        final Composite trailingButtonsComposite = createButtonBarComposite(composite, SWT.TRAIL);

        createButtonsForLeftButtonBar(leadingButtonsComposite);
        createButtonsForButtonBar(trailingButtonsComposite);

        if (leadingButtonsComposite.getChildren().length == 0) {
            ((GridLayout) composite.getLayout()).numColumns -= 1;
            leadingButtonsComposite.dispose();
        }

        if (trailingButtonsComposite.getChildren().length == 0) {
            ((GridLayout) composite.getLayout()).numColumns -= 1;
            trailingButtonsComposite.dispose();
        }

        return composite;
    }

    protected void createButtonsForLeftButtonBar(@NotNull Composite parent) {

    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        super.createButtonsForButtonBar(parent);
    }

    @NotNull
    protected Composite createButtonBarComposite(@NotNull Composite parent, int alignment) {
        final GridLayout layout = new GridLayout(0, true);
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);

        final GridData data = new GridData(alignment, SWT.CENTER, true, false);

        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(layout);
        composite.setLayoutData(data);
        //composite.setFont(parent.getFont());

        return composite;
    }

    // Overloaded just to add the @Nullable annotation
    @Override
    @Nullable
    protected Button getButton(int id) {
        return super.getButton(id);
    }

    protected void enableButton(int buttonID, boolean enabled) {
        Button button = getButton(buttonID);
        if (button != null) {
            button.setEnabled(enabled);
        }
    }

}
