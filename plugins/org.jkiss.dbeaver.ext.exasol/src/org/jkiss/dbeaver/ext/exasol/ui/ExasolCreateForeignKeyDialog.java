/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017-2017 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.ui;

import org.jkiss.dbeaver.ext.exasol.model.ExasolTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;





public class ExasolCreateForeignKeyDialog extends EditForeignKeyPage {
    public ExasolCreateForeignKeyDialog(String title, ExasolTable table) {
        super(title, table, new DBSForeignKeyModifyRule[0]);
    }

    @Override
    protected boolean supportsCustomName() {
        return true;
    }

    /*
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
	
	private ExasolTable ownTable;
	private ExasolTable curRefTable;
	private List<ExasolTableUniqueKey> curConstraints;
	private DBNDatabaseNode ownerTableNode;
	private Combo uniqueKeyCombo;
	private Table columnsTable;
	private ItemListControl tableList;
	private String name;
	
	private Boolean constEnabled = false;

	private ExasolTableUniqueKey curConstraint;
	private List<? extends DBSEntityAttribute> ownColumns;
	private List<FKColumnInfo> fkColumns = new ArrayList<>();

    private static final Log log = Log.getLog(EditForeignKeyPage.class);
	

	
	public ExasolCreateForeignKeyDialog(String title, ExasolTable ownTable) {
		super(title);
        this.ownTable = ownTable;
        this.ownerTableNode = DBWorkbench.getPlatform().getNavigatorModel().findNode(ownTable);

        if (ownerTableNode != null) {
            setImageDescriptor(DBeaverIcons.getImageDescriptor(ownerTableNode.getNodeIcon()));
            setTitle(title + " | " + NLS.bind(CoreMessages.dialog_struct_edit_fk_title, title, ownerTableNode.getNodeName()));
        }

    }
	
    
    @Override
    protected Control createPageContents(Composite parent) {
        final Composite panel = UIUtils.createPlaceholder(parent, 1, 5);
        panel.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            final Composite tableGroup = UIUtils.createPlaceholder(panel, 2, 5);
            tableGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            UIUtils.createLabelText(tableGroup, CoreMessages.dialog_struct_edit_fk_label_table, ownTable.getFullyQualifiedName(DBPEvaluationContext.UI), SWT.READ_ONLY | SWT.BORDER);
            
            final Text fkName = UIUtils.createLabelText(tableGroup, ExasolMessages.dialog_struct_edit_fk_label_fk_name, "NEW_FOREIGN_KEY");
            name = fkName.getText();
            fkName.addModifyListener(new ModifyListener() {
				
				@Override
				public void modifyText(ModifyEvent arg0)
				{
					name = fkName.getText();
				}
			});

            if (ownerTableNode != null) {
                try {
                    createSchemaSelector(tableGroup);
                } catch (Throwable e) {
                    log.debug("Can't create schema selector", e);
                }
            }
        }

        {
            DBNNode rootNode = ownerTableNode == null ? DBWorkbench.getPlatform().getNavigatorModel().getRoot() : ownerTableNode.getParentNode();

            UIUtils.createControlLabel(panel, CoreMessages.dialog_struct_edit_fk_label_ref_table);
            tableList = new ItemListControl(panel, SWT.SINGLE | SWT.SHEET | SWT.BORDER, null, rootNode, null);

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
            final Button enabled = UIUtils.createCheckbox(cascadeGroup, "Enable Constraint", false);
            
            enabled.addSelectionListener(new SelectionAdapter() {
            	@Override
            	public void widgetSelected(SelectionEvent e)
            	{
            		constEnabled = enabled.getSelection();
            	}
			});
        }
        //panel.setTabList(new Control[] { tableList, pkGroup, columnsTable, cascadeGroup });
        //panel.setTabList(new Control[] { tableList, pkGroup, columnsTable, cascadeGroup });

        return panel;
    }

    


    private void createSchemaSelector(Composite tableGroup) throws DBException {
        // Here is a trick - we need to find schema/catalog container node and list its children
        DBNDatabaseNode schemaContainerNode = null;
        for (DBNNode node = ownerTableNode.getParentNode(); node != null; node = node.getParentNode()) {
            if (node instanceof DBNDatabaseNode) {
                DBSObject nodeObject = ((DBNDatabaseNode) node).getObject();
                if (nodeObject instanceof DBSSchema || nodeObject instanceof DBSCatalog) {
                    if (node.getParentNode() instanceof DBNDatabaseNode) {
                        schemaContainerNode = (DBNDatabaseNode) node.getParentNode();
                        break;
                    }
                }

            }
        }
        if (schemaContainerNode != null) {
            ILabelProvider labelProvider = new LabelProvider() {
                @Override
                public Image getImage(Object element) {
                    return DBeaverIcons.getImage(((DBNDatabaseNode) element).getNodeIcon());
                }
                @Override
                public String getText(Object element) {
                    return ((DBNDatabaseNode) element).getNodeName();
                }
            };

            boolean isSchema = (ownTable.getParentObject() instanceof DBSSchema);
            DBPDataSourceInfo dsInfo = ownTable.getDataSource().getInfo();

            UIUtils.createControlLabel(tableGroup, isSchema ? dsInfo.getSchemaTerm() : dsInfo.getCatalogTerm());
            final CSmartCombo<DBNDatabaseNode> schemaCombo = new CSmartCombo<>(tableGroup, SWT.BORDER, labelProvider);
            schemaCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            DBNDatabaseNode selectedNode = null;
            for (DBNNode node : schemaContainerNode.getChildren(new VoidProgressMonitor())) {
                if (node instanceof DBNDatabaseNode && ((DBNDatabaseNode) node).getObject() instanceof DBSObjectContainer) {
                    schemaCombo.addItem((DBNDatabaseNode) node);
                    if (((DBNDatabaseNode) node).getObject() == ownTable.getParentObject()) {
                        selectedNode = (DBNDatabaseNode) node;
                    }
                }
            }
            if (selectedNode != null) {
                schemaCombo.select(selectedNode);
            }

            schemaCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    // Here is another trick
                    // We need to find table container node
                    // This node is a child of schema node and has the same meta as our original table parent node
                    DBNDatabaseNode newContainerNode = null;
                    DBXTreeNode tableContainerMeta = ((DBNDatabaseNode) ownerTableNode.getParentNode()).getMeta();
                    DBNDatabaseNode schemaNode = schemaCombo.getSelectedItem();
                    if (schemaNode.getMeta() == tableContainerMeta) {
                        newContainerNode = schemaNode;
                    } else {
                        try {
                            for (DBNNode child : schemaNode.getChildren(new VoidProgressMonitor())) {
                                if (child instanceof DBNDatabaseNode && ((DBNDatabaseNode) child).getMeta() == tableContainerMeta) {
                                    newContainerNode = (DBNDatabaseNode) child;
                                    break;
                                }
                            }
                        } catch (DBException e1) {
                            log.debug(e1);
                            // Shouldn't be here
                        }
                    }
                    if (newContainerNode != null) {
                        tableList.setRootNode(newContainerNode);
                        tableList.loadData();
                    }
                }
            });
        }
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
                curRefTable = (ExasolTable) refTableNode.getObject();
            }
        }
        uniqueKeyCombo.removeAll();

        try {
            curConstraints = new ArrayList<>();
            curConstraint = null;
            if (refTableNode != null) {
                final ExasolTable refTable = (ExasolTable) refTableNode.getObject();
                UIUtils.runInProgressService(new DBRRunnableWithProgress() {
                    @Override
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        try {
                            // Cache own table columns
                            ownTable.getAttributes(monitor);

                            // Cache ref table columns
                            refTable.getAttributes(monitor);
                            // Get constraints
                            final Collection<? extends ExasolTableUniqueKey> constraints = refTable.getConstraints(monitor);
                            if (!CommonUtils.isEmpty(constraints)) {
                                for (ExasolTableUniqueKey constraint : constraints) {
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
            DBUserInterface.getInstance().showError(CoreMessages.dialog_struct_edit_fk_error_load_constraints_title, CoreMessages.dialog_struct_edit_fk_error_load_constraints_message, e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }
        handleUniqueKeySelect();
        updatePageState();
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
                        item.setText(1, fkInfo.ownColumn.getFullTypeName());
                        updatePageState();
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
    
    private Image getColumnIcon(DBSEntityAttribute column)
    {
        return DBeaverIcons.getImage(DBValueFormatting.getObjectImage(column));
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
            for (DBSEntityAttributeRef pkColumn : curConstraint.getAttributeReferences(new VoidProgressMonitor())) {
                FKColumnInfo fkColumnInfo = new FKColumnInfo(pkColumn.getAttribute());
                // Try to find matched column in own table
                Collection<? extends DBSEntityAttribute> tmpColumns = ownTable.getAttributes(new VoidProgressMonitor());
                ownColumns = tmpColumns == null ?
                    Collections.<DBSTableColumn>emptyList() :
                    new ArrayList<>(ownTable.getAttributes(new VoidProgressMonitor()));
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
                    item.setText(1, fkColumnInfo.ownColumn.getFullTypeName());
                }
                item.setText(2, pkColumn.getAttribute().getName());
                item.setImage(2, getColumnIcon(pkColumn.getAttribute()));
                item.setText(3, pkColumn.getAttribute().getFullTypeName());
                item.setData(fkColumnInfo);
            }
        } catch (DBException e) {
            DBUserInterface.getInstance().showError(CoreMessages.dialog_struct_edit_fk_error_load_constraint_columns_title, CoreMessages.dialog_struct_edit_fk_error_load_constraint_columns_message, e);
        }
        UIUtils.packColumns(columnsTable, true);
    }
    public Boolean isEnabled()
    {
    	return this.constEnabled;
    }
    
    public List<FKColumnInfo> getColumns()
    {
        return fkColumns;
    }

    public ExasolTableUniqueKey getUniqueConstraint()
    {
        return curConstraint;
    }
    
    public String getName()
    {
    	return name;
    }


*/
}
