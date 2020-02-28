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
package org.jkiss.dbeaver.ui.editors.object.struct;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.dbeaver.model.virtual.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CSmartCombo;
import org.jkiss.dbeaver.ui.controls.ObjectContainerSelectorPanel;
import org.jkiss.dbeaver.ui.editors.internal.EditorsMessages;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * EditForeignKeyPage
 *
 * @author Serge Rider
 */
public class EditForeignKeyPage extends BaseObjectEditPage {

    private static final String CONTAINER_LOGICAL_FK = "container.logical-fk";
    private static final Log log = Log.getLog(EditForeignKeyPage.class);

    private static final FKType FK_TYPE_PHYSICAL = new FKType("Physical", true);
    public static final FKType FK_TYPE_LOGICAL = new FKType("Logical", false);

    private DBSForeignKeyModifyRule[] supportedModifyRules;
    private DBSEntityAssociation foreignKey;
    private DBSEntity curRefTable;
    private List<DBSEntityConstraint> curConstraints;
    private DBNDatabaseNode ownerTableNode, ownerContainerNode;
    private Table tableList;
    private Combo uniqueKeyCombo;
    private Text fkNameText;
    private Table columnsTable;
    private Button customUKButton;

    private String fkName;

    private DBSEntityConstraint curConstraint;
    private List<? extends DBSEntityAttribute> ownColumns;
    private List<FKColumnInfo> fkColumns = new ArrayList<>();
    private DBSForeignKeyModifyRule onDeleteRule;
    private DBSForeignKeyModifyRule onUpdateRule;

    private boolean enableCustomKeys = false;
    private boolean supportsCustomName = false;

    private FKType[] allowedKeyTypes = new FKType[] {  FK_TYPE_PHYSICAL };
    private FKType preferredKeyType = FK_TYPE_PHYSICAL;
    private FKType selectedKeyType = FK_TYPE_PHYSICAL;

    private List<DBSEntityAttribute> sourceAttributes;
    private List<DBSEntityAttribute> refAttributes;

    private final List<Control> physicalKeyComponents = new ArrayList<>();

    public static class FKType implements DBPNamedObject {
        private final String name;
        private final boolean physical;

        FKType(String name, boolean physical) {
            this.name = name;
            this.physical = physical;
        }

        @NotNull
        @Override
        public String getName() {
            return name;
        }

        public boolean isPhysical() {
            return physical;
        }
    }

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

    public EditForeignKeyPage(
        String title,
        DBSEntityAssociation foreignKey,
        DBSForeignKeyModifyRule[] supportedModifyRules)
    {
        super(title);
        this.foreignKey = foreignKey;
        this.ownerTableNode = DBWorkbench.getPlatform().getNavigatorModel().findNode(foreignKey.getParentObject());
        this.supportedModifyRules = supportedModifyRules;

        if (ownerTableNode == null) {
            try {
                if (foreignKey.getParentObject() instanceof DBVEntity) {
                    DBSEntity realEntity = ((DBVEntity) foreignKey.getParentObject()).getRealEntity(new VoidProgressMonitor());
                    if (realEntity != null) {
                        ownerTableNode = DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(realEntity);
                        if (ownerTableNode == null) {
                            try {
                                UIUtils.runInProgressDialog(monitor ->
                                    ownerTableNode = DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(
                                        monitor, realEntity, true));
                            } catch (InvocationTargetException e) {
                                setErrorMessage(e.getTargetException().getMessage());
                                log.error(e.getTargetException());
                            }
                        }
                    }
                }
            } catch (DBException e) {
                log.error(e);
            }
        }

        if (ownerTableNode != null) {
            setImageDescriptor(DBeaverIcons.getImageDescriptor(ownerTableNode.getNodeIcon()));
            setTitle(title + " | " + NLS.bind(EditorsMessages.dialog_struct_edit_fk_title, title, ownerTableNode.getNodeName()));
        }
    }

    public boolean isEnableCustomKeys() {
        return enableCustomKeys;
    }

    private void setEnableCustomKeys(boolean enableCustomKeys) {
        this.enableCustomKeys = enableCustomKeys;
    }

    private void setAllowedKeyTypes(FKType[] allowedKeyTypes) {
        this.allowedKeyTypes = allowedKeyTypes;
        setPreferredKeyType(allowedKeyTypes[0]);
    }

    private void setPreferredKeyType(FKType preferredKeyType) {
        this.preferredKeyType = preferredKeyType;
        this.selectedKeyType = preferredKeyType;
    }

    private void setRefTable(DBSEntity curRefTable) {
        this.curRefTable = curRefTable;
    }

    protected void addPhysicalKeyComponent(Control control) {
        physicalKeyComponents.add(control);
    }

    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
        updateControlsVisibility();
        if (curRefTable != null) {
            handleRefTableSelect();
        }
        UIUtils.asyncExec(() -> UIUtils.packColumns(columnsTable, true));
    }

    @Override
    protected Composite createPageContents(Composite parent) {
        final Composite panel = UIUtils.createComposite(parent, 1);
        panel.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            final Composite tableGroup = UIUtils.createComposite(panel, 2);
            tableGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            UIUtils.createLabelText(
                tableGroup,
                EditorsMessages.dialog_struct_edit_fk_label_table, DBUtils.getObjectFullName(foreignKey.getParentObject(), DBPEvaluationContext.UI), SWT.READ_ONLY | SWT.BORDER);

            if (allowedKeyTypes.length > 1) {
                UIUtils.createControlLabel(tableGroup, "Key type");
                Composite ktPanel = UIUtils.createFormPlaceholder(tableGroup, allowedKeyTypes.length, 1);
                //keyTypeCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
                for (FKType type : allowedKeyTypes) {
                    Button keyTypeButton = UIUtils.createRadioButton(ktPanel, type.getName(), type, new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            selectedKeyType = (FKType) e.widget.getData();
                            updateControlsVisibility();
                        }
                    });
                    if (type == preferredKeyType) {
                        keyTypeButton.setSelection(true);
                    }
                }
            }

            if (curRefTable == null) {
                try {
                    if (foreignKey instanceof DBVEntityForeignKey) {
                        // Virtual key - add container selector
                        createContainerSelector(tableGroup);
                    } else if (ownerTableNode != null) {
                        createSchemaSelector(tableGroup);
                    }
                } catch (Throwable e) {
                    log.debug("Can't create schema selector", e);
                }
            } else {
                UIUtils.createLabelText(
                    tableGroup,
                    EditorsMessages.dialog_struct_edit_fk_label_ref_table, DBUtils.getObjectFullName(curRefTable, DBPEvaluationContext.UI), SWT.READ_ONLY | SWT.BORDER);
            }
        }

        if (curRefTable == null) {
            DBNNode containerNode = ownerTableNode == null ? null : ownerTableNode.getParentNode();
            while (containerNode instanceof DBNDatabaseFolder) {
                containerNode = containerNode.getParentNode();
            }

            DBNNode rootNode = containerNode == null ? DBWorkbench.getPlatform().getNavigatorModel().getRoot() : containerNode;

            UIUtils.createControlLabel(panel, EditorsMessages.dialog_struct_edit_fk_label_ref_table);
            tableList = new Table(panel, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
            tableList.setLinesVisible(true);
            final GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 500;
            gd.heightHint = 150;
            tableList.setLayoutData(gd);
            tableList.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    handleRefTableSelect((DBNDatabaseNode) e.item.getData());
                }
            });
            if (rootNode instanceof DBNDatabaseNode) {
                loadTableList((DBNDatabaseNode) rootNode);
            }
        }

        final Composite pkGroup = UIUtils.createComposite(panel, enableCustomKeys ? 3 : 2);
        {
            pkGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            uniqueKeyCombo = UIUtils.createLabelCombo(pkGroup, EditorsMessages.dialog_struct_edit_fk_combo_unik, SWT.DROP_DOWN | SWT.READ_ONLY);
            //uniqueKeyCombo.setEnabled(false);
            uniqueKeyCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    handleUniqueKeySelect();
                    updatePageState();
                }
            });
            if (enableCustomKeys) {
                customUKButton = UIUtils.createDialogButton(pkGroup, "Create", new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        defineRefTableConstraint();
                    }
                });
                customUKButton.setEnabled(false);
            }

            if (supportsCustomName()) {
                fkNameText = UIUtils.createLabelText(pkGroup, EditorsMessages.dialog_struct_edit_fk_name, "");
                fkNameText.addModifyListener(e -> fkName = fkNameText.getText());
            }
        }
        {
            UIUtils.createControlLabel(panel, EditorsMessages.dialog_struct_edit_fk_label_columns);
            columnsTable = new Table(panel, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
            columnsTable.setHeaderVisible(true);
            columnsTable.setLinesVisible(true);
            final GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 500;
            gd.heightHint = 100;
            columnsTable.setLayoutData(gd);

            UIUtils.createTableColumn(columnsTable, SWT.LEFT, EditorsMessages.dialog_struct_edit_fk_column_column);
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, EditorsMessages.dialog_struct_edit_fk_column_col_type);
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, EditorsMessages.dialog_struct_edit_fk_column_ref_col);
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, EditorsMessages.dialog_struct_edit_fk_column_ref_col_type);

            final TableEditor tableEditor = new TableEditor(columnsTable);
            tableEditor.horizontalAlignment = SWT.CENTER;
            tableEditor.verticalAlignment = SWT.TOP;
            tableEditor.grabHorizontal = true;
            tableEditor.minimumWidth = 50;

            columnsTable.addMouseListener(new ColumnsMouseListener(tableEditor, columnsTable));
        }

        if (!ArrayUtils.isEmpty(supportedModifyRules)) {
            final Composite cascadeGroup = UIUtils.createPlaceholder(panel, 4, 5);
            {
                // Cascades
                cascadeGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                final Combo onDeleteCombo = UIUtils.createLabelCombo(cascadeGroup, EditorsMessages.dialog_struct_edit_fk_combo_on_delete, SWT.DROP_DOWN | SWT.READ_ONLY);
                onDeleteCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                final Combo onUpdateCombo = UIUtils.createLabelCombo(cascadeGroup, EditorsMessages.dialog_struct_edit_fk_combo_on_update, SWT.DROP_DOWN | SWT.READ_ONLY);
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
                    public void widgetSelected(SelectionEvent e) {
                        onDeleteRule = supportedModifyRules[onDeleteCombo.getSelectionIndex()];
                    }
                });
                onUpdateCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        onUpdateRule = supportedModifyRules[onUpdateCombo.getSelectionIndex()];
                    }
                });
            }
            addPhysicalKeyComponent(cascadeGroup);
        }

        if (tableList != null) {
            tableList.setFocus();
        }

        return panel;
    }

    private void updateControlsVisibility() {
        boolean pkVisible = selectedKeyType.isPhysical();
        for (Control pkc : physicalKeyComponents) {
            pkc.setVisible(pkVisible);
            if (pkc.getLayoutData() instanceof GridData) {
                ((GridData) pkc.getLayoutData()).exclude = !pkVisible;
            }
        }

        ((Composite)getControl()).layout(true, true);
    }

    private void defineRefTableConstraint() {
        if (curRefTable == null) {
            log.error("No reference table");
            return;
        }
        DBVEntity vRefEntity = DBVUtils.getVirtualEntity(curRefTable, true);
        assert vRefEntity != null;
        DBVEntityConstraint constraint = vRefEntity.getBestIdentifier();

        EditConstraintPage page = new EditConstraintPage(
            "Define unique key",
            constraint);
        if (page.edit()) {
            constraint.setAttributes(page.getSelectedAttributes());
            handleRefTableSelect(DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(curRefTable));
            int constraintIndex = curConstraints.indexOf(constraint);
            uniqueKeyCombo.select(constraintIndex);
            handleUniqueKeySelect();
        }
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

            boolean isSchema = (foreignKey.getParentObject().getParentObject() instanceof DBSSchema);
            DBPDataSourceInfo dsInfo = foreignKey.getDataSource().getInfo();

            UIUtils.createControlLabel(tableGroup, "Container");
            final CSmartCombo<DBNDatabaseNode> schemaCombo = new CSmartCombo<>(tableGroup, SWT.BORDER, labelProvider);
            schemaCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            DBNDatabaseNode selectedNode = null;
            for (DBNNode node : schemaContainerNode.getChildren(new VoidProgressMonitor())) {
                if (node instanceof DBNDatabaseNode && ((DBNDatabaseNode) node).getObject() instanceof DBSObjectContainer) {
                    schemaCombo.addItem((DBNDatabaseNode) node);
                    if (((DBNDatabaseNode) node).getObject() == foreignKey.getParentObject().getParentObject()) {
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
                        loadTableList(newContainerNode);
                    }
                }
            });
        }
    }

    private void createContainerSelector(Composite tableGroup) throws DBException {
        ObjectContainerSelectorPanel containerPanel = new ObjectContainerSelectorPanel(
            tableGroup,
            foreignKey.getDataSource().getContainer().getRegistry().getProject(),
            CONTAINER_LOGICAL_FK,
            "Reference table container",
            "Select reference table catalog/schema") {
            @Nullable
            @Override
            protected DBNNode getSelectedNode() {
                if (ownerContainerNode != null) {
                    return ownerContainerNode;
                }
                DBSObject containerObject;
                if (ownerTableNode != null) {
                    DBNNode containerNode = ownerTableNode.getParentNode();
                    while (containerNode instanceof DBNDatabaseFolder) {
                        containerNode = containerNode.getParentNode();
                    }
                    if (containerNode instanceof DBNDatabaseNode) {
                        containerObject = ((DBNDatabaseNode)containerNode).getObject();
                    } else {
                        containerObject = null;
                    }
                } else {
                    containerObject = foreignKey.getParentObject();
                }
                if (containerObject != null && containerObject.getParentObject() instanceof DBSObjectContainer) {
                    containerObject = containerObject.getParentObject();
                }
                if (containerObject instanceof DBVContainer) {
                    try {
                        containerObject = ((DBVContainer)containerObject).getRealContainer(new VoidProgressMonitor());
                    } catch (DBException e) {
                        log.error("Error getting real object container", e);
                    }
                }
                return ownerContainerNode = DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(containerObject);
            }

            @Override
            protected void setSelectedNode(DBNDatabaseNode node) {
                ownerContainerNode = node;
                if (ownerContainerNode == null) {
                    setContainerInfo(null);
                } else {
                    setContainerInfo(node);
                    loadTableList(ownerContainerNode);
                }
            }
        };
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        containerPanel.setLayoutData(gd);

        if (ownerTableNode != null) {
            DBNNode containerNode = ownerTableNode.getParentNode();
            while (containerNode instanceof DBNDatabaseFolder) {
                containerNode = containerNode.getParentNode();
            }
            if (containerNode instanceof DBNDatabaseNode) {
                containerPanel.setContainerInfo((DBNDatabaseNode) containerNode);
            }
        }
    }

    private void loadTableList(DBNDatabaseNode newContainerNode) {
        tableList.removeAll();
        final List<DBNDatabaseNode> entities = new ArrayList<>();
        try {
            UIUtils.runInProgressService(monitor -> {
                try {
                    loadEntities(monitor, entities, newContainerNode);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Error loading tables", "Error during table load", e);
        } catch (InterruptedException e) {
            // Ignore
        }

        for (DBNDatabaseNode entityNode : entities) {
            TableItem tableItem = new TableItem(tableList, SWT.LEFT);
            tableItem.setText(entityNode.getNodeName());
            tableItem.setImage(DBeaverIcons.getImage(entityNode.getNodeIconDefault()));
            tableItem.setData(entityNode);
        }

    }
    private void loadEntities(DBRProgressMonitor monitor, List<DBNDatabaseNode> entities, DBNDatabaseNode container) throws DBException {
        for (DBNNode childNode : container.getChildren(monitor)) {
            if (monitor.isCanceled()) {
                break;
            }
            if (childNode instanceof DBNDatabaseFolder) {
                DBXTreeItem itemsMeta = ((DBNDatabaseFolder) childNode).getItemsMeta();
                if (itemsMeta != null) {
                    Class<?> childrenClass = ((DBNDatabaseFolder) childNode).getChildrenClass(itemsMeta);
                    if (childrenClass != null && DBSEntity.class.isAssignableFrom(childrenClass)) {
                        loadEntities(monitor, entities, (DBNDatabaseFolder) childNode);
                    }
                }
            } else {
                if (childNode instanceof DBNDatabaseNode) {
                    DBSObject object = ((DBNDatabaseNode) childNode).getObject();
                    // Extr checks. In fact just for PosgreSQL like databases where everything is a table
                    if (object instanceof DBSEntity && !(object instanceof DBSSequence) && !(object instanceof DBSDataType)) {
                        entities.add((DBNDatabaseNode) childNode);
                    }
                }
            }
        }
    }

    private void handleRefTableSelect(DBNDatabaseNode refTableNode) {
        if (refTableNode != null) {
            DBSObject object = refTableNode.getObject();
            if (object instanceof DBSEntity) {
                curRefTable = (DBSEntity) refTableNode.getObject();
            }
            if (fkNameText != null) {
                fkNameText.setText("FK_" + refTableNode.getObject().getName());
            }
        } else {
            curRefTable = null;
        }
        handleRefTableSelect();
    }

    private void handleRefTableSelect() {
        uniqueKeyCombo.removeAll();

        try {
            curConstraints = new ArrayList<>();
            curConstraint = null;
            if (curRefTable != null) {
                final DBSEntity refTable = curRefTable;
                UIUtils.runInProgressService(monitor -> {
                    try {
                        // Cache own table columns
                        foreignKey.getParentObject().getAttributes(monitor);

                        // Cache ref table columns
                        refTable.getAttributes(monitor);

                        // Get constraints
                        final Collection<? extends DBSEntityConstraint> constraints = DBVUtils.getAllConstraints(monitor, refTable);
                        if (!CommonUtils.isEmpty(constraints)) {
                            for (DBSEntityConstraint constraint : constraints) {
                                if (constraint.getConstraintType().isUnique() && constraint instanceof DBSEntityReferrer) {
                                    if (isValidRefConstraint(monitor, (DBSEntityReferrer) constraint)) {
                                        curConstraints.add(constraint);
                                    }
                                }
                            }
                        }

                        if (refTable instanceof DBSTable) {
                            // Get indexes
                            final Collection<? extends DBSTableIndex> indexes = ((DBSTable) refTable).getIndexes(monitor);
                            if (!CommonUtils.isEmpty(indexes)) {
                                for (DBSTableIndex constraint : indexes) {
                                    if (constraint.isUnique() &&
                                        isConstraintIndex(monitor, curConstraints, constraint) &&
                                        isValidRefConstraint(monitor, constraint)) {
                                        curConstraints.add(constraint);
                                    }
                                }
                            }
                        }
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
                if (CommonUtils.isEmpty(curConstraints) && enableCustomKeys && !CommonUtils.isEmpty(refAttributes)) {
                    // We have ref attrs specified - create virtual unique key automatically
                    DBVEntity vRefEntity = DBVUtils.getVirtualEntity(curRefTable, true);
                    assert vRefEntity != null;
                    DBVEntityConstraint vUniqueKey = new DBVEntityConstraint(
                        vRefEntity,
                        DBSEntityConstraintType.VIRTUAL_KEY,
                        vRefEntity.getName() + "_VK");
                    for (DBSEntityAttribute refAttr : refAttributes) {
                        vUniqueKey.addAttribute(refAttr.getName());
                    }
                    vRefEntity.addConstraint(vUniqueKey, true);
                    curConstraints.add(vUniqueKey);
                }
            }

            for (DBSEntityConstraint constraint : curConstraints) {
                uniqueKeyCombo.add(constraint.getName() + " (" + constraint.getConstraintType().getLocalizedName() + ")");
            }
            if (uniqueKeyCombo.getItemCount() == 0) {
                if (curRefTable == null) {
                    uniqueKeyCombo.add("<No reference table selected>");
                } else {
                    uniqueKeyCombo.add("<No unique keys in table '" + DBUtils.getObjectFullName(curRefTable, DBPEvaluationContext.UI) + "'>");
                }
                uniqueKeyCombo.select(0);
                curConstraint = null;

            } else {
                uniqueKeyCombo.select(0);
                //uniqueKeyCombo.setEnabled(curConstraints.size() > 1);
                curConstraint = curConstraints.get(0);
            }
            if (enableCustomKeys) {
                enableCurConstraintEdit();
            }

        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(EditorsMessages.dialog_struct_edit_fk_error_load_constraints_title, EditorsMessages.dialog_struct_edit_fk_error_load_constraints_message, e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }
        handleUniqueKeySelect();
        updatePageState();
    }

    private void enableCurConstraintEdit() {
        if (curConstraint instanceof DBVEntityConstraint) {
            customUKButton.setEnabled(true);
            customUKButton.setText("Edit");
        } else {
            boolean hasLogicalConstraint = false;
            for (DBSEntityConstraint constraint : curConstraints) {
                if (constraint instanceof DBVEntityConstraint) {
                    hasLogicalConstraint = true;
                    break;
                }
            }
            customUKButton.setText("Create");
            customUKButton.setEnabled(!hasLogicalConstraint);
        }
    }

    private boolean isConstraintIndex(DBRProgressMonitor monitor, List<DBSEntityConstraint> constraints, DBSTableIndex index) throws DBException {
        List<? extends DBSTableIndexColumn> iAttrs = index.getAttributeReferences(monitor);

        for (DBSEntityConstraint constraint : constraints) {
            if (constraint instanceof DBSEntityReferrer) {
                List<? extends DBSEntityAttributeRef> cAttrs = ((DBSEntityReferrer) constraint).getAttributeReferences(monitor);
                if (CommonUtils.equalObjects(iAttrs, cAttrs)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isValidRefConstraint(DBRProgressMonitor monitor, DBSEntityReferrer constraint) throws DBException {
        if (!CommonUtils.isEmpty(refAttributes)) {
            // Constraint must include ref attributes
            for (DBSEntityAttribute refAttr : refAttributes) {
                if (DBUtils.getConstraintAttribute(monitor, constraint, refAttr) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private void handleUniqueKeySelect()
    {
        fkColumns.clear();
        ownColumns = null;
        columnsTable.removeAll();
        int ukSelectionIndex = uniqueKeyCombo.getSelectionIndex();
        if ((curConstraints.isEmpty() || ukSelectionIndex < 0) && !enableCustomKeys) {
            return;
        }
        if (ukSelectionIndex >= 0) {
            curConstraint = curConstraints.isEmpty() ? null : curConstraints.get(ukSelectionIndex);
        }
        DBSEntity curEntity = foreignKey.getParentObject();
        DBRProgressMonitor monitor = new VoidProgressMonitor();
        try {
            Collection<? extends DBSEntityAttribute> tmpColumns = curEntity.getAttributes(monitor);
            ownColumns = tmpColumns == null ?
                Collections.<DBSTableColumn>emptyList() :
                new ArrayList<>(getValidAttributes(curEntity));

            if (curConstraint instanceof DBSEntityReferrer) {
                // Read column nodes with void monitor because we already cached them above
                for (DBSEntityAttributeRef pkColumn : ((DBSEntityReferrer)curConstraint).getAttributeReferences(monitor)) {
                    DBSEntityAttribute pkAttribute = pkColumn.getAttribute();
                    if (pkAttribute == null) {
                        log.debug("Constraint " + curConstraint.getName() + " column attribute not found");
                        continue;
                    }
                    FKColumnInfo fkColumnInfo = new FKColumnInfo(pkAttribute);
                    // Try to find matched column in own table
                    if (!CommonUtils.isEmpty(ownColumns)) {
                        for (DBSEntityAttribute ownColumn : ownColumns) {
                            if (ownColumn.getName().equals(pkAttribute.getName()) && curEntity != pkAttribute.getParentObject()) {
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
                    item.setText(2, pkAttribute.getName());
                    item.setImage(2, getColumnIcon(pkAttribute));
                    item.setText(3, pkAttribute.getFullTypeName());
                    item.setData(fkColumnInfo);
                }
            } else if (enableCustomKeys && curRefTable != null) {
                // TODO: direct custom foreign key creation. show columns list
/*
                for (DBSEntityAttribute attr : CommonUtils.safeCollection(curEntity.getAttributes(monitor))) {
                    FKColumnInfo fkColumnInfo = new FKColumnInfo(null);
                    fkColumnInfo.ownColumn = attr;

                    TableItem item = new TableItem(columnsTable, SWT.NONE);
                    item.setText(0, fkColumnInfo.ownColumn.getName());
                    item.setImage(0, getColumnIcon(fkColumnInfo.ownColumn));
                    item.setText(1, fkColumnInfo.ownColumn.getFullTypeName());
                    item.setText(2, "");
                    item.setImage(2, DBeaverIcons.getImage(DBIcon.TYPE_UNKNOWN));
                    item.setText(3, "");
                    item.setData(fkColumnInfo);
                }
*/
            }
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(
                EditorsMessages.dialog_struct_edit_fk_error_load_constraint_columns_title,
                EditorsMessages.dialog_struct_edit_fk_error_load_constraint_columns_message, e);
        }
        if (enableCustomKeys) {
            enableCurConstraintEdit();
        }
        UIUtils.packColumns(columnsTable, true);
    }

    private static List<DBSEntityAttribute> getValidAttributes(DBSEntity table) throws DBException {
        List<DBSEntityAttribute> result = new ArrayList<>();
        for (DBSEntityAttribute attr : table.getAttributes(new VoidProgressMonitor())) {
            if (!DBUtils.isHiddenObject(attr) && !DBUtils.isPseudoAttribute(attr)) {
                result.add(attr);
            }
        }
        return result;
    }

    private Image getColumnIcon(DBSEntityAttribute column)
    {
        return DBeaverIcons.getImage(DBValueFormatting.getObjectImage(column));
    }

    @Override
    public boolean isPageComplete() {
        if (fkColumns.isEmpty()) {
            return false;
        }
        for (FKColumnInfo col : fkColumns) {
            if (col.ownColumn == null || col.refColumn == null) {
                return false;
            }
        }
        return true;
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

    public DBSEntityConstraint getUniqueConstraint()
    {
        return curConstraint;
    }

    private class ColumnsMouseListener extends MouseAdapter {
        private final TableEditor tableEditor;
        private final Table columnsTable;

        ColumnsMouseListener(TableEditor tableEditor, Table columnsTable)
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
            FKColumnInfo fkInfo = (FKColumnInfo) item.getData();
            int columnIndex = UIUtils.getColumnAtPos(item, e.x, e.y);
            if (fkInfo.ownColumn == null && columnIndex != 0) {
                return;
            }
            if (fkInfo.refColumn == null && columnIndex != 2) {
                return;
            }

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
                        assignForeignKeyRefConstraint(fkInfo, columnsCombo, item);
                    }
                }
            });
            columnsCombo.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e)
                {
                    if (columnsCombo.getSelectionIndex() >= 0) {
                        assignForeignKeyRefConstraint(fkInfo, columnsCombo, item);
                    }
                    disposeOldEditor();
                }
            });
            tableEditor.setEditor(columnsCombo, item, 0);
        }
    }

    private void assignForeignKeyRefConstraint(FKColumnInfo fkInfo, CCombo columnsCombo, TableItem item) {
        fkInfo.ownColumn = ownColumns.get(columnsCombo.getSelectionIndex());
        item.setText(0, fkInfo.ownColumn.getName());
        item.setImage(0, getColumnIcon(fkInfo.ownColumn));
        item.setText(1, fkInfo.ownColumn.getFullTypeName());
        updatePageState();
    }

    public boolean isEnabled() {
        return true;
    }

    public String getName() {
        return fkName;
    }

    protected boolean supportsCustomName() {
        return supportsCustomName;
    }

    public void setSupportsCustomName(boolean supportsCustomName) {
        this.supportsCustomName = supportsCustomName;
    }

    private void setSourceAttributes(Collection<? extends DBSEntityAttribute> srcAttributes) {
        this.sourceAttributes = CommonUtils.isEmpty(srcAttributes) ? null : new ArrayList<>(srcAttributes);
    }

    private void setReferenceAttributes(Collection<? extends DBSEntityAttribute> refAttributes) {
        this.refAttributes = CommonUtils.isEmpty(refAttributes) ? null : new ArrayList<>(refAttributes);
    }

    @Nullable
    public static DBVEntityForeignKey createVirtualForeignKey(@NotNull DBVEntity vEntity) {
        return createVirtualForeignKey(vEntity, null, new FKType[] {FK_TYPE_LOGICAL}, null, null);
    }

    @Nullable
    public static DBVEntityForeignKey createVirtualForeignKey(
        @NotNull DBVEntity vEntity,
        @Nullable DBSEntity refEntity,
        @Nullable FKType[] allowedKeyTypes,
        @Nullable Collection<? extends DBSEntityAttribute> srcAttributes,
        @Nullable Collection<? extends DBSEntityAttribute> refAttributes)
    {
        DBVEntityForeignKey virtualFK = new DBVEntityForeignKey(vEntity);
        EditForeignKeyPage editDialog = new EditForeignKeyPage(
            "Define virtual foreign keys",
            virtualFK,
            new DBSForeignKeyModifyRule[]{DBSForeignKeyModifyRule.NO_ACTION});
        editDialog.setEnableCustomKeys(true);
        if (allowedKeyTypes != null) {
            editDialog.setAllowedKeyTypes(allowedKeyTypes);
        }
        if (refEntity != null) {
            editDialog.setRefTable(refEntity);
        }
        if (srcAttributes != null) {
            editDialog.setSourceAttributes(srcAttributes);
        }
        if (refAttributes != null) {
            editDialog.setReferenceAttributes(refAttributes);
        }
        if (!editDialog.edit()) {
            return null;
        }
        // Save
        try {
            virtualFK.setReferencedConstraint(new VoidProgressMonitor(), editDialog.getUniqueConstraint());
        } catch (DBException e1) {
            log.error(e1);
            return null;
        }
        List<DBVEntityForeignKeyColumn> columns = new ArrayList<>();
        for (FKColumnInfo tableColumn : editDialog.getColumns()) {
            columns.add(
                new DBVEntityForeignKeyColumn(
                    virtualFK, tableColumn.getOwnColumn().getName(), tableColumn.getRefColumn().getName()));
        }
        virtualFK.setAttributes(columns);
        vEntity.addForeignKey(virtualFK);
        return virtualFK;
    }

}
