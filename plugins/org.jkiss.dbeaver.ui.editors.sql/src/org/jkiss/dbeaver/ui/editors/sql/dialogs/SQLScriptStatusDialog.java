/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.dialogs;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.querylog.QueryLogViewer;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

import java.util.Collection;

public abstract class SQLScriptStatusDialog<T extends DBSObject> extends BaseDialog implements SQLScriptProgressListener<T> {

    private static final String DIALOG_ID = "SQLScriptStatusDialog";

    private Tree objectTree;
    private ProgressBar progressBar;
    private Label finishLabel;

    private Job job;
    private Collection<T> objects;
    private int processedCount;

    protected SQLScriptStatusDialog(String title, @Nullable DBPImage image)
    {
        super(UIUtils.getActiveWorkbenchShell(), title, image);
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = super.createDialogArea(parent);
        objectTree = new Tree(composite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 500;
        gd.heightHint = 200;
        objectTree.setLayoutData(gd);
        objectTree.setHeaderVisible(true);
        objectTree.setLinesVisible(true);

        TreeColumn nameColumn = new TreeColumn(objectTree, SWT.NONE);
        nameColumn.setText("Object");
        createStatusColumns(objectTree);
        for (T object : objects) {
            TreeItem item = new TreeItem(objectTree, SWT.NONE);
            item.setData(object);
            item.setText(0, DBUtils.getObjectFullName(object, DBPEvaluationContext.UI));
        }

        Composite progressPanel = UIUtils.createPlaceholder(composite, 2, 5);
        progressPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        progressBar = new ProgressBar(progressPanel, SWT.HORIZONTAL);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        progressBar.setLayoutData(gd);
        progressBar.setMinimum(0);
        progressBar.setMaximum(this.objects.size());
        final Button stopButton = UIUtils.createPushButton(
            progressPanel,
            null,
            UIUtils.getShardImage(ISharedImages.IMG_ELCL_STOP));
        stopButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                job.cancel();
                stopButton.setEnabled(false);
            }
        });

        finishLabel = new Label(composite, SWT.NONE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.exclude = true;
        finishLabel.setLayoutData(gd);
        finishLabel.setText("Finished");

        UIUtils.asyncExec(new Runnable() {
            @Override
            public void run() {
                UIUtils.packColumns(objectTree, false, null);
            }
        });

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button button = createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, true);
        button.setEnabled(false);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.CLOSE_ID) {
            okPressed();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    protected TreeItem getTreeItem(T object) {
        return UIUtils.getTreeItem(objectTree, object);
    }

    protected void createStatusColumns(Tree objectTree) {

    }

    @Override
    public void beginScriptProcessing(Job job, Collection<T> objects) {
        this.job =  job;
        this.objects = objects;
        this.open();
    }

    @Override
    public void endScriptProcessing() {
        if (getShell().isDisposed()) {
            return;
        }
        getButton(IDialogConstants.CLOSE_ID).setEnabled(true);

        Composite progressPanel = progressBar.getParent();
        progressPanel.setVisible(false);
        ((GridData)progressPanel.getLayoutData()).exclude = true;

        finishLabel.setVisible(true);
        ((GridData)finishLabel.getLayoutData()).exclude = false;
        finishLabel.setText("Finished - " + processedCount + " object(s) processed");

        progressPanel.getParent().layout();
    }

    @Override
    public void beginObjectProcessing(@NotNull T object, int objectNumber) {
        progressBar.setSelection(objectNumber + 1);
        TreeItem item = getTreeItem(object);
        if (item != null) {
            objectTree.setSelection(item);
        }
        processedCount++;
    }

    @Override
    public void endObjectProcessing(@NotNull T object, Exception error) {
        UIUtils.packColumns(objectTree, false, null);
        TreeItem treeItem = getTreeItem(object);
        if (treeItem != null) {
            treeItem.setText(1, error == null ? "Done" : error.getMessage());
            if (error != null) {
                ColorRegistry colorRegistry = UIUtils.getActiveWorkbenchWindow().getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
                Color colorError = colorRegistry.get(QueryLogViewer.COLOR_REVERTED);
                treeItem.setForeground(1, colorError);
            }
        }
    }

    @Override
    public void processObjectResults(@NotNull T object, @Nullable DBCStatement statement, @Nullable DBCResultSet resultSet) throws DBCException {

    }


}