/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
public class BaseDialog extends Dialog
{

    private String title;
    private DBPImage icon;

    public BaseDialog(Shell parentShell, String title, @Nullable DBPImage icon)
    {
        super(parentShell);
        this.title = title;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DBPImage getImage() {
        return icon;
    }

    public void setImage(DBPImage image)
    {
        this.icon = image;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);
        applyDialogFont(dialogArea);
        return contents;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite dialogArea1 = (Composite) super.createDialogArea(parent);

        return dialogArea1;
    }

    @Override
    public void create()
    {
        super.create();
        if (title != null) {
            getShell().setText(title);
        }
        if (icon != null) {
            getShell().setImage(DBeaverIcons.getImage(icon));
        }

    }

    @Override
    protected Control createButtonBar(Composite parent) {
        final Composite composite = UIUtils.createPlaceholder(parent, 2, 0);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        final Composite leadingButtonsComposite = createButtonBarComposite(composite, SWT.LEAD);
        final Composite trailingButtonsComposite = createButtonBarComposite(composite, SWT.TRAIL);

        createButtonsForButtonBar(leadingButtonsComposite, SWT.LEAD);
        createButtonsForButtonBar(trailingButtonsComposite, SWT.TRAIL);

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

    protected void createButtonsForButtonBar(@NotNull Composite parent, int alignment) {
        if (alignment == SWT.TRAIL) {
            createButtonsForButtonBar(parent);
        }
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
        composite.setFont(parent.getFont());

        return composite;
    }

    // Overloaded just to add the @Nullable annotation
    @Override
    @Nullable
    protected Button getButton(int id) {
        return super.getButton(id);
    }
}
