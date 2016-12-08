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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

/**
 * Active status label
 */
abstract class ActiveStatusMessage extends Composite {

    private final ResultSetViewer viewer;
    private final Text messageText;

    public ActiveStatusMessage(@NotNull Composite parent, int style, @Nullable final ResultSetViewer viewer) {
        super(parent, SWT.BORDER);

        this.viewer = viewer;

        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        this.setLayout(layout);

        // Toolbar
        ToolBar tb = new ToolBar(this, SWT.HORIZONTAL);
        ToolItem ti = new ToolItem(tb, SWT.NONE);
        ti.setImage(DBeaverIcons.getImage(UIIcon.SQL_EXECUTE));

        messageText = new Text(this, SWT.READ_ONLY);
        if (RuntimeUtils.isPlatformWindows()) {
            messageText.setBackground(null);
        } else {
            messageText.setBackground(parent.getBackground());
        }
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        messageText.setLayoutData(gd);

        UIUtils.enableHostEditorKeyBindingsSupport(viewer.getSite(), this.messageText);
        UIUtils.addFocusTracker(viewer.getSite(), UIUtils.INLINE_WIDGET_EDITOR_ID, this.messageText);
        this.messageText.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                // Unregister from focus service
                UIUtils.removeFocusTracker(viewer.getSite(), messageText);
            }
        });
    }

    protected void showDetails() {
        StatusDetailsDialog dialog = new StatusDetailsDialog(
            viewer,
            getMessage(),
            viewer.getDataReceiver().getErrorList());
        dialog.open();
    }

    public void setStatus(String message)
    {
        if (messageText.isDisposed()) {
            return;
        }
        messageText.setText(message);
    }

    public String getMessage() {
        return messageText.getText();
    }

    protected abstract ILoadService createLoadService();

}
