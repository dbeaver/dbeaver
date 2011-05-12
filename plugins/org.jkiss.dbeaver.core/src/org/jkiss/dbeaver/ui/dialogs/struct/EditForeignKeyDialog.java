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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IProgressControlProvider;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSConstraint;
import org.jkiss.dbeaver.model.struct.DBSConstraintColumn;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.itemlist.ItemListControl;
import org.jkiss.dbeaver.ui.editors.MultiPageDatabaseEditor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * EditForeignKeyDialog
 *
 * @author Serge Rider
 */
public class EditForeignKeyDialog extends Dialog {

    public static class FKColumnInfo {
        final DBNDatabaseNode refColumnNode;
        DBNDatabaseNode ownColumnNode;

        FKColumnInfo(DBNDatabaseNode refColumnNode)
        {
            this.refColumnNode = refColumnNode;
        }

    }

    private String title;
    private IProgressControlProvider progressProvider;
    private DBNDatabaseNode ownerTableNode;
    private List<DBNDatabaseNode> constraintNodes;
    private Combo uniqueKeyCombo;
    private Table columnsTable;

    private List<FKColumnInfo> fkColumns = new ArrayList<FKColumnInfo>();

    public EditForeignKeyDialog(
        Shell shell,
        String title,
        IEditorPart curEditor,
        DBSTable table) {
        super(shell);
        setShellStyle(SWT.APPLICATION_MODAL | SWT.SHELL_TRIM);
        this.title = title;
        if (curEditor instanceof MultiPageDatabaseEditor) {
            curEditor = ((MultiPageDatabaseEditor) curEditor).getActiveEditor();
        }
        this.progressProvider = curEditor instanceof IProgressControlProvider ? (IProgressControlProvider) curEditor : null;
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
            UIUtils.createLabelText(tableGroup, "Table", ownerTableNode.getNodeName(), SWT.READ_ONLY | SWT.BORDER);

            createContentsBeforeColumns(tableGroup);
        }

        {
            DBNNode rootNode = ownerTableNode.getParentNode();

            //Composite columnsGroup = UIUtils.createControlGroup(panel, "Reference Table", 1, GridData.FILL_BOTH, 0);
            UIUtils.createControlLabel(panel, "Reference table");
            ItemListControl tableList = new ItemListControl(panel, SWT.SINGLE | SWT.SHEET | SWT.BORDER, null, rootNode, null);
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
                    handleRefTableSelect(event.getSelection());
                }
            });
        }
        {
            final Composite pkGroup = UIUtils.createPlaceholder(panel, 2);
            pkGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            uniqueKeyCombo = UIUtils.createLabelCombo(pkGroup, "Unique Key", SWT.DROP_DOWN | SWT.READ_ONLY);
            uniqueKeyCombo.setEnabled(false);
            uniqueKeyCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    handleUniqueKeySelect();
                }
            });
        }
        {
            UIUtils.createControlLabel(panel, "Columns");
            columnsTable = new Table(panel, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
            columnsTable.setHeaderVisible(true);
            final GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 500;
            gd.heightHint = 100;
            columnsTable.setLayoutData(gd);

            UIUtils.createTableColumn(columnsTable, SWT.LEFT, "Column");
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, "Column Type");
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, "Ref Column");
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, "Ref Column Type");
            UIUtils.packColumns(columnsTable);
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

    private void handleRefTableSelect(ISelection selection)
    {
        DBNDatabaseNode refTableNode = null;
        if (!selection.isEmpty() && selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() == 1) {
            final Object element = ((IStructuredSelection) selection).getFirstElement();
            if (element instanceof DBNDatabaseNode &&
                ((DBNDatabaseNode) element).getObject() instanceof DBSTable &&
                ((DBNDatabaseNode) element).getObject().isPersisted())
            {
                refTableNode = (DBNDatabaseNode) element;
            }
        }
        uniqueKeyCombo.removeAll();

        try {
            constraintNodes = new ArrayList<DBNDatabaseNode>();
            final DBeaverCore core = DBeaverCore.getInstance();
            if (refTableNode != null) {
                final DBSTable refTable = (DBSTable) refTableNode.getObject();
                core.runInProgressService(new DBRRunnableWithProgress() {
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        try {
                            final List<? extends DBSConstraint> constraints = refTable.getConstraints(monitor);
                            if (!CommonUtils.isEmpty(constraints)) {
                                for (DBSTableColumn col : refTable.getColumns(monitor)) {
                                    core.getNavigatorModel().getNodeByObject(monitor, col, true);
                                }

                                for (DBSConstraint constraint : constraints) {
                                    if (constraint.getConstraintType().isUnique()) {
                                        final DBNDatabaseNode constraintNode = core.getNavigatorModel().getNodeByObject(monitor, constraint, true);
                                        if (constraintNode != null) {
                                            // Cache constraint columns (we assume that columns are direct children)
                                            // Cache constraint's table columns
                                            constraintNode.getChildren(monitor);
                                            constraintNodes.add(constraintNode);
                                        }
                                    }
                                }
                            }
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                });
            }
            for (DBNDatabaseNode node : constraintNodes) {
                uniqueKeyCombo.add(node.getNodeName());
            }
            uniqueKeyCombo.select(0);
            uniqueKeyCombo.setEnabled(constraintNodes.size() > 1);

        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(getShell(), "Load constraints", "Can't load table constraints", e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }
        handleUniqueKeySelect();
        updateButtons();
    }

    private void handleUniqueKeySelect()
    {
        fkColumns.clear();
        columnsTable.removeAll();
        if (constraintNodes.isEmpty() || uniqueKeyCombo.getSelectionIndex() < 0) {
            return;
        }
        final DBNDatabaseNode uniqueKeyNode = constraintNodes.get(uniqueKeyCombo.getSelectionIndex());
        try {
            // Read column nodes with void monitor because we already cached them above
            final List<DBNDatabaseNode> refColumnNodes = uniqueKeyNode.getChildren(VoidProgressMonitor.INSTANCE);
            if (!CommonUtils.isEmpty(refColumnNodes)) {
                for (DBNDatabaseNode columnNode : refColumnNodes) {
                    if (columnNode.getObject() instanceof DBSConstraintColumn) {
                        DBSConstraintColumn pkColumn = (DBSConstraintColumn) columnNode.getObject();
                        DBNDatabaseNode refColumnNode = DBeaverCore.getInstance().getNavigatorModel().findNode(pkColumn.getTableColumn());
                        if (refColumnNode != null) {
                            FKColumnInfo fkColumnInfo = new FKColumnInfo(refColumnNode);
                            // Try to find matched column in own table
                            DBSTable ownTable = (DBSTable)ownerTableNode.getObject();
                            DBSTableColumn ownColumn = null;
                            final List<? extends DBSTableColumn> ownColumns = ownTable.getColumns(VoidProgressMonitor.INSTANCE);
                            if (!CommonUtils.isEmpty(ownColumns)) {
                                for (DBSTableColumn col : ownColumns) {
                                    DBNDatabaseNode colNode = DBeaverCore.getInstance().getNavigatorModel().findNode(col);
                                    if (colNode != null && colNode.getNodeName().equals(refColumnNode.getNodeName()) && ownTable != refColumnNode.getObject().getParentObject()) {
                                        ownColumn = col;
                                        fkColumnInfo.ownColumnNode = colNode;
                                        break;
                                    }
                                }
                            }
                            fkColumns.add(fkColumnInfo);

                            TableItem item = new TableItem(columnsTable, SWT.NONE);
                            if (ownColumn != null) {
                                item.setText(0, fkColumnInfo.ownColumnNode.getNodeName());
                                item.setImage(0, fkColumnInfo.ownColumnNode.getNodeIcon());
                                item.setText(1, ownColumn.getTypeName());
                            }
                            item.setText(2, refColumnNode.getNodeName());
                            item.setImage(2, refColumnNode.getNodeIcon());
                            item.setText(3, pkColumn.getTableColumn().getTypeName());
                            item.setData(fkColumnInfo);
                        }
                    }
                }
            }
        } catch (DBException e) {
            UIUtils.showErrorDialog(getShell(), "Load constraint columns", "Can't load table constraint columns", e);
        }
        UIUtils.packColumns(columnsTable);
    }

    private void updateButtons()
    {
        boolean columnsValid = !fkColumns.isEmpty();
        for (FKColumnInfo col : fkColumns) {
            if (col.ownColumnNode == null || col.refColumnNode == null) {
                columnsValid = false;
                break;
            }
        }

        getButton(IDialogConstants.OK_ID).setEnabled(columnsValid);
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

    public List<FKColumnInfo> getColumns()
    {
        return fkColumns;
    }
}
