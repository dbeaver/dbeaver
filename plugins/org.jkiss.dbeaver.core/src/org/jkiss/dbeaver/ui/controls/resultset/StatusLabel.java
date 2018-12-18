/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

/**
 * Status label
 */
class StatusLabel extends Composite {

    private final IResultSetController viewer;
    private final Label statusIcon;
    private final Text statusText;
    //private final Color colorDefault, colorError, colorWarning;
    private DBPMessageType messageType;

    public StatusLabel(@NotNull Composite parent, int style, @Nullable final IResultSetController viewer) {
        super(parent, SWT.BORDER);
        this.viewer = viewer;

        final GridLayout layout = new GridLayout(3, false);
        layout.marginHeight = 0;
        layout.marginWidth = 2;
        layout.horizontalSpacing = 3;
        setLayout(layout);

/*
        colorDefault = getForeground();
        colorError = JFaceColors.getErrorText(Display.getDefault());
        colorWarning = colorDefault;
*/

        final Image statusImage = JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_INFO);
        statusIcon = new Label(this, SWT.NONE);
        statusIcon.setImage(statusImage);
        statusIcon.setToolTipText("Status information");
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        statusIcon.setLayoutData(gd);

        statusText = new Text(this, SWT.SINGLE | SWT.READ_ONLY);
        if (RuntimeUtils.isPlatformWindows()) {
            statusText.setBackground(null);
        } else {
            statusText.setBackground(parent.getBackground());
        }
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.minimumHeight = statusImage.getBounds().height;
        statusText.setLayoutData(gd);

        if (viewer != null) {
            TextEditorUtils.enableHostEditorKeyBindingsSupport(viewer.getSite(), this.statusText);
            UIUtils.addDefaultEditActionsSupport(viewer.getSite(), this.statusText);
        }

        final ToolBar tb = new ToolBar(this, SWT.HORIZONTAL);
        final ToolItem detailsIcon = new ToolItem(tb, SWT.NONE);
        detailsIcon.setImage(DBeaverIcons.getImage(UIIcon.TEXTFIELD));
        tb.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        detailsIcon.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                showDetails();
            }
        });
        statusText.addTraverseListener(e -> {
            if (e.detail == SWT.TRAVERSE_RETURN) {
                showDetails();
            }
        });
    }

    protected void showDetails() {
        DBDDataReceiver dataReceiver = viewer.getDataReceiver();
        if (dataReceiver instanceof ResultSetDataReceiver) {
            StatusDetailsDialog dialog = new StatusDetailsDialog(
                viewer.getSite().getShell(),
                getMessage(),
                ((ResultSetDataReceiver) dataReceiver).getErrorList());
            dialog.open();
        }
    }

    public void setStatus(String message) {
        this.setStatus(message, DBPMessageType.INFORMATION);
    }

    public void setStatus(String message, DBPMessageType messageType)
    {
        if (statusText.isDisposed()) {
            return;
        }
        this.messageType = messageType;

        //Color fg;
        String statusIconId;
        switch (messageType) {
            case ERROR:
                //fg = colorError;
                statusIconId = Dialog.DLG_IMG_MESSAGE_ERROR;
                break;
            case WARNING:
                //fg = colorWarning;
                statusIconId = Dialog.DLG_IMG_MESSAGE_WARNING;
                break;
            default:
                //fg = null;
                statusIconId = Dialog.DLG_IMG_MESSAGE_INFO;
                break;
        }
        //statusText.setForeground(fg);

        if (message == null) {
            message = "???"; //$NON-NLS-1$
        }
        statusIcon.setImage(JFaceResources.getImage(statusIconId));
        statusText.setText(TextUtils.getSingleLineString(message));
        if (messageType != DBPMessageType.INFORMATION) {
            statusText.setToolTipText(message);
        } else {
            statusText.setToolTipText(null);
        }
    }

    public String getMessage() {
        return statusText.getText();
    }

    public DBPMessageType getMessageType() {
        return messageType;
    }

    public void setUpdateListener(Runnable runnable) {

    }
}
