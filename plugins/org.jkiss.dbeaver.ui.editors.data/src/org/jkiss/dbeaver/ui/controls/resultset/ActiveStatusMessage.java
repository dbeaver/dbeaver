/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.swt.SWT;
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
import org.jkiss.dbeaver.ui.css.CSSUtils;
import org.jkiss.dbeaver.ui.css.DBStyles;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

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
        super(parent, SWT.NONE);

        this.viewer = viewer;
        this.actionImage = actionImage;

        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = 1;
        this.setLayout(layout);

        // Toolbar
        ToolBar tb = new ToolBar(this, SWT.FLAT | SWT.HORIZONTAL);
        CSSUtils.setCSSClass(tb, DBStyles.COLORED_BY_CONNECTION_TYPE);
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

        if (viewer != null) {
            TextEditorUtils.enableHostEditorKeyBindingsSupport(viewer.getSite(), this.messageText);
            UIUtils.addDefaultEditActionsSupport(viewer.getSite(), this.messageText);
        }
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
        if (!actionItem.isDisposed()) {
            actionItem.setEnabled(isActionEnabled());
        }
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
            if (!CommonUtils.equalObjects(getMessage(), message)) {
                setMessage(message);
                getParent().layout(true, true);
            }
            actionItem.setImage(actionImage);
            loadService = null;
        }
    }
}
