/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

/**
 * StandardErrorDialog
 */
public class StandardErrorDialog extends ErrorDialog implements BlockingPopupDialog {

    private static final String DIALOG_ID = "DBeaver.StandardErrorDialog";//$NON-NLS-1$
    private Text messageText;
    private boolean detailsVisible = false;

    private static final int MAX_AUTO_SIZE_X = 500;
    private static final int MAX_AUTO_SIZE_Y = 300;

    public StandardErrorDialog(
        @NotNull Shell parentShell,
        @NotNull String dialogTitle,
        @Nullable String message,
        @NotNull IStatus status,
        int displayMask)
    {
        super(parentShell, dialogTitle, message, status, displayMask);
        setStatus(status);
        if (message == null) {
            IStatus rootStatus = GeneralUtils.getRootStatus(status);
            if (rootStatus.getException() != null) {
                String lastMessage = null;
                for (Throwable e = rootStatus.getException(); e != null; e = e.getCause()) {
                    if (e.getMessage() != null) {
                        lastMessage = e.getMessage();
                    }
                }
                if (CommonUtils.isEmpty(lastMessage)) {
                    lastMessage = rootStatus.getMessage();
                    if (CommonUtils.isEmpty(lastMessage)) {
                        lastMessage = status.getMessage();
                        if (CommonUtils.isEmpty(lastMessage)) {
                            // No message at all. This may happen in case of NPE and other messageless errors.
                            // Let's use exception name then
                            if (rootStatus.getException() != null) {
                                lastMessage = rootStatus.getException().getClass().getName();
                            }
                        }
                    }
                }
                this.message = CommonUtils.cutExtraLines(lastMessage, 20);
            } else {
                this.message = CommonUtils.cutExtraLines(rootStatus.getMessage(), 20);
            }
        } else {
            this.message = CommonUtils.cutExtraLines(JFaceResources.format("Reason", message, status.getMessage()), 20); //$NON-NLS-1$
        }
        // Truncate message to 64kb
        this.message = CommonUtils.truncateString(this.message, 64000);
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    protected Control createDialogArea(Composite parent) {
        return createMessageArea(parent);
    }

    protected Control createMessageArea(Composite composite) {
        // create composite
        // create image
        Image image = getImage();
        if (image != null) {
            imageLabel = new Label(composite, SWT.NULL);
            image.setBackground(imageLabel.getBackground());
            imageLabel.setImage(image);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.BEGINNING).applyTo(imageLabel);
        }
        // create message
        if (message != null) {
            messageText = new Text(composite, SWT.READ_ONLY | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
            messageText.setText(message);
            messageText.setFont(UIUtils.getMonospaceFont());
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.minimumWidth = IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH;
            gd.heightHint = UIUtils.getFontHeight(composite) * 10;
            gd.grabExcessVerticalSpace = true;
            gd.grabExcessHorizontalSpace = true;
            messageText.setLayoutData(gd);
        }
        return composite;
    }

    @Override
    public void create() {
        super.create();
        Point prefSize = getContents().computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Point actualSize = getShell().getSize();
        if ((prefSize.x < MAX_AUTO_SIZE_X && prefSize.x > actualSize.x) ||
            (prefSize.y < MAX_AUTO_SIZE_Y && prefSize.y > actualSize.y)) {
            if (prefSize.x > actualSize.x) {
                actualSize.x = prefSize.x;
            }
            if (prefSize.y > actualSize.y) {
                actualSize.y = prefSize.y;
            }
            getShell().setSize(actualSize);
        }
        detailsVisible = getDialogBoundsSettings().getBoolean("showDetails");
        if (detailsVisible) {
            showDetailsArea();
        }
        UIUtils.asyncExec(() -> {
            Button okButton = getButton(IDialogConstants.OK_ID);
            if (okButton != null) {
                okButton.setFocus();
            }
        });
    }

    @Override
    protected List createDropDownList(Composite parent) {
        detailsVisible = true;
        List dropDownList = super.createDropDownList(parent);
        dropDownList.setFont(UIUtils.getMonospaceFont());
        dropDownList.addDisposeListener(e -> {
            detailsVisible = false;
        });
        int itemCount = dropDownList.getItemCount();
        if (itemCount > 1 && dropDownList.getItem(itemCount - 2).equals(dropDownList.getItem(itemCount - 1))) {
            // Remove last list item (dup)
            dropDownList.remove(itemCount - 1);
        }
        return dropDownList;
    }

    public Image getErrorImage() {
        return DBeaverIcons.getImage(DBIcon.STATUS_ERROR);
    }

    public Image getWarningImage() {
        return DBeaverIcons.getImage(DBIcon.STATUS_WARNING);
    }

    public Image getInfoImage() {
        return DBeaverIcons.getImage(DBIcon.STATUS_INFO);
    }

    @Override
    public boolean close() {
        getDialogBoundsSettings().put("showDetails", detailsVisible);
        return super.close();
    }

}