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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * Active status label
 */
abstract class ActiveStatusMessage extends Composite {

    private static final Log log = Log.getLog(ActiveStatusMessage.class);

    private final ResultSetViewer viewer;
    private final Image actionImage;
    private final Text messageText;
    private final ToolItem actionItem;

    private ILoadService<String> loadService;

    public ActiveStatusMessage(@NotNull Composite parent, Image actionImage, String actionText, @Nullable final ResultSetViewer viewer) {
        super(parent, SWT.BORDER);

        this.viewer = viewer;
        this.actionImage = actionImage;

        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = 1;
        this.setLayout(layout);

        // Toolbar
        ToolBar tb = new ToolBar(this, SWT.HORIZONTAL);
        actionItem = new ToolItem(tb, SWT.NONE);
        actionItem.setImage(this.actionImage);
        if (actionText != null) {
            actionItem.setToolTipText(actionText);
        }
        actionItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executeAction();
            }
        });

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

    public void setMessage(String message)
    {
        if (messageText.isDisposed()) {
            return;
        }
        messageText.setText(message);
    }

    public String getMessage() {
        return messageText.getText();
    }

    public void updateActionState() {
        actionItem.setEnabled(isActionEnabled());
    }

    public void executeAction() {
        if (loadService != null) {
            try {
                loadService.cancel();
            } catch (InvocationTargetException e) {
                log.error(e.getTargetException());
            }
            loadService = null;
        } else {
            loadService = createLoadService();
            LoadingJob.createService(
                loadService,
                new LoadVisualizer()).schedule();
        }
    }

    protected abstract boolean isActionEnabled();
    protected abstract ILoadService<String> createLoadService();

    private class LoadVisualizer implements ILoadVisualizer<String> {
        private boolean completed;
        @Override
        public DBRProgressMonitor overwriteMonitor(DBRProgressMonitor monitor) {
            return monitor;
        }

        @Override
        public boolean isCompleted() {
            return completed || ActiveStatusMessage.this.isDisposed();
        }

        @Override
        public void visualizeLoading() {
            actionItem.setImage(DBeaverIcons.getImage(UIIcon.CLOSE));
        }

        @Override
        public void completeLoading(String message) {
            completed = true;
            setMessage(message);
            actionItem.setImage(actionImage);
            loadService = null;
        }
    }
}
