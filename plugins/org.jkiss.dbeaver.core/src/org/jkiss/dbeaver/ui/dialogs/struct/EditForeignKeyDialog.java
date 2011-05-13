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
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IProgressControlProvider;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.*;
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
        final DBSTableColumn refColumn;
        DBSTableColumn ownColumn;

        FKColumnInfo(DBSTableColumn refColumn)
        {
            this.refColumn = refColumn;
        }

        public DBSTableColumn getRefColumn()
        {
            return refColumn;
        }

        public DBSTableColumn getOwnColumn()
        {
            return ownColumn;
        }
    }

    private String title;
    private IProgressControlProvider progressProvider;
    private DBSConstraintModifyRule[] supportedModifyRules;
    private DBSTable ownTable;
    private DBSTable curRefTable;
    private List<DBSConstraint> curConstraints;
    private DBNDatabaseNode ownerTableNode;
    private Combo uniqueKeyCombo;
    private Table columnsTable;

    private DBSConstraint curConstraint;
    private List<? extends DBSTableColumn> ownColumns;
    private List<FKColumnInfo> fkColumns = new ArrayList<FKColumnInfo>();
    private DBSConstraintModifyRule onDeleteRule;
    private DBSConstraintModifyRule onUpdateRule;

    public EditForeignKeyDialog(
        Shell shell,
        String title,
        IEditorPart curEditor,
        DBSTable table,
        DBSConstraintModifyRule[] supportedModifyRules)
    {
        super(shell);
        setShellStyle(SWT.APPLICATION_MODAL | SWT.SHELL_TRIM);
        this.title = title;
        if (curEditor instanceof MultiPageDatabaseEditor) {
            curEditor = ((MultiPageDatabaseEditor) curEditor).getActiveEditor();
        }
        this.progressProvider = curEditor instanceof IProgressControlProvider ? (IProgressControlProvider) curEditor : null;
        this.ownTable = table;
        this.ownerTableNode = DBeaverCore.getInstance().getNavigatorModel().findNode(ownTable);
        Assert.isNotNull(this.ownerTableNode);
        this.supportedModifyRules = supportedModifyRules;
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
            UIUtils.createLabelText(tableGroup, "Table", ownTable.getFullQualifiedName(), SWT.READ_ONLY | SWT.BORDER);
        }

        ItemListControl tableList;
        {
            DBNNode rootNode = ownerTableNode.getParentNode();

            //Composite columnsGroup = UIUtils.createControlGroup(panel, "Reference Table", 1, GridData.FILL_BOTH, 0);
            UIUtils.createControlLabel(panel, "Reference table");
            tableList = new ItemListControl(panel, SWT.SINGLE | SWT.SHEET | SWT.BORDER, null, rootNode, null);
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

        final Composite pkGroup = UIUtils.createPlaceholder(panel, 2);
        {
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
            columnsTable.setLinesVisible(true);
            final GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 500;
            gd.heightHint = 100;
            columnsTable.setLayoutData(gd);

            UIUtils.createTableColumn(columnsTable, SWT.LEFT, "Column");
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, "Column Type");
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, "Ref Column");
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, "Ref Column Type");
            UIUtils.packColumns(columnsTable);

            final TableEditor tableEditor = new TableEditor(columnsTable);
            tableEditor.horizontalAlignment = SWT.CENTER;
            tableEditor.verticalAlignment = SWT.TOP;
            tableEditor.grabHorizontal = true;
            tableEditor.minimumWidth = 50;

            columnsTable.addMouseListener(new ColumnsMouseListener(tableEditor, columnsTable));
        }

        final Composite cascadeGroup = UIUtils.createPlaceholder(panel, 4, 5);
        {
            // Cascades
            cascadeGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            final Combo onDeleteCombo = UIUtils.createLabelCombo(cascadeGroup, "On Delete", SWT.DROP_DOWN | SWT.READ_ONLY);
            onDeleteCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            final Combo onUpdateCombo = UIUtils.createLabelCombo(cascadeGroup, "On Update", SWT.DROP_DOWN | SWT.READ_ONLY);
            onUpdateCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            for (DBSConstraintModifyRule modifyRule : supportedModifyRules) {
                onDeleteCombo.add(modifyRule.getName());
                onUpdateCombo.add(modifyRule.getName());
            }
            onDeleteCombo.select(0);
            onUpdateCombo.select(0);
            onDeleteRule = onUpdateRule = supportedModifyRules[0];
            onDeleteCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    onDeleteRule = supportedModifyRules[onDeleteCombo.getSelectionIndex()];
                }
            });
            onUpdateCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    onUpdateRule = supportedModifyRules[onUpdateCombo.getSelectionIndex()];
                }
            });
        }
        panel.setTabList(new Control[] { tableList, pkGroup, columnsTable, cascadeGroup });

        return dialogGroup;
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
        if (refTableNode != null) {
            if (refTableNode.getObject() == curRefTable) {
                // The same selection
                return;
            } else {
                curRefTable = (DBSTable) refTableNode.getObject();
            }
        }
        uniqueKeyCombo.removeAll();

        try {
            curConstraints = new ArrayList<DBSConstraint>();
            curConstraint = null;
            final DBeaverCore core = DBeaverCore.getInstance();
            if (refTableNode != null) {
                final DBSTable refTable = (DBSTable) refTableNode.getObject();
                core.runInProgressService(new DBRRunnableWithProgress() {
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        try {
                            // Cache own table columns
                            ownTable.getColumns(monitor);

                            // Cache ref table columns
                            refTable.getColumns(monitor);
                            // Get constraints
                            final List<? extends DBSConstraint> constraints = refTable.getConstraints(monitor);
                            if (!CommonUtils.isEmpty(constraints)) {
                                for (DBSConstraint constraint : constraints) {
                                    if (constraint.getConstraintType().isUnique()) {
                                        curConstraints.add(constraint);
                                    }
                                }
                            }
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                });
            }
            for (DBSConstraint constraint : curConstraints) {
                uniqueKeyCombo.add(constraint.getName());
            }
            uniqueKeyCombo.select(0);
            uniqueKeyCombo.setEnabled(curConstraints.size() > 1);
            if (curConstraints.size() == 1) {
                curConstraint = curConstraints.get(0);
            }

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
        curConstraint = null;
        fkColumns.clear();
        ownColumns = null;
        columnsTable.removeAll();
        if (curConstraints.isEmpty() || uniqueKeyCombo.getSelectionIndex() < 0) {
            return;
        }
        curConstraint = curConstraints.get(uniqueKeyCombo.getSelectionIndex());
        try {
            // Read column nodes with void monitor because we already cached them above
            for (DBSConstraintColumn pkColumn : curConstraint.getColumns(VoidProgressMonitor.INSTANCE)) {
                FKColumnInfo fkColumnInfo = new FKColumnInfo(pkColumn.getTableColumn());
                // Try to find matched column in own table
                ownColumns = ownTable.getColumns(VoidProgressMonitor.INSTANCE);
                if (!CommonUtils.isEmpty(ownColumns)) {
                    for (DBSTableColumn ownColumn : ownColumns) {
                        if (ownColumn.getName().equals(pkColumn.getTableColumn().getName()) && ownTable != pkColumn.getTableColumn().getTable()) {
                            fkColumnInfo.ownColumn = ownColumn;
                            break;
                        }
                    }
                }
                fkColumns.add(fkColumnInfo);

                TableItem item = new TableItem(columnsTable, SWT.NONE);
                if (fkColumnInfo.ownColumn != null) {
                    item.setText(0, fkColumnInfo.ownColumn.getName());
                    item.setImage(0, getColumnIcon(fkColumnInfo.ownColumn));
                    item.setText(1, fkColumnInfo.ownColumn.getTypeName());
                }
                item.setText(2, pkColumn.getTableColumn().getName());
                item.setImage(2, getColumnIcon(pkColumn.getTableColumn()));
                item.setText(3, pkColumn.getTableColumn().getTypeName());
                item.setData(fkColumnInfo);
            }
        } catch (DBException e) {
            UIUtils.showErrorDialog(getShell(), "Load constraint columns", "Can't load table constraint columns", e);
        }
        UIUtils.packColumns(columnsTable, true);
    }

    private Image getColumnIcon(DBSTableColumn column)
    {
        if (column instanceof IObjectImageProvider) {
            return ((IObjectImageProvider) column).getObjectImage();
        }
        return null;
    }

    private void updateButtons()
    {
        boolean columnsValid = !fkColumns.isEmpty();
        for (FKColumnInfo col : fkColumns) {
            if (col.ownColumn == null || col.refColumn == null) {
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

    public DBSConstraintModifyRule getOnDeleteRule()
    {
        return onDeleteRule;
    }

    public DBSConstraintModifyRule getOnUpdateRule()
    {
        return onUpdateRule;
    }

    public DBSConstraint getUniqueConstraint()
    {
        return curConstraint;
    }

    private class ColumnsMouseListener extends MouseAdapter {
        private final TableEditor tableEditor;
        private final Table columnsTable;
        private FKColumnInfo curKeyColumn;

        public ColumnsMouseListener(TableEditor tableEditor, Table columnsTable)
        {
            this.tableEditor = tableEditor;
            this.columnsTable = columnsTable;
        }

        private void disposeOldEditor()
        {
            Control oldEditor = tableEditor.getEditor();
            if (oldEditor != null) oldEditor.dispose();
            curKeyColumn = null;
        }

        public void mouseUp(MouseEvent e)
        {
            handleColumnClick(e);
        }

        private void handleColumnClick(MouseEvent e) {
            // Clean up any previous editor control
            disposeOldEditor();

            final TableItem item = columnsTable.getItem(new Point(e.x, e.y));
            if (item == null) {
                return;
            }
            int columnIndex = UIUtils.getColumnAtPos(item, e.x, e.y);
            if (columnIndex != 0) {
                return;
            }

            // Identify the selected row
            final CCombo columnsCombo = new CCombo(columnsTable, SWT.DROP_DOWN | SWT.READ_ONLY);
            if (!CommonUtils.isEmpty(ownColumns)) {
                for (DBSTableColumn ownColumn : ownColumns) {
                    columnsCombo.add(ownColumn.getName());
                    for (FKColumnInfo fkInfo : fkColumns) {
                        if (fkInfo.ownColumn == ownColumn) {
                            curKeyColumn = fkInfo;
                            columnsCombo.select(columnsCombo.getItemCount() - 1);
                        }
                    }
                }
                if (columnsCombo.getSelectionIndex() < 0) {
                    columnsCombo.select(0);
                }
            }
            // Selected by mouse
            columnsCombo.setFocus();
            columnsCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    if (curKeyColumn != null && columnsCombo.getSelectionIndex() >= 0) {
                        curKeyColumn.ownColumn = ownColumns.get(columnsCombo.getSelectionIndex());
                        item.setText(0, curKeyColumn.ownColumn.getName());
                        item.setImage(0, getColumnIcon(curKeyColumn.ownColumn));
                    }
                }
            });
            columnsCombo.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e)
                {
                    disposeOldEditor();
                }
            });
            tableEditor.setEditor(columnsCombo, item, 0);
        }
    }

}
