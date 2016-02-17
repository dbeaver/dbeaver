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
package org.jkiss.dbeaver.ui.dialogs.sql;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

import java.util.Collection;

public abstract class SQLScriptStatusDialog<T extends DBSObject> extends BaseDialog implements SQLScriptProgressListener<T> {

    private Tree objectTree;
    private ProgressBar progressBar;
    private Label finishLabel;

    private Job job;
    private Collection<T> objects;
    private int processedCount;

    public SQLScriptStatusDialog(final Shell shell, String title, @Nullable DBPImage image)
    {
        super(shell, title, image);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = super.createDialogArea(parent);
        objectTree = new Tree(composite, SWT.BORDER | SWT.SINGLE);
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
            item.setText(0, DBUtils.getObjectFullName(object));
        }
        UIUtils.packColumns(objectTree, true, null);

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
            PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_STOP));
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
    public void beginObjectProcessing(T object, int objectNumber) {
        progressBar.setSelection(objectNumber + 1);
        TreeItem item = getTreeItem(object);
        if (item != null) {
            objectTree.setSelection(item);
        }
        processedCount++;
    }

    @Override
    public void endObjectProcessing(T object, Exception error) {
        UIUtils.packColumns(objectTree, true, null);
    }

    @Override
    public void processObjectResults(T object, DBCResultSet resultSet) throws DBCException {

    }


}