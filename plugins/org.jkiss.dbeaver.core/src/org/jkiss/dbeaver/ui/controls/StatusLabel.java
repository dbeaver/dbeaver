/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;
import org.jkiss.dbeaver.utils.RuntimeUtils;

/**
 * Status label
 */
public class StatusLabel extends Composite {

    private final Label statusIcon;
    private final Text statusText;
    private final Color colorDefault, colorError, colorWarning;
    private DBPMessageType messageType;

    public StatusLabel(@NotNull Composite parent) {
        this(parent, null);
    }

    public StatusLabel(@NotNull Composite parent, @Nullable final IWorkbenchPartSite site) {
        super(parent, SWT.BORDER);
        setBackgroundMode(SWT.INHERIT_FORCE);

        GridLayout layout = new GridLayout(3, false);
        layout.marginHeight = 2;
        layout.marginWidth = 2;
        setLayout(layout);

        colorDefault = getForeground();
        colorError = JFaceColors.getErrorText(Display.getDefault());
        colorWarning = colorDefault;

        statusIcon = new Label(this, SWT.NONE);
        statusIcon.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_INFO));
        statusIcon.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        statusText = new Text(this, SWT.READ_ONLY);
        if (RuntimeUtils.isPlatformWindows()) {
            statusText.setBackground(null);
        } else {
            statusText.setBackground(parent.getBackground());
        }
        statusText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        if (site != null) {
            UIUtils.enableHostEditorKeyBindingsSupport(site, this.statusText);
            UIUtils.addFocusTracker(site, UIUtils.INLINE_WIDGET_EDITOR_ID, this.statusText);
            this.statusText.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e) {
                    // Unregister from focus service
                    UIUtils.removeFocusTracker(site, statusText);
                }
            });
        }

        Label detailsIcon = new Label(this, SWT.NONE);
        detailsIcon.setImage(DBeaverIcons.getImage(UIIcon.TEXTFIELD));
        detailsIcon.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        detailsIcon.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        detailsIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                showDetails();
            }
        });
        statusText.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    showDetails();
                }
            }
        });
    }

    protected void showDetails() {
        EditTextDialog.showText(getShell(), CoreMessages.controls_resultset_viewer_dialog_status_title, statusText.getText());
    }

    public void setStatus(String message, DBPMessageType messageType)
    {
        if (statusText.isDisposed()) {
            return;
        }
        this.messageType = messageType;

        Color fg;
        String statusIconId;
        switch (messageType) {
            case ERROR:
                fg = colorError;
                statusIconId = Dialog.DLG_IMG_MESSAGE_ERROR;
                break;
            case WARNING:
                fg = colorWarning;
                statusIconId = Dialog.DLG_IMG_MESSAGE_WARNING;
                break;
            default:
                fg = colorDefault;
                statusIconId = Dialog.DLG_IMG_MESSAGE_INFO;
                break;
        }
        statusText.setForeground(fg);
        if (message == null) {
            message = "???"; //$NON-NLS-1$
        }
        statusIcon.setImage(JFaceResources.getImage(statusIconId));
        statusText.setText(message);
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
}
