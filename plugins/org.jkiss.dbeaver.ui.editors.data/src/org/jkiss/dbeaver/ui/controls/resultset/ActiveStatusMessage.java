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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
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
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * Active status label
 */
abstract class ActiveStatusMessage extends Composite {
    private static final Log log = Log.getLog(ActiveStatusMessage.class);

    private final Image actionImage;
    private final Text messageText;
    private final ToolItem actionItem;

    private ILoadService<String> loadService;

    public ActiveStatusMessage(
        @NotNull Composite parent,
        @Nullable Image actionImage,
        @NotNull String actionText,
        @Nullable final ResultSetViewer viewer
    ) {
        super(parent, SWT.NONE);
        CSSUtils.markConnectionTypeColor(this);
        this.actionImage = actionImage;

        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = 1;
        this.setLayout(layout);

        // Toolbar
        ToolBar tb = new ToolBar(this, SWT.FLAT | SWT.HORIZONTAL);
        actionItem = new ToolItem(tb, SWT.PUSH);
        actionItem.setImage(actionImage);
        actionItem.setToolTipText(actionText);
        actionItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> executeAction(true)));

        messageText = new Text(this, SWT.READ_ONLY);
        if (RuntimeUtils.isWindows()) {
            messageText.setBackground(null);
        } else {
            messageText.setBackground(parent.getBackground());
        }
        messageText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        if (viewer != null) {
            TextEditorUtils.enableHostEditorKeyBindingsSupport(viewer.getSite(), this.messageText);
            UIUtils.addDefaultEditActionsSupport(viewer.getSite(), this.messageText);
        }
    }

    public void setMessage(@NotNull String message) {
        if (messageText.isDisposed()) {
            return;
        }
        messageText.setText(message);
    }

    @NotNull
    public String getMessage() {
        return messageText.getText();
    }

    public void updateActionState() {
        if (!actionItem.isDisposed()) {
            actionItem.setEnabled(isActionEnabled());
        }
    }

    public void executeAction(boolean showErrors) {
        if (loadService != null) {
            try {
                loadService.cancel();
            } catch (InvocationTargetException e) {
                log.debug(e.getTargetException());
            }
            loadService = null;
        }
        loadService = createLoadService();
        LoadingJob<String> service = LoadingJob.createService(
            loadService,
            new LoadVisualizer()
        );
        service.setShowErrors(showErrors);
        service.schedule();
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
            return completed || isDisposed();
        }

        @Override
        public void visualizeLoading() {
            actionItem.setImage(DBeaverIcons.getImage(UIIcon.CLOSE));
        }

        @Override
        public void completeLoading(String message) {
            completed = true;
            if (!messageText.isDisposed() && !CommonUtils.isEmpty(message) && !CommonUtils.equalObjects(getMessage(), message)) {
                actionItem.setImage(actionImage);
                setMessage(message);
                getParent().layout(true, true);
            }
            loadService = null;
        }
    }
}
