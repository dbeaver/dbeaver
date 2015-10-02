/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs.struct;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IProgressControlProvider;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.itemlist.ItemListControl;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * EditForeignKeyDialog
 *
 * @author Serge Rider
 */
public class EditForeignKeyDialog extends Dialog {

    public static class FKColumnInfo {
        final DBSEntityAttribute refColumn;
        DBSEntityAttribute ownColumn;

        FKColumnInfo(DBSEntityAttribute refColumn)
        {
            this.refColumn = refColumn;
        }

        public DBSEntityAttribute getRefColumn()
        {
            return refColumn;
        }

        public DBSEntityAttribute getOwnColumn()
        {
            return ownColumn;
        }
    }

    private String title;
    private IProgressControlProvider progressProvider;
    private DBSForeignKeyModifyRule[] supportedModifyRules;
    private DBSTable ownTable;
    private DBSTable curRefTable;
    private List<DBSTableConstraint> curConstraints;
    private DBNDatabaseNode ownerTableNode;
    private Combo uniqueKeyCombo;
    private Table columnsTable;

    private DBSTableConstraint curConstraint;
    private List<? extends DBSEntityAttribute> ownColumns;
    private List<FKColumnInfo> fkColumns = new ArrayList<>();
    private DBSForeignKeyModifyRule onDeleteRule;
    private DBSForeignKeyModifyRule onUpdateRule;

    public EditForeignKeyDialog(
        Shell shell,
        String title,
        DBSTable table,
        DBSForeignKeyModifyRule[] supportedModifyRules)
    {
        super(shell);
        setShellStyle(SWT.APPLICATION_MODAL | SWT.SHELL_TRIM);
        this.title = title;
        this.progressProvider = null;
        this.ownTable = table;
        this.ownerTableNode = DBeaverCore.getInstance().getNavigatorModel().findNode(ownTable);
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
            UIUtils.createLabelText(tableGroup, CoreMessages.dialog_struct_edit_fk_label_table, ownTable.getFullQualifiedName(), SWT.READ_ONLY | SWT.BORDER);
        }

        {
            DBNNode rootNode = ownerTableNode == null ? DBeaverCore.getInstance().getNavigatorModel().getRoot() : ownerTableNode.getParentNode();

            //Composite columnsGroup = UIUtils.createControlGroup(panel, "Reference Table", 1, GridData.FILL_BOTH, 0);
            UIUtils.createControlLabel(panel, CoreMessages.dialog_struct_edit_fk_label_ref_table);
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
                @Override
                public void selectionChanged(SelectionChangedEvent event)
                {
                    handleRefTableSelect(event.getSelection());
                }
            });
        }

        final Composite pkGroup = UIUtils.createPlaceholder(panel, 2);
        {
            pkGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            uniqueKeyCombo = UIUtils.createLabelCombo(pkGroup, CoreMessages.dialog_struct_edit_fk_combo_unik, SWT.DROP_DOWN | SWT.READ_ONLY);
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
            UIUtils.createControlLabel(panel, CoreMessages.dialog_struct_edit_fk_label_columns);
            columnsTable = new Table(panel, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
            columnsTable.setHeaderVisible(true);
            columnsTable.setLinesVisible(true);
            final GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 500;
            gd.heightHint = 100;
            columnsTable.setLayoutData(gd);

            UIUtils.createTableColumn(columnsTable, SWT.LEFT, CoreMessages.dialog_struct_edit_fk_column_column);
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, CoreMessages.dialog_struct_edit_fk_column_col_type);
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, CoreMessages.dialog_struct_edit_fk_column_ref_col);
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, CoreMessages.dialog_struct_edit_fk_column_ref_col_type);
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
            final Combo onDeleteCombo = UIUtils.createLabelCombo(cascadeGroup, CoreMessages.dialog_struct_edit_fk_combo_on_delete, SWT.DROP_DOWN | SWT.READ_ONLY);
            onDeleteCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            final Combo onUpdateCombo = UIUtils.createLabelCombo(cascadeGroup, CoreMessages.dialog_struct_edit_fk_combo_on_update, SWT.DROP_DOWN | SWT.READ_ONLY);
            onUpdateCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            for (DBSForeignKeyModifyRule modifyRule : supportedModifyRules) {
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
        //panel.setTabList(new Control[] { tableList, pkGroup, columnsTable, cascadeGroup });

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
            curConstraints = new ArrayList<>();
            curConstraint = null;
            final DBeaverCore core = DBeaverCore.getInstance();
            if (refTableNode != null) {
                final DBSTable refTable = (DBSTable) refTableNode.getObject();
                DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                    @Override
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        try {
                            // Cache own table columns
                            ownTable.getAttributes(monitor);

                            // Cache ref table columns
                            refTable.getAttributes(monitor);
                            // Get constraints
                            final Collection<? extends DBSTableConstraint> constraints = refTable.getConstraints(monitor);
                            if (!CommonUtils.isEmpty(constraints)) {
                                for (DBSTableConstraint constraint : constraints) {
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
            for (DBSTableConstraint constraint : curConstraints) {
                uniqueKeyCombo.add(constraint.getName());
            }
            uniqueKeyCombo.select(0);
            uniqueKeyCombo.setEnabled(curConstraints.size() > 1);
            if (curConstraints.size() == 1) {
                curConstraint = curConstraints.get(0);
            }

        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(getShell(), CoreMessages.dialog_struct_edit_fk_error_load_constraints_title, CoreMessages.dialog_struct_edit_fk_error_load_constraints_message, e.getTargetException());
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
            for (DBSEntityAttributeRef pkColumn : curConstraint.getAttributeReferences(VoidProgressMonitor.INSTANCE)) {
                FKColumnInfo fkColumnInfo = new FKColumnInfo(pkColumn.getAttribute());
                // Try to find matched column in own table
                Collection<? extends DBSEntityAttribute> tmpColumns = ownTable.getAttributes(VoidProgressMonitor.INSTANCE);
                ownColumns = tmpColumns == null ?
                    Collections.<DBSTableColumn>emptyList() :
                    new ArrayList<>(ownTable.getAttributes(VoidProgressMonitor.INSTANCE));
                if (!CommonUtils.isEmpty(ownColumns)) {
                    for (DBSEntityAttribute ownColumn : ownColumns) {
                        if (ownColumn.getName().equals(pkColumn.getAttribute().getName()) && ownTable != pkColumn.getAttribute().getParentObject()) {
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
                item.setText(2, pkColumn.getAttribute().getName());
                item.setImage(2, getColumnIcon(pkColumn.getAttribute()));
                item.setText(3, pkColumn.getAttribute().getTypeName());
                item.setData(fkColumnInfo);
            }
        } catch (DBException e) {
            UIUtils.showErrorDialog(getShell(), CoreMessages.dialog_struct_edit_fk_error_load_constraint_columns_title, CoreMessages.dialog_struct_edit_fk_error_load_constraint_columns_message, e);
        }
        UIUtils.packColumns(columnsTable, true);
    }

    private Image getColumnIcon(DBSEntityAttribute column)
    {
        if (column instanceof DBPImageProvider) {
            return DBeaverIcons.getImage(((DBPImageProvider) column).getObjectImage());
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

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        if (ownerTableNode != null) {
            shell.setText(NLS.bind(CoreMessages.dialog_struct_edit_fk_title, title, ownerTableNode.getNodeName()));
            shell.setImage(DBeaverIcons.getImage(ownerTableNode.getNodeIcon()));
        }
    }

    public List<FKColumnInfo> getColumns()
    {
        return fkColumns;
    }

    public DBSForeignKeyModifyRule getOnDeleteRule()
    {
        return onDeleteRule;
    }

    public DBSForeignKeyModifyRule getOnUpdateRule()
    {
        return onUpdateRule;
    }

    public DBSTableConstraint getUniqueConstraint()
    {
        return curConstraint;
    }

    private class ColumnsMouseListener extends MouseAdapter {
        private final TableEditor tableEditor;
        private final Table columnsTable;

        public ColumnsMouseListener(TableEditor tableEditor, Table columnsTable)
        {
            this.tableEditor = tableEditor;
            this.columnsTable = columnsTable;
        }

        private void disposeOldEditor()
        {
            Control oldEditor = tableEditor.getEditor();
            if (oldEditor != null) oldEditor.dispose();
        }

        @Override
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
            final FKColumnInfo fkInfo = (FKColumnInfo) item.getData();
            // Identify the selected row
            final CCombo columnsCombo = new CCombo(columnsTable, SWT.DROP_DOWN | SWT.READ_ONLY);
            if (!CommonUtils.isEmpty(ownColumns)) {
                for (DBSEntityAttribute ownColumn : ownColumns) {
                    columnsCombo.add(ownColumn.getName());
                    if (fkInfo.ownColumn == ownColumn) {
                        columnsCombo.select(columnsCombo.getItemCount() - 1);
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
                    if (columnsCombo.getSelectionIndex() >= 0) {
                        fkInfo.ownColumn = ownColumns.get(columnsCombo.getSelectionIndex());
                        item.setText(0, fkInfo.ownColumn.getName());
                        item.setImage(0, getColumnIcon(fkInfo.ownColumn));
                        item.setText(1, fkInfo.ownColumn.getTypeName());
                        updateButtons();
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
