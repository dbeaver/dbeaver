/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.dnd.TextTransfer;
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
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEStructEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
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
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.editors.object.internal.ObjectEditorMessages;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorContentProvider;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTreeFilter;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTreeFilterObjectType;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;

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
    private static final String NEW_COLUMN_LABEL = "<new>";
    private static final String SELECT_COLUMN_LABEL = "<click>";

    private final DBSForeignKeyModifyRule[] supportedModifyRules;
    private final DBSEntityAssociation foreignKey;
    private final DBNModel navigatorModel;
    private DBSEntity curRefTable;
    private List<DBSEntityConstraint> curConstraints;
    private DBNDatabaseNode ownerTableNode, ownerContainerNode;
    private DatabaseNavigatorTree tableList;
    private Combo uniqueKeyCombo;
    private Text fkNameText;
    private Table columnsTable;
    private Button customUKButton;

    private String fkName;

    private DBSEntityConstraint curConstraint;
    private List<? extends DBSEntityAttribute> ownAttributes;
    private List<DBSEntityAttribute> sourceAttributes;
    private List<DBSEntityAttribute> refAttributes;
    private final List<FKColumnInfo> fkColumns = new ArrayList<>();
    private DBSForeignKeyModifyRule onDeleteRule;
    private DBSForeignKeyModifyRule onUpdateRule;

    private boolean enableCustomKeys = false;
    private boolean supportsCustomName = false;

    private FKType[] allowedKeyTypes = new FKType[]{FK_TYPE_PHYSICAL};
    private FKType preferredKeyType = FK_TYPE_PHYSICAL;
    private FKType selectedKeyType = FK_TYPE_PHYSICAL;

    private final List<Control> physicalKeyComponents = new ArrayList<>();
    private Button columnOptionsButton;

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
        private String customName;
        public boolean customNotNull;

        FKColumnInfo(DBSEntityAttribute refColumn) {
            this.refColumn = refColumn;
        }

        public <T extends DBSEntityAttribute> T getRefColumn() {
            return (T) refColumn;
        }

        public <T extends DBSEntityAttribute> T getOwnColumn() {
            return (T) ownColumn;
        }

        public <T extends DBSEntityAttribute> T getOrCreateOwnColumn(
            @NotNull DBRProgressMonitor monitor,
            @Nullable DBECommandContext commandContext,
            @NotNull DBSEntity entity
        ) throws DBException {
            if (ownColumn != null) {
                return (T)ownColumn;
            }
            if (CommonUtils.isEmpty(customName)) {
                throw new DBException("Custom column name not specified");
            }

            DBEStructEditor<?> entityEditor = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(
                entity.getClass(),
                DBEStructEditor.class);
            if (entityEditor != null) {
                for (Class<?> childType : entityEditor.getChildTypes()) {
                    if (DBSEntityAttribute.class.isAssignableFrom(childType)) {
                        DBEObjectMaker<?,?> objectManager = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(
                            childType, DBEObjectMaker.class);
                        if (objectManager != null) {
                            Map<String, Object> options = new LinkedHashMap<>();
                            options.put(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true);
                            DBSObject newColumn = objectManager.createNewObject(monitor, commandContext, entity, null, options);
                            if (newColumn instanceof DBSEntityAttribute attr) {
                                setNewColumnDataType(newColumn);
                                if (newColumn instanceof DBPNamedObject2 no) {
                                    no.setName(customName);
                                    ownColumn = attr;
                                    return (T) attr;
                                }
                            }
                        }
                    }
                }
            }
            throw new DBException("Cannot create new column in table '" + DBUtils.getObjectFullName(entity, DBPEvaluationContext.UI) + "'");
        }

        private void setNewColumnDataType(DBSObject newColumn) throws DBException {
            if (newColumn instanceof DBSTypedObjectExt2 toe) {
                toe.setRequired(customNotNull);
                toe.setMaxLength(refColumn.getMaxLength());
                toe.setScale(refColumn.getScale());
                toe.setPrecision(refColumn.getPrecision());
            }
            if (refColumn != null) {
                // Set new column data type
                if (newColumn instanceof DBSTypedObjectExt4 toe4 && refColumn instanceof DBSTypedObjectEx refTO) {
                    DBSDataType refDataType = refTO.getDataType();
                    if (refDataType instanceof DBSDataTypeSerial dts && dts.isSerialDataType()) {
                        DBSDataType sbDataType = dts.getBaseDataType();
                        if (sbDataType == null) {
                            log.debug("Base data type for serial data type '" + dts.getFullTypeName() + "'");
                        } else {
                            refDataType = sbDataType;
                        }
                    }
                    toe4.setDataType(refDataType);
                } else if (newColumn instanceof DBSTypedObjectExt3 toe3) {
                    toe3.setFullTypeName(refColumn.getFullTypeName());
                } else if (newColumn instanceof DBSTypedObjectExt2 toe) {
                    toe.setTypeName(refColumn.getTypeName());
                }
            }
        }

        public String getCustomName() {
            return customName;
        }
    }

    public EditForeignKeyPage(
        String title,
        DBSEntityAssociation foreignKey,
        DBSForeignKeyModifyRule[] supportedModifyRules,
        Map<String, Object> options
    ) {
        super(title);
        navigatorModel = foreignKey.getDataSource().getContainer().getProject().getNavigatorModel();
        assert navigatorModel != null;

        this.foreignKey = foreignKey;
        this.ownerTableNode = navigatorModel.findNode(foreignKey.getParentObject());
        this.supportedModifyRules = supportedModifyRules;

        if (ownerTableNode == null) {
            try {
                if (foreignKey.getParentObject() instanceof DBVEntity) {
                    DBSEntity realEntity = ((DBVEntity) foreignKey.getParentObject()).getRealEntity(new VoidProgressMonitor());
                    if (realEntity != null) {
                        ownerTableNode = navigatorModel.getNodeByObject(realEntity);
                        if (ownerTableNode == null) {
                            try {
                                UIUtils.runInProgressDialog(monitor ->
                                    ownerTableNode = navigatorModel.getNodeByObject(
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
            setTitle(NLS.bind(ObjectEditorMessages.dialog_struct_edit_fk_title,
                title,
                ownerTableNode.getNodeDisplayName()));
        }

        if (!(foreignKey.getParentObject() instanceof DBVEntity)) {
            DBSEntityConstraint refConstraint = foreignKey.getReferencedConstraint();
            if (refConstraint != null) {
                curRefTable = refConstraint.getParentObject();
                curConstraint = refConstraint;
            }
        }

        sourceAttributes = (List<DBSEntityAttribute>) options.get(SQLForeignKeyManager.OPTION_OWN_ATTRIBUTES);
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
    public DBSObject getObject() {
        return foreignKey;
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
                ObjectEditorMessages.dialog_struct_edit_fk_label_table, DBUtils.getObjectFullName(foreignKey.getParentObject(), DBPEvaluationContext.UI), SWT.READ_ONLY | SWT.BORDER);

            if (allowedKeyTypes.length > 1) {
                UIUtils.createControlLabel(tableGroup, ObjectEditorMessages.dialog_struct_edit_fk_label_key_type);
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
                    ObjectEditorMessages.dialog_struct_edit_fk_label_ref_table, DBUtils.getObjectFullName(curRefTable, DBPEvaluationContext.UI), SWT.READ_ONLY | SWT.BORDER);
            }
        }

        if (curRefTable == null) {
            DBNNode containerNode = ownerTableNode == null ? null : ownerTableNode.getParentNode();
            while (containerNode instanceof DBNDatabaseFolder) {
                containerNode = containerNode.getParentNode();
            }

            DBNNode rootNode = containerNode == null ? navigatorModel.getRoot() : containerNode;
            DBNNode tablesNode = rootNode instanceof DBNDatabaseNode dbNode ? getTablesNode(dbNode) : rootNode;

            UIUtils.createControlLabel(panel, ObjectEditorMessages.dialog_struct_edit_fk_label_ref_table);
            tableList = new DatabaseNavigatorTree(
                panel,
                tablesNode,
                SWT.BORDER | SWT.FULL_SELECTION,
                false,
                new DatabaseNavigatorTreeFilter()) {
                @NotNull
                @Override
                protected DatabaseNavigatorContentProvider createContentProvider(boolean showRoot) {
                    return new DatabaseNavigatorContentProvider(this, showRoot) {
                        @Override
                        public boolean hasChildren(Object parent) {
                            // Do not show anything below tables
                            if (parent instanceof DBNDatabaseNode dbnNode && dbnNode.getObject() instanceof DBSEntity) {
                                return false;
                            }
                            return super.hasChildren(parent);
                        }
                    };
                }
            };
            tableList.getViewer().addFilter(new ViewerFilter() {
                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element) {
                    return element instanceof DBNDatabaseNode dbnNode && dbnNode.getObject() instanceof DBSEntity;
                }
            });
            tableList.setFilterObjectType(DatabaseNavigatorTreeFilterObjectType.table);
            NavigatorUtils.createContextMenu(null, tableList.getViewer(), manager -> {
                manager.add(new Action(UIMessages.ui_properties_tree_viewer_action_copy_name) {
                    @Override
                    public void run() {
                        Object firstElement = tableList.getViewer().getStructuredSelection().getFirstElement();
                        if (firstElement instanceof DBNNode node) {
                            UIUtils.setClipboardContents(
                                getShell().getDisplay(), TextTransfer.getInstance(), node.getNodeDisplayName());
                        }
                    }
                });
            });
            final GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 500;
            gd.heightHint = 150;
            tableList.setLayoutData(gd);
            tableList.getViewer().addSelectionChangedListener(
                event -> handleRefTableSelect((DBNDatabaseNode) event.getStructuredSelection().getFirstElement()));
            if (tablesNode instanceof DBNDatabaseNode dbnNode) {
                loadTableList(dbnNode);
            }
        }

        final Composite pkGroup = UIUtils.createComposite(panel, enableCustomKeys ? 3 : 2);
        {
            pkGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            uniqueKeyCombo = UIUtils.createLabelCombo(pkGroup, ObjectEditorMessages.dialog_struct_edit_fk_combo_unik, SWT.DROP_DOWN | SWT.READ_ONLY);
            //uniqueKeyCombo.setEnabled(false);
            uniqueKeyCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    handleUniqueKeySelect();
                    updatePageState();
                }
            });
            if (enableCustomKeys) {
                customUKButton = UIUtils.createDialogButton(
                    pkGroup,
                    ObjectEditorMessages.dialog_struct_edit_fk_custom_uk_button_create,
                    new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            defineRefTableConstraint();
                        }
                    });
                customUKButton.setEnabled(false);
            }

            if (supportsCustomName()) {
                fkNameText = UIUtils.createLabelText(pkGroup, ObjectEditorMessages.dialog_struct_edit_fk_name, "");
                fkNameText.addModifyListener(e -> fkName = fkNameText.getText());
            }
        }
        {
            UIUtils.createControlLabel(panel, ObjectEditorMessages.dialog_struct_edit_fk_label_columns);
            columnsTable = new Table(panel, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
            columnsTable.setHeaderVisible(true);
            columnsTable.setLinesVisible(true);
            final GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 500;
            gd.heightHint = 100;
            columnsTable.setLayoutData(gd);

            UIUtils.createTableColumn(columnsTable, SWT.LEFT, ObjectEditorMessages.dialog_struct_edit_fk_column_column);
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, ObjectEditorMessages.dialog_struct_edit_fk_column_col_type);
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, ObjectEditorMessages.dialog_struct_edit_fk_column_ref_col);
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, ObjectEditorMessages.dialog_struct_edit_fk_column_ref_col_type);

            final TableEditor tableEditor = new TableEditor(columnsTable);
            tableEditor.horizontalAlignment = SWT.CENTER;
            tableEditor.verticalAlignment = SWT.TOP;
            tableEditor.grabHorizontal = true;
            tableEditor.minimumWidth = 50;

            columnsTable.addMouseListener(new ColumnsMouseListener(tableEditor, columnsTable));
            columnsTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    boolean hasCustomColumn = false;
                    FKColumnInfo fki = getSelectedColumnInfo();
                    if (fki != null && fki.ownColumn == null && fki.customName != null) {
                        hasCustomColumn = true;
                    }
                    columnOptionsButton.setEnabled(hasCustomColumn);
                }
            });
        }

        boolean supportModifyRules = !ArrayUtils.isEmpty(supportedModifyRules);

        Composite settingsPanel = UIUtils.createComposite(panel, supportModifyRules ? 2 : 1);
        settingsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (supportModifyRules) {
            final Composite cascadeGroup = UIUtils.createComposite(settingsPanel, 4);
            {
                // Cascades
                cascadeGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                final Combo onDeleteCombo = UIUtils.createLabelCombo(cascadeGroup, ObjectEditorMessages.dialog_struct_edit_fk_combo_on_delete, SWT.DROP_DOWN | SWT.READ_ONLY);
                onDeleteCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                final Combo onUpdateCombo = UIUtils.createLabelCombo(cascadeGroup, ObjectEditorMessages.dialog_struct_edit_fk_combo_on_update, SWT.DROP_DOWN | SWT.READ_ONLY);
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
        {
            final Composite columnGroup = UIUtils.createComposite(settingsPanel, 1);
            columnOptionsButton = UIUtils.createDialogButton(columnGroup, "Column options ...", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    FKColumnInfo fki = getSelectedColumnInfo();
                    if (fki != null) {
                        editColumnOptions(fki);
                    }
                }
            });
            columnOptionsButton.setEnabled(false);
        }

        if (tableList != null) {
            tableList.getViewer().getTree().setFocus();
        }

        setErrorMessage("Select reference table");

        return panel;
    }

    private void editColumnOptions(FKColumnInfo fkColumnInfo) {
        FKColumnOptionsDialog dialog = new FKColumnOptionsDialog(getShell(), fkColumnInfo);
        if (dialog.open() == IDialogConstants.OK_ID) {
            fkColumnInfo.customName = dialog.columnName;
            fkColumnInfo.customNotNull = dialog.columnRequired;
            TableItem item = columnsTable.getSelection()[0];
            item.setText(0, "<" + fkColumnInfo.customName + ">");
        }
    }

    private FKColumnInfo getSelectedColumnInfo() {
        TableItem[] selection = columnsTable.getSelection();
        if (selection.length == 1) {
            if (selection[0].getData() instanceof FKColumnInfo fki) {
                return fki;
            }
        }
        return null;
    }

    private void updateControlsVisibility() {
        boolean pkVisible = selectedKeyType.isPhysical();
        for (Control pkc : physicalKeyComponents) {
            pkc.setVisible(pkVisible);
            if (pkc.getLayoutData() instanceof GridData) {
                ((GridData) pkc.getLayoutData()).exclude = !pkVisible;
            }
        }

        ((Composite) getControl()).layout(true, true);
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
            ObjectEditorMessages.dialog_struct_edit_fk_page_title,
            constraint);
        if (page.edit()) {
            constraint.setAttributes(page.getSelectedAttributes());
            handleRefTableSelect(navigatorModel.getNodeByObject(curRefTable));
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
                    return ((DBNDatabaseNode) element).getNodeDisplayName();
                }
            };

            Label controlLabel = UIUtils.createControlLabel(
                tableGroup, ObjectEditorMessages.edit_foreign_key_page_create_schema_container);
            final CSmartCombo<DBNDatabaseNode> schemaCombo = new CSmartCombo<>(tableGroup, SWT.BORDER, labelProvider);
            schemaCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            DBNDatabaseNode selectedNode = null;
            for (DBNNode node : schemaContainerNode.getChildren(new VoidProgressMonitor())) {
                if (node instanceof DBNDatabaseNode dbNode && dbNode.getObject() instanceof DBSObjectContainer) {
                    schemaCombo.addItem(dbNode);
                    if (dbNode.getObject() == foreignKey.getParentObject().getParentObject()) {
                        selectedNode = dbNode;
                    }
                }
            }
            List<DBNDatabaseNode> allContainers = schemaCombo.getItems();
            if (!allContainers.isEmpty()) {
                String nodeType = allContainers.get(0).getMeta().getNodeTypeLabel(foreignKey.getDataSource(), null);
                if (!CommonUtils.isEmpty(nodeType)) {
                    controlLabel.setText(nodeType);
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
                    DBNDatabaseNode schemaNode = schemaCombo.getSelectedItem();
                    DBNDatabaseNode newContainerNode = getTablesNode(schemaNode);
                    if (newContainerNode != null) {
                        loadTableList(newContainerNode);
                    }
                }
            });
        }
    }

    @Nullable
    private DBNDatabaseNode getTablesNode(DBNDatabaseNode schemaNode) {
        DBNDatabaseNode newContainerNode = null;
        DBXTreeNode tableContainerMeta = ((DBNDatabaseNode) ownerTableNode.getParentNode()).getMeta();
        if (schemaNode.getMeta() == tableContainerMeta) {
            newContainerNode = schemaNode;
        } else {
            try {
                boolean found = false;
                for (DBNNode child : schemaNode.getChildren(new VoidProgressMonitor())) {
                    if (child instanceof DBNDatabaseFolder dbNode) {
                        for (DBXTreeNode childItem : dbNode.getMeta().getChildren(child)) {
                            if (childItem instanceof DBXTreeItem dbxItem) {
                                Class<?> childrenClass = schemaNode.getChildrenClass(dbxItem);
                                if (childrenClass != null && DBSEntity.class.isAssignableFrom(childrenClass)) {
                                    newContainerNode = dbNode;
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (found) {
                        break;
                    }
                }
            } catch (DBException e1) {
                log.debug(e1);
                // Shouldn't be here
            }
        }
        return newContainerNode;
    }

    private void createContainerSelector(Composite tableGroup) throws DBException {
        ObjectContainerSelectorPanel containerPanel = new ObjectContainerSelectorPanel(
            tableGroup,
            this.getOwnerProject(),
            CONTAINER_LOGICAL_FK,
            ObjectEditorMessages.edit_foreign_key_page_create_container_reference_table_container,
            ObjectEditorMessages.edit_foreign_key_page_create_container_select_reference_table_container) {
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
                        containerObject = ((DBNDatabaseNode) containerNode).getObject();
                    } else {
                        containerObject = null;
                    }
                } else {
                    containerObject = foreignKey.getParentObject();
                }
                return ownerContainerNode = getNodeByContainerObject(containerObject);
            }

            @Override
            protected void setSelectedNode(DBNDatabaseNode node) {
                ownerContainerNode = node;
                if (ownerContainerNode == null) {
                    setContainerInfo(null);
                } else {
                    setContainerInfo(node);
                    DBNDatabaseNode tablesNode = getTablesNode(ownerContainerNode);
                    loadTableList(tablesNode);
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

    @Nullable
    private DBPProject getOwnerProject() {
        if (foreignKey != null) {
            DBNDatabaseNode node = getNodeByContainerObject(foreignKey.getParentObject());
            if (node != null) {
                return node.getOwnerProject();
            }
        }
        if (ownerTableNode != null) {
            return ownerTableNode.getOwnerProject();
        }
        return null;
    }

    @Nullable
    private DBNDatabaseNode getNodeByContainerObject(@Nullable DBSObject containerObject) {
        if (containerObject != null && containerObject.getParentObject() instanceof DBSObjectContainer) {
            containerObject = containerObject.getParentObject();
        }
        if (containerObject instanceof DBVContainer) {
            try {
                containerObject = ((DBVContainer) containerObject).getRealContainer(new VoidProgressMonitor());
            } catch (DBException e) {
                log.error("Error getting real object container", e);
            }
        }
        return navigatorModel.getNodeByObject(containerObject);
    }

    private void loadTableList(DBNDatabaseNode newContainerNode) {
        tableList.setInput(newContainerNode);
    }

    private void handleRefTableSelect(DBNDatabaseNode refTableNode) {
        if (refTableNode != null) {
            DBSObject object = refTableNode.getObject();
            if (object instanceof DBSEntity) {
                curRefTable = (DBSEntity) refTableNode.getObject();
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

                        if (refTable instanceof DBSTable dbsTable) {
                            // Get indexes
                            final Collection<? extends DBSTableIndex> indexes = dbsTable.getIndexes(monitor);
                            if (!CommonUtils.isEmpty(indexes)) {
                                for (DBSTableIndex index : indexes) {
                                    if (index.isUnique() &&
                                        !isConstraintIndex(monitor, curConstraints, index) &&
                                        isValidRefConstraint(monitor, index)) {
                                        curConstraints.add(index);
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
                setErrorMessage(uniqueKeyCombo.getText());

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
            DBWorkbench.getPlatformUI().showError(ObjectEditorMessages.dialog_struct_edit_fk_error_load_constraints_title, ObjectEditorMessages.dialog_struct_edit_fk_error_load_constraints_message, e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }

        if (fkNameText != null) {
            String fkAutoName = SQLForeignKeyManager.generateConstraintName((DBSEntity) ownerTableNode.getObject(), curConstraint);
            fkNameText.setMessage(fkAutoName);
        }

        handleUniqueKeySelect();
        updatePageState();
    }

    private void enableCurConstraintEdit() {
        if (curConstraint instanceof DBVEntityConstraint) {
            customUKButton.setEnabled(true);
            customUKButton.setText("Edit");
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
        List<? extends DBSEntityAttribute> iAttrs = Objects.requireNonNull(index.getAttributeReferences(monitor))
            .stream().map(DBSEntityAttributeRef::getAttribute).toList();

        for (DBSEntityConstraint constraint : constraints) {
            if (constraint instanceof DBSEntityReferrer referrer) {
                List<? extends DBSEntityAttribute> cAttrs = Objects.requireNonNull(referrer.getAttributeReferences(monitor))
                    .stream().map(DBSEntityAttributeRef::getAttribute).toList();
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

    private void handleUniqueKeySelect() {
        fkColumns.clear();
        ownAttributes = null;
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
            ownAttributes = tmpColumns == null ?
                Collections.<DBSTableColumn>emptyList() :
                new ArrayList<>(getValidAttributes(curEntity));

            if (curConstraint instanceof DBSEntityReferrer) {
                // Read column nodes with void monitor because we already cached them above
                List<? extends DBSEntityAttributeRef> attributeReferences =
                    CommonUtils.safeList(((DBSEntityReferrer) curConstraint).getAttributeReferences(monitor));
                for (int i = 0; i < attributeReferences.size(); i++) {
                    DBSEntityAttributeRef pkColumn = attributeReferences.get(i);
                    DBSEntityAttribute pkAttribute = pkColumn.getAttribute();
                    if (pkAttribute == null) {
                        log.debug("Constraint " + curConstraint.getName() + " column attribute not found");
                        continue;
                    }
                    FKColumnInfo fkColumnInfo = new FKColumnInfo(pkAttribute);
                    if (!CommonUtils.isEmpty(sourceAttributes) && sourceAttributes.size() > i) {
                        fkColumnInfo.ownColumn = sourceAttributes.get(i);
                    }
                    if (fkColumnInfo.ownColumn == null) {
                        // Try to find matched column in own table
                        if (!CommonUtils.isEmpty(ownAttributes)) {
                            for (DBSEntityAttribute ownColumn : ownAttributes) {
                                if (ownColumn.getName().equals(pkAttribute.getName()) && curEntity != pkAttribute.getParentObject()) {
                                    fkColumnInfo.ownColumn = ownColumn;
                                    break;
                                }
                            }
                        }
                    }
                    fkColumns.add(fkColumnInfo);

                    TableItem item = new TableItem(columnsTable, SWT.NONE);
                    if (fkColumnInfo.ownColumn != null) {
                        item.setText(0, fkColumnInfo.ownColumn.getName());
                        item.setImage(0, getColumnIcon(fkColumnInfo.ownColumn));
                        item.setText(1, fkColumnInfo.ownColumn.getFullTypeName());
                    } else {
                        item.setText(0, SELECT_COLUMN_LABEL);
                        item.setText(1, "");
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
                ObjectEditorMessages.dialog_struct_edit_fk_error_load_constraint_columns_title,
                ObjectEditorMessages.dialog_struct_edit_fk_error_load_constraint_columns_message, e);
        }
        if (enableCustomKeys) {
            enableCurConstraintEdit();
        }
        verifyTableColumns();
        UIUtils.packColumns(columnsTable, true);
    }

    private void verifyTableColumns() {
        String errorMessage = null;
        for (TableItem item : columnsTable.getItems()) {
            FKColumnInfo fkColumnInfo = (FKColumnInfo) item.getData();
            if (fkColumnInfo.ownColumn == null && fkColumnInfo.customName == null) {
                errorMessage = "You have to specify column for '" + fkColumnInfo.refColumn.getName() + "'";
                break;
            }
        }
        setErrorMessage(errorMessage);
    }

    private static List<DBSEntityAttribute> getValidAttributes(DBSEntity table) throws DBException {
        List<DBSEntityAttribute> result = new ArrayList<>();
        for (DBSEntityAttribute attr : CommonUtils.safeList(table.getAttributes(new VoidProgressMonitor()))) {
            if (!DBUtils.isHiddenObject(attr) && !DBUtils.isPseudoAttribute(attr)) {
                result.add(attr);
            }
        }
        return result;
    }

    private Image getColumnIcon(DBSEntityAttribute column) {
        return DBeaverIcons.getImage(DBValueFormatting.getObjectImage(column));
    }

    @Override
    public boolean isPageComplete() {
        if (fkColumns.isEmpty()) {
            return false;
        }
        for (FKColumnInfo col : fkColumns) {
            if ((col.ownColumn == null && col.customName == null) || col.refColumn == null) {
                return false;
            }
        }
        return true;
    }

    public List<FKColumnInfo> getColumns() {
        return fkColumns;
    }

    public DBSForeignKeyModifyRule getOnDeleteRule() {
        return onDeleteRule;
    }

    public DBSForeignKeyModifyRule getOnUpdateRule() {
        return onUpdateRule;
    }

    public DBSEntityConstraint getUniqueConstraint() {
        return curConstraint;
    }

    private class ColumnsMouseListener extends MouseAdapter {
        private final TableEditor tableEditor;
        private final Table columnsTable;

        ColumnsMouseListener(TableEditor tableEditor, Table columnsTable) {
            this.tableEditor = tableEditor;
            this.columnsTable = columnsTable;
        }

        private void disposeOldEditor() {
            Control oldEditor = tableEditor.getEditor();
            if (oldEditor != null) oldEditor.dispose();
        }

        @Override
        public void mouseUp(MouseEvent e) {
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
            columnsCombo.add(NEW_COLUMN_LABEL);
            if (!CommonUtils.isEmpty(ownAttributes)) {
                for (DBSEntityAttribute ownColumn : ownAttributes) {
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
                public void widgetSelected(SelectionEvent e) {
                    if (columnsCombo.getSelectionIndex() >= 0) {
                        assignForeignKeyRefConstraint(fkInfo, columnsCombo, item);
                    }
                }
            });
            columnsCombo.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
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
        int selectionIndex = columnsCombo.getSelectionIndex();
        if (selectionIndex == 0) {
            // New auto column
            fkInfo.ownColumn = null;
            fkInfo.customName = fkInfo.refColumn.getName();
            item.setText(0, "<" + fkInfo.refColumn.getName() + ">");
            item.setImage(0, getColumnIcon(fkInfo.refColumn));
            item.setText(1, fkInfo.refColumn.getFullTypeName());
            columnOptionsButton.setEnabled(true);
        } else {
            fkInfo.ownColumn = ownAttributes.get(selectionIndex - 1);
            fkInfo.customName = null;
            item.setText(0, fkInfo.ownColumn.getName());
            item.setImage(0, getColumnIcon(fkInfo.ownColumn));
            item.setText(1, fkInfo.ownColumn.getFullTypeName());
            columnOptionsButton.setEnabled(false);
        }
        verifyTableColumns();
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
        return createVirtualForeignKey(vEntity, null, new FKType[]{FK_TYPE_LOGICAL}, null, null);
    }

    @Nullable
    public static DBVEntityForeignKey createVirtualForeignKey(
        @NotNull DBVEntity vEntity,
        @Nullable DBSEntity refEntity,
        @Nullable FKType[] allowedKeyTypes,
        @Nullable Collection<? extends DBSEntityAttribute> srcAttributes,
        @Nullable Collection<? extends DBSEntityAttribute> refAttributes) {
        DBVEntityForeignKey virtualFK = new DBVEntityForeignKey(vEntity);
        EditForeignKeyPage editDialog = new EditForeignKeyPage(
            ObjectEditorMessages.dialog_struct_edit_fk_virtual_page_title,
            virtualFK,
            new DBSForeignKeyModifyRule[]{DBSForeignKeyModifyRule.NO_ACTION},
            Collections.emptyMap());
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
            DBSEntityAttribute ownColumn = tableColumn.getOwnColumn();
            if (ownColumn == null) {
                log.error("Own column not specified");
                continue;
            }
            columns.add(
                new DBVEntityForeignKeyColumn(
                    virtualFK, ownColumn.getName(), tableColumn.getRefColumn().getName()));
        }
        virtualFK.setAttributes(columns);
        vEntity.addForeignKey(virtualFK);
        return virtualFK;
    }

    private static class FKColumnOptionsDialog extends BaseDialog {

        private final FKColumnInfo fkColumnInfo;
        private String columnName;
        private boolean columnRequired;

        public FKColumnOptionsDialog(Shell parentShell, FKColumnInfo fkColumnInfo) {
            super(parentShell, "New column options", null);
            this.fkColumnInfo = fkColumnInfo;
            this.columnName = fkColumnInfo.getCustomName();
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            Composite composite = super.createDialogArea(parent);
            Group group = UIUtils.createControlGroup(composite, "New column options", 2, GridData.FILL_HORIZONTAL, 300);
            Text columnNameText = UIUtils.createLabelText(group, "Column name", fkColumnInfo.getCustomName(), SWT.BORDER);
            columnNameText.addModifyListener(e -> columnName = columnNameText.getText());
            Button notNullCheck = UIUtils.createCheckbox(group, "Not Null", "Make new column required", false, 2);
            notNullCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    columnRequired = notNullCheck.getSelection();
                }
            });

            return composite;
        }
    }
}
