/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.struct;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IProgressControlProvider;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.itemlist.ItemListControl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * EditForeignKeyDialog
 *
 * @author Serge Rider
 */
public class EditForeignKeyDialog extends Dialog {

    private String title;
    private IProgressControlProvider progressProvider;
    private DBNDatabaseNode ownerTableNode;
    private DBNDatabaseNode refTableNode;

    public EditForeignKeyDialog(
        Shell shell,
        String title,
        IProgressControlProvider progressProvider,
        DBSTable table) {
        super(shell);
        setShellStyle(SWT.APPLICATION_MODAL | SWT.SHELL_TRIM);
        this.title = title;
        this.progressProvider = progressProvider;
        this.ownerTableNode = DBeaverCore.getInstance().getNavigatorModel().findNode(table);
        Assert.isNotNull(this.ownerTableNode);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        final Composite panel = UIUtils.createPlaceholder(dialogGroup, 1, 5);
        panel.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            final Composite tableGroup = UIUtils.createPlaceholder(panel, 2);
            tableGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            UIUtils.createLabelText(tableGroup, "Table", ownerTableNode.getNodeName(), SWT.READ_ONLY);

            createContentsBeforeColumns(tableGroup);
        }

        {
            DBNNode rootNode = ownerTableNode.getParentNode();

            //Composite columnsGroup = UIUtils.createControlGroup(panel, "Reference Table", 1, GridData.FILL_BOTH, 0);
            UIUtils.createControlLabel(panel, "Reference table");
            ItemListControl tableList = new ItemListControl(panel, SWT.SINGLE | SWT.SHEET | SWT.BORDER, null, rootNode, null);
            ProgressPageControl progressControl = null;
            if (progressProvider != null) {
                tableList.substituteProgressPanel(progressProvider.getProgressControl());
            } else {
                tableList.createProgressPanel();
            }

            tableList.loadData();
            final GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 500;
            gd.heightHint = 150;
            tableList.setLayoutData(gd);
            tableList.getSelectionProvider().addSelectionChangedListener(new ISelectionChangedListener() {
                public void selectionChanged(SelectionChangedEvent event)
                {
                    handleItemSelect(event.getSelection());
                }
            });
        }
        createContentsAfterColumns(panel);

        // Load columns
        final List<DBNDatabaseNode> columnNodes = new ArrayList<DBNDatabaseNode>();
        try {
            DBeaverCore.getInstance().runInProgressService(new DBRRunnableWithProgress() {
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        final List<DBNDatabaseNode> folders = ownerTableNode.getChildren(monitor);
                        for (DBNDatabaseNode node : folders) {
                            if (node instanceof DBNContainer && DBSTableColumn.class.isAssignableFrom(((DBNContainer) node).getItemsClass())) {
                                final List<DBNDatabaseNode> children = node.getChildren(monitor);
                                if (!CommonUtils.isEmpty(children)) {
                                    for (DBNDatabaseNode child : children) {
                                        if (child.getObject() instanceof DBSTableColumn) {
                                            columnNodes.add(child);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(getShell(), "Load columns", "Error loading table columns", e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }

        return dialogGroup;
    }

    protected void createContentsBeforeColumns(Composite panel)
    {

    }

    protected void createContentsAfterColumns(Composite panel)
    {

    }

    private void handleItemSelect(ISelection selection)
    {
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

    @Override
    protected void okPressed()
    {
        super.okPressed();
    }

    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(title + " for table '" + ownerTableNode.getNodeName() + "'");
        shell.setImage(ownerTableNode.getNodeIcon());
    }

}
