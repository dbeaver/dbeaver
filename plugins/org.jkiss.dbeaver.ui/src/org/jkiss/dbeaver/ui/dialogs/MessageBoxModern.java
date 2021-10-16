/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

final class MessageBoxModern extends Dialog {
    @Nullable
    private String title;
    @Nullable
    private String message;
    @Nullable
    private List<String> labels;
    private int defaultAnswerIdx;
    @Nullable
    private DBPImage primaryImage;

    @Nullable
    private List<Button> buttons;

    MessageBoxModern(@Nullable Shell parentShell) {
        super(parentShell);
    }

    void setTitle(@Nullable String title) {
        this.title = title;
    }

    void setMessage(@Nullable String message) {
        this.message = message;
    }

    void setPrimaryImage(@NotNull DBPImage primaryImage) {
        this.primaryImage = primaryImage;
    }

    void setLabels(@NotNull List<String> labels) {
        this.labels = labels;
    }

    void setDefaultAnswerIdx(int defaultAnswerIdx) {
        this.defaultAnswerIdx = defaultAnswerIdx;
    }

    // ----- jface.Dialog methods

    protected Control createContents(Composite parent) {
        initializeDialogUnits(parent);
        GridLayout gl = new GridLayout();
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        Point defaultSpacing = LayoutConstants.getSpacing();
        gl.horizontalSpacing = defaultSpacing.x * 2;
        gl.verticalSpacing = defaultSpacing.y;
        Point defaultMargins = LayoutConstants.getMargins();
        gl.marginWidth = defaultMargins.x;
        gl.marginHeight = defaultMargins.y;
        gl.numColumns = 2;
        parent.setLayout(gl);

        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        parent.setLayoutData(gd);

        dialogArea = createDialogArea(parent);
        buttonBar = createButtonBar(parent);
        applyDialogFont(parent);
        return parent;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        if (primaryImage != null) {
            Control imageLabel = UIUtils.createLabel(parent, primaryImage);
            GridData gd = new GridData();
            gd.minimumWidth = 1;
            gd.minimumHeight = 1;
            gd.horizontalAlignment = SWT.CENTER;
            gd.verticalAlignment = SWT.BEGINNING;
            imageLabel.setLayoutData(gd);
        }

        if (message != null) {
            Label messageLabel = new Label(parent, SWT.WRAP);
            messageLabel.setText(message);
            GridData gd = new GridData();
            gd.minimumWidth = 1;
            gd.minimumHeight = 1;
            gd.horizontalAlignment = SWT.FILL;
            gd.verticalAlignment = SWT.BEGINNING;
            gd.grabExcessHorizontalSpace = true;
            gd.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
            messageLabel.setLayoutData(gd);
        }

        // create the top level composite for the dialog area
        Composite composite = UIUtils.createComposite(parent,1);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.horizontalSpan = 2;
        composite.setLayoutData(data);

        /* TODO
            Here the users of this class can add a custom area below the message. I believe that passing a
            Function<? super Composite, ? extends Composite> through the builder is a good option. This is not implemented (for now).

            Composite customArea = null;
            //If custom area is null create a dummy label for spacing purposes
         */
        UIUtils.createEmptyLabel(composite, 2, 1);

        return composite;
    }

    @Override
    protected Control createButtonBar(@NotNull Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(0, true);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        composite.setLayout(gl);
        GridData gd = new GridData();
        gd.horizontalSpan = 2;
        gd.horizontalAlignment = SWT.END;
        composite.setLayoutData(gd);
        composite.setFont(parent.getFont());
        createButtonsForButtonBar(composite);
        return composite;
    }

    @Override
    protected void configureShell(@NotNull Shell shell) {
        super.configureShell(shell);
        shell.setText(title);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        if (labels == null) {
            return;
        }
        buttons = new ArrayList<>(labels.size());
        for (int i = 0; i < labels.size(); i++) {
            buttons.add(createButton(parent, i, labels.get(i), defaultAnswerIdx == i));
        }
    }

    @Nullable
    @Override
    protected Button getButton(int index) {
        if (buttons != null && CommonUtils.isValidIndex(index, buttons.size())) {
            return buttons.get(index);
        }
        return null;
    }

    @Override
    protected void buttonPressed(int buttonId) {
        setReturnCode(buttonId);
        close();
    }

    @Override
    protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
        Button button = super.createButton(parent, id, label, defaultButton);
        if (defaultButton) {
            button.setFocus();
        }
        return button;
    }

    @Override
    protected void handleShellCloseEvent() {
        super.handleShellCloseEvent();
        setReturnCode(SWT.DEFAULT);
    }
}
