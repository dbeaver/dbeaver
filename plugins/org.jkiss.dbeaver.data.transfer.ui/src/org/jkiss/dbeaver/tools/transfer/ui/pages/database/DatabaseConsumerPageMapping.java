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
package org.jkiss.dbeaver.tools.transfer.ui.pages.database;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.tools.transfer.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.database.*;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.ui.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.SharedTextColors;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomComboBoxCellEditor;
import org.jkiss.dbeaver.ui.controls.ObjectContainerSelectorPanel;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseConsumerPageMapping extends ActiveWizardPage<DataTransferWizard> {

    private static final Log log = Log.getLog(DatabaseConsumerPageMapping.class);

    private static final String TARGET_NAME_BROWSE = "[browse]";
    private TreeViewer mappingViewer;
    private Button autoAssignButton;
    private ObjectContainerSelectorPanel containerPanel;

    private static abstract class MappingLabelProvider extends CellLabelProvider {
        @Override
        public void update(ViewerCell cell)
        {
        }
    }

    public DatabaseConsumerPageMapping() {
        super("Tables mapping");
        setTitle("Tables mapping");
        setDescription("Map tables and columns transfer");
    }

    private DatabaseConsumerSettings getDatabaseConsumerSettings() {
        return getWizard().getPageSettings(this, DatabaseConsumerSettings.class);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        final DatabaseConsumerSettings settings = getDatabaseConsumerSettings();

        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout gl = new GridLayout();
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        composite.setLayout(gl);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            // Target container
            // Use first source object as cur selection (it's better than nothing)
            containerPanel = new ObjectContainerSelectorPanel(composite,
                getWizard().getProject(),
                "container.data-transfer.database-consumer",
                DTMessages.data_transfer_db_consumer_target_container,
                DTMessages.data_transfer_db_consumer_choose_container)
            {
                @Nullable
                @Override
                protected DBNNode getSelectedNode() {
                    DBNNode selectedNode = settings.getContainerNode();
                    if (selectedNode == null && !settings.getDataMappings().isEmpty()) {
                        // Use first source object as cur selection (it's better than nothing)
                        DBSDataContainer firstSource = settings.getDataMappings().keySet().iterator().next();
                        selectedNode = DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(firstSource);
                        while (selectedNode != null) {
                            if (selectedNode instanceof DBSWrapper && ((DBSWrapper) selectedNode).getObject() instanceof DBSObjectContainer) {
                                break;
                            } else {
                                selectedNode = selectedNode.getParentNode();
                            }
                        }
                    }
                    return selectedNode;
                }

                @Override
                protected void setSelectedNode(DBNDatabaseNode node) {
                    settings.setContainerNode(node);
                    setContainerInfo(node);
                    // Reset mappings
                    for (DatabaseMappingContainer mappingContainer : settings.getDataMappings().values()) {
                        if (mappingContainer.getMappingType() != DatabaseMappingType.unspecified) {
                            try {
                                mappingContainer.refreshMappingType(getWizard().getRunnableContext(), DatabaseMappingType.unspecified);
                            } catch (DBException e1) {
                                log.error(e1);
                            }
                        }
                    }
                    mappingViewer.refresh();
                    updatePageCompletion();
                    setMessage(null);
                }

            };
        }

        createMappingsTree(composite);

        {
            // Control buttons
            Composite buttonsPanel = new Composite(composite, SWT.NONE);
            buttonsPanel.setLayout(new GridLayout(5, false));
            buttonsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            autoAssignButton = new Button(buttonsPanel, SWT.PUSH);
            autoAssignButton.setImage(DBeaverIcons.getImage(UIIcon.ASTERISK));
            autoAssignButton.setText(DTMessages.data_transfer_db_consumer_auto_assign);
            autoAssignButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    autoAssignMappings();
                }
            });

            final Button mapTableButton = new Button(buttonsPanel, SWT.PUSH);
            mapTableButton.setImage(DBeaverIcons.getImage(DBIcon.TREE_TABLE));
            mapTableButton.setText(DTMessages.data_transfer_db_consumer_existing_table);
            mapTableButton.setEnabled(false);
            mapTableButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    mapExistingTable((DatabaseMappingContainer) getSelectedMapping());
                }
            });

            final Button createNewButton = new Button(buttonsPanel, SWT.PUSH);
            createNewButton.setImage(DBeaverIcons.getImage(DBIcon.TREE_VIEW));
            createNewButton.setText(DTMessages.data_transfer_db_consumer_new_table);
            createNewButton.setEnabled(false);
            createNewButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    mapNewTable((DatabaseMappingContainer) getSelectedMapping());
                }
            });

            final Button columnsButton = new Button(buttonsPanel, SWT.PUSH);
            columnsButton.setImage(DBeaverIcons.getImage(DBIcon.TREE_COLUMNS));
            columnsButton.setText(DTMessages.data_transfer_db_consumer_column_mappings);
            columnsButton.setEnabled(false);
            columnsButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    DatabaseMappingObject selectedMapping = getSelectedMapping();
                    mapColumns(selectedMapping instanceof DatabaseMappingContainer ?
                        (DatabaseMappingContainer) selectedMapping :
                        ((DatabaseMappingAttribute)selectedMapping).getParent());
                }
            });

            final Button ddlButton = new Button(buttonsPanel, SWT.PUSH);
            ddlButton.setImage(DBeaverIcons.getImage(UIIcon.SQL_TEXT));
            ddlButton.setText(DTMessages.data_transfer_db_consumer_ddl);
            ddlButton.setEnabled(false);
            ddlButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    DatabaseMappingObject selectedMapping = getSelectedMapping();
                    showDDL(selectedMapping instanceof DatabaseMappingContainer ?
                        (DatabaseMappingContainer) selectedMapping :
                        ((DatabaseMappingAttribute)selectedMapping).getParent());
                }
            });

            mappingViewer.getTree().addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    try {
                        boolean updated = false;
                        Object element = null;
                        if (e.character == SWT.CR) {
                            //Object element = mappingViewer.getStructuredSelection().getFirstElement();
                            //mappingViewer.editElement(element, 1);
                        } else if (e.character == SWT.DEL) {
                            for (TreeItem item : mappingViewer.getTree().getSelection()) {
                                element = item.getData();
                                if (element instanceof DatabaseMappingAttribute) {
                                    DatabaseMappingAttribute attribute = (DatabaseMappingAttribute) element;
                                    attribute.setMappingType(DatabaseMappingType.skip);
                                } else if (element instanceof DatabaseMappingContainer) {
                                    DatabaseMappingContainer container = (DatabaseMappingContainer) element;
                                    container.refreshMappingType(getWizard().getRunnableContext(), DatabaseMappingType.skip);
                                }
                            }
                            updated = true;
                        } else if (e.character == SWT.SPACE) {
                            for (TreeItem item : mappingViewer.getTree().getSelection()) {
                                element = item.getData();
                                if (element instanceof DatabaseMappingAttribute) {
                                    DatabaseMappingAttribute attribute = (DatabaseMappingAttribute) item.getData();
                                    attribute.setMappingType(DatabaseMappingType.existing);
                                    attribute.updateMappingType(new VoidProgressMonitor());
                                } else if (element instanceof DatabaseMappingContainer) {
                                    DatabaseMappingContainer container = (DatabaseMappingContainer) element;
                                    setMappingTarget(container, container.getSource().getName());
                                }
                            }
                            updated = true;
                        }
                        if (updated) {
                            mappingViewer.refresh();
                            updatePageCompletion();
                            if (element instanceof DatabaseMappingContainer) {
                                // Select next container
                                @SuppressWarnings("unchecked")
                                List<DatabaseMappingContainer> model = (List<DatabaseMappingContainer>) mappingViewer.getInput();
                                int curIndex = model.indexOf(element);
                                if (curIndex < model.size() - 1) {
                                    mappingViewer.setSelection(new StructuredSelection(model.get(curIndex + 1)), true);
                                }
                            }
                        }
                    } catch (DBException e1) {
                        DBWorkbench.getPlatformUI().showError("Error mapping table", "Error mapping target table", e1);
                    }
                }
            });
            mappingViewer.addSelectionChangedListener(event -> {
                DatabaseMappingObject mapping = getSelectedMapping();
                mapTableButton.setEnabled(mapping instanceof DatabaseMappingContainer);
                createNewButton.setEnabled(mapping instanceof DatabaseMappingContainer && settings.getContainerNode() != null);
                final boolean hasMappings =
                    (mapping instanceof DatabaseMappingContainer && mapping.getMappingType() != DatabaseMappingType.unspecified) ||
                    (mapping instanceof DatabaseMappingAttribute && ((DatabaseMappingAttribute) mapping).getParent().getMappingType() != DatabaseMappingType.unspecified);
                columnsButton.setEnabled(hasMappings);
                ddlButton.setEnabled(hasMappings);
            });
            mappingViewer.addDoubleClickListener(event -> {
                DatabaseMappingObject selectedMapping = getSelectedMapping();
                if (selectedMapping != null) {
                    if (selectedMapping instanceof DatabaseMappingContainer){
/*
                        if (selectedMapping.getMappingType() == DatabaseMappingType.unspecified) {
                            mapExistingTable((DatabaseMappingContainer) selectedMapping);
                        } else {
                            mapColumns((DatabaseMappingContainer) selectedMapping);
                        }
*/
                    }
                }
            });
        }

        {
            Composite hintPanel = new Composite(composite, SWT.NONE);
            hintPanel.setLayout(new GridLayout(3, false));
            hintPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            new Label(hintPanel, SWT.NONE).setText("* DEL - skip column(s)  SPACE - map column(s)");
        }

        setControl(composite);
    }

    private void createMappingsTree(Composite composite)
    {
        // Mapping table
        mappingViewer = new TreeViewer(composite, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
        mappingViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        mappingViewer.getTree().setLinesVisible(true);
        mappingViewer.getTree().setHeaderVisible(true);

        TreeViewerColumn columnSource = new TreeViewerColumn(mappingViewer, SWT.LEFT);
        columnSource.setLabelProvider(new MappingLabelProvider() {
            @Override
            public void update(ViewerCell cell)
            {
                DatabaseMappingObject mapping = (DatabaseMappingObject) cell.getElement();
                cell.setText(DBUtils.getObjectFullName(mapping.getSource(), DBPEvaluationContext.UI));
                if (mapping.getIcon() != null) {
                    cell.setImage(DBeaverIcons.getImage(mapping.getIcon()));
                }
                super.update(cell);
            }
        });
        columnSource.getColumn().setText("Source");

        TreeViewerColumn columnTarget = new TreeViewerColumn(mappingViewer, SWT.LEFT);
        columnTarget.setLabelProvider(new MappingLabelProvider() {
            @Override
            public void update(ViewerCell cell)
            {
                DatabaseMappingObject mapping = (DatabaseMappingObject) cell.getElement();
                cell.setText(mapping.getTargetName());
                if (mapping.getMappingType() == DatabaseMappingType.unspecified) {
                    cell.setBackground(UIUtils.getSharedTextColors().getColor(SharedTextColors.COLOR_WARNING));
                } else {
                    cell.setBackground(null);
                }
                super.update(cell);
            }
        });
        columnTarget.getColumn().setText("Target");
        columnTarget.setEditingSupport(new EditingSupport(mappingViewer) {
            @Override
            protected CellEditor getCellEditor(Object element)
            {
                try {
                    return createTargetEditor(element);
                } catch (DBException e) {
                    setErrorMessage(e.getMessage());
                    return null;
                }
            }

            @Override
            protected boolean canEdit(Object element)
            {
                return true;
            }

            @Override
            protected Object getValue(Object element)
            {
                DatabaseMappingObject mapping = (DatabaseMappingObject)element;
                if (mapping.getMappingType() == DatabaseMappingType.unspecified) {
                    String newName = transformTargetName(DBUtils.getQuotedIdentifier(mapping.getSource()));
                    setValue(element, newName);
                    return newName;
                }
                if (mapping instanceof DatabaseMappingContainer) {
                    if (mapping.getMappingType() == DatabaseMappingType.existing) {
                        return ((DatabaseMappingContainer)mapping).getTarget();
                    }
                    return mapping.getTargetName();
                } else {
                    if (mapping.getMappingType() == DatabaseMappingType.existing) {
                        return ((DatabaseMappingAttribute)mapping).getTarget();
                    }
                    return mapping.getTargetName();
                }
            }

            @Override
            protected void setValue(final Object element, Object value)
            {
                try {
                    final DatabaseConsumerSettings settings = getDatabaseConsumerSettings();
                    String name = CommonUtils.toString(value);
                    DBPDataSource dataSource = settings.getTargetDataSource((DatabaseMappingObject) element);
                    if (!name.equals(DatabaseMappingAttribute.TARGET_NAME_SKIP) && !name.equals(TARGET_NAME_BROWSE) && dataSource != null) {
                        name = DBUtils.getQuotedIdentifier(dataSource, name);
                        name = DBObjectNameCaseTransformer.transformName(dataSource, name);
                    }
                    setMappingTarget((DatabaseMappingObject) element, name);
                    mappingViewer.update(element, null);
                    mappingViewer.setSelection(mappingViewer.getSelection());
                    updatePageCompletion();

                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError("Mapping error", "Error setting target table", e);
                }
            }
        });
        //TreeViewerEditor.create(mappingViewer, new TreeViewerFocusCellManager(), ColumnViewerEditor.TABBING_CYCLE_IN_ROW);

        TreeViewerColumn columnMapping = new TreeViewerColumn(mappingViewer, SWT.LEFT);
        columnMapping.setLabelProvider(new MappingLabelProvider() {
            @Override
            public void update(ViewerCell cell)
            {
                DatabaseMappingObject mapping = (DatabaseMappingObject) cell.getElement();
                cell.setText(mapping.getMappingType().name());
                super.update(cell);
            }
        });
        columnMapping.getColumn().setText("Mapping");
        columnMapping.setEditingSupport(new EditingSupport(mappingViewer) {
            @Override
            protected CellEditor getCellEditor(Object element) {
                List<String> mappingTypes = new ArrayList<>();
                mappingTypes.add(DatabaseMappingType.skip.name());
                DatabaseMappingObject mapping = (DatabaseMappingObject) element;
                if (mapping instanceof DatabaseMappingAttribute) {
                    mappingTypes.add(((DatabaseMappingAttribute) mapping).getParent().getMappingType().name());
                } else {
                    mappingTypes.add(mapping.getMappingType().name());
                }
                return new CustomComboBoxCellEditor(
                    mappingViewer,
                    mappingViewer.getTree(),
                    mappingTypes.toArray(new String[0]),
                    SWT.DROP_DOWN | SWT.READ_ONLY);
            }
            @Override
            protected boolean canEdit(Object element) {
                return true;
            }
            @Override
            protected Object getValue(Object element) {
                DatabaseMappingObject mapping = (DatabaseMappingObject) element;
                return mapping.getMappingType().name();
            }

            @Override
            protected void setValue(Object element, Object value) {
                try {
                    DatabaseMappingObject mapping = (DatabaseMappingObject) element;
                    DatabaseMappingType mappingType = DatabaseMappingType.valueOf(value.toString());
                    if (mapping instanceof DatabaseMappingAttribute) {
                        ((DatabaseMappingAttribute)mapping).setMappingType(mappingType);
                    } else {
                        ((DatabaseMappingContainer)mapping).refreshMappingType(getWizard().getRunnableContext(), mappingType);
                    }
                    mappingViewer.refresh();
                    setErrorMessage(null);
                } catch (DBException e) {
                    setErrorMessage(e.getMessage());
                }
            }
        });

        mappingViewer.setContentProvider(new TreeContentProvider() {
            @Override
            public boolean hasChildren(Object element)
            {
                return element instanceof DatabaseMappingContainer;
            }

            @Override
            public Object[] getChildren(Object parentElement)
            {
                if (parentElement instanceof DatabaseMappingContainer) {
                    return ((DatabaseMappingContainer) parentElement).getAttributeMappings(getWizard().getRunnableContext()).toArray();
                }
                return null;
            }
        });
    }

    private CellEditor createTargetEditor(Object element) throws DBException
    {
        final DatabaseConsumerSettings settings = getDatabaseConsumerSettings();
        boolean allowsCreate = true;
        List<String> items = new ArrayList<>();
        if (element instanceof DatabaseMappingContainer) {
            if (settings.getContainerNode() == null) {
                allowsCreate = false;
            }
            if (settings.getContainer() != null) {
                // container's tables
                DBSObjectContainer container = settings.getContainer();
                for (DBSObject child : container.getChildren(new VoidProgressMonitor())) {
                    if (child instanceof DBSDataManipulator) {
                        items.add(transformTargetName(DBUtils.getQuotedIdentifier(child)));
                    }
                }

            }
            items.add(TARGET_NAME_BROWSE);
        } else {
            DatabaseMappingAttribute mapping = (DatabaseMappingAttribute) element;
            switch (mapping.getParent().getMappingType()) {
                case skip:
                case unspecified:
                    allowsCreate = false;
                    break;
            }
            if (mapping.getParent().getTarget() instanceof DBSEntity) {
                DBSEntity parentEntity = (DBSEntity)mapping.getParent().getTarget();
                for (DBSEntityAttribute attr : parentEntity.getAttributes(new VoidProgressMonitor())) {
                    items.add(transformTargetName(DBUtils.getQuotedIdentifier(attr)));
                }
            }

        }
        items.add(DatabaseMappingAttribute.TARGET_NAME_SKIP);
        CustomComboBoxCellEditor editor = new CustomComboBoxCellEditor(
            mappingViewer,
            mappingViewer.getTree(),
            items.toArray(new String[0]),
            SWT.DROP_DOWN | (allowsCreate ? SWT.NONE : SWT.READ_ONLY));
        return editor;
    }

    private void setMappingTarget(DatabaseMappingObject mapping, String name) throws DBException
    {
        if (name.equals(DatabaseMappingAttribute.TARGET_NAME_SKIP)) {
            if (mapping instanceof DatabaseMappingAttribute) {
                ((DatabaseMappingAttribute)mapping).setMappingType(DatabaseMappingType.skip);
            } else {
                ((DatabaseMappingContainer)mapping).refreshMappingType(getWizard().getRunnableContext(), DatabaseMappingType.skip);
            }
        } else if (name.equals(TARGET_NAME_BROWSE)) {
            mapExistingTable((DatabaseMappingContainer) mapping);
        } else {
            name = transformTargetName(name);
            final DatabaseConsumerSettings settings = getDatabaseConsumerSettings();
            if (mapping instanceof DatabaseMappingContainer) {
                DatabaseMappingContainer containerMapping = (DatabaseMappingContainer)mapping;
                if (settings.getContainer() != null) {
                    // container's tables
                    DBSObjectContainer container = settings.getContainer();
                    for (DBSObject child : container.getChildren(new VoidProgressMonitor())) {
                        if (child instanceof DBSDataManipulator && name.equalsIgnoreCase(child.getName())) {
                            containerMapping.setTarget((DBSDataManipulator)child);
                            containerMapping.refreshMappingType(getWizard().getRunnableContext(), DatabaseMappingType.existing);
                            return;
                        }
                    }
                }
                containerMapping.refreshMappingType(getWizard().getRunnableContext(), DatabaseMappingType.create);
                ((DatabaseMappingContainer) mapping).setTargetName(name);
            } else {
                DatabaseMappingAttribute attrMapping = (DatabaseMappingAttribute) mapping;
                if (attrMapping.getParent().getTarget() instanceof DBSEntity) {
                    DBSEntity parentEntity = (DBSEntity)attrMapping.getParent().getTarget();
                    for (DBSEntityAttribute attr : parentEntity.getAttributes(new VoidProgressMonitor())) {
                        if (name.equalsIgnoreCase(attr.getName())) {
                            attrMapping.setMappingType(DatabaseMappingType.existing);
                            attrMapping.setTarget(attr);
                            return;
                        }
                    }
                }
                attrMapping.setMappingType(DatabaseMappingType.create);
                attrMapping.setTargetName(name);
            }
        }
    }

    private void autoAssignMappings() {
        for (TreeItem item : mappingViewer.getTree().getItems()) {
            Object element = item.getData();
            if (element instanceof DatabaseMappingContainer) {
                DatabaseMappingContainer container = (DatabaseMappingContainer) element;
                try {
                    setMappingTarget(container, container.getSource().getName());
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError("Mapping error", "Error auto mapping source table '" + container.getSource().getName() + "'", e);
                }
            }
        }
        mappingViewer.refresh();
        updatePageCompletion();
    }

    private void updateAutoAssign() {
        boolean hasUnassigned = false;
        final DatabaseConsumerSettings settings = getDatabaseConsumerSettings();
        for (DatabaseMappingContainer mapping : settings.getDataMappings().values()) {
            if (mapping.getMappingType() != DatabaseMappingType.create && mapping.getMappingType() != DatabaseMappingType.existing) {
                hasUnassigned = true;
                break;
            }
        }
        autoAssignButton.setEnabled(hasUnassigned);
    }

    private void mapExistingTable(DatabaseMappingContainer mapping)
    {
        final DatabaseConsumerSettings settings = getDatabaseConsumerSettings();
        DBPProject activeProject = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        if (activeProject != null) {
            DBNNode rootNode = settings.getContainerNode();
            if (rootNode == null) {
                rootNode = DBWorkbench.getPlatform().getNavigatorModel().getRoot().getProjectNode(
                    activeProject).getDatabases();
            }
            DBNNode selectedNode = rootNode;
            if (mapping.getTarget() != null) {
                selectedNode = DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(mapping.getTarget());
            }
            DBNNode node = DBWorkbench.getPlatformUI().selectObject(
                getShell(),
                "Choose target table",
                rootNode,
                selectedNode,
                new Class[] {DBSObjectContainer.class, DBSDataManipulator.class},
                new Class[] {DBSDataManipulator.class},
                null);
            if (node != null && node instanceof DBSWrapper) {
                DBSObject object = ((DBSWrapper) node).getObject();
                try {
                    if (object instanceof DBSDataManipulator) {
                        mapping.setTarget((DBSDataManipulator) object);
                        mapping.refreshMappingType(getWizard().getRunnableContext(), DatabaseMappingType.existing);
                        mapColumns(mapping);
                    } else {
                        mapping.setTarget(null);
                        mapping.refreshMappingType(getWizard().getRunnableContext(), DatabaseMappingType.unspecified);
                    }
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError("Error mapping table", "Error mapping existing table", e);
                }
                mappingViewer.refresh();
                updatePageCompletion();
            }
        }
    }

    private void mapNewTable(DatabaseMappingContainer mapping)
    {
        String tableName = EnterNameDialog.chooseName(
            getShell(),
            "New table name",
            transformTargetName(mapping.getMappingType() == DatabaseMappingType.create ? mapping.getTargetName() : ""));
        if (!CommonUtils.isEmpty(tableName)) {
            try {
                mapping.setTargetName(tableName);
                mapping.refreshMappingType(getWizard().getRunnableContext(), DatabaseMappingType.create);
                mappingViewer.refresh();
                updatePageCompletion();
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Mapping error", "Error mapping new table", e);
            }
        }
    }

    private String transformTargetName(String name) {
        DBSObjectContainer container = getDatabaseConsumerSettings().getContainer();
        if (container == null) {
            return name;
        }
        return DBObjectNameCaseTransformer.transformName(container.getDataSource(), name);
    }

    private void mapColumns(DatabaseMappingContainer mapping)
    {
        ColumnsMappingDialog dialog = new ColumnsMappingDialog(
            getWizard(),
            getDatabaseConsumerSettings(),
            mapping);
        if (dialog.open() == IDialogConstants.OK_ID) {
            mappingViewer.refresh();
            updatePageCompletion();
        }

    }

    private void showDDL(DatabaseMappingContainer mapping)
    {
        final DatabaseConsumerSettings settings = getDatabaseConsumerSettings();
        final DBSObjectContainer container = settings.getContainer();
        if (container == null) {
            return;
        }
        DBPDataSource dataSource = container.getDataSource();

        final String[] ddl = new String[1];
        try {
            getContainer().run(true, true, monitor -> {
                monitor.beginTask("Generate table DDL", 1);
                try {
                    DBCExecutionContext executionContext = DBUtils.getDefaultContext(dataSource, true);
                    ddl[0] = DatabaseTransferConsumer.generateTargetTableDDL(new DefaultProgressMonitor(monitor), executionContext, container, mapping);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
                monitor.done();
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Target DDL", "Error generating target DDL", e);
            return;
        } catch (InterruptedException e) {
            return;
        }
        UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
        if (serviceSQL != null) {
            serviceSQL.openSQLViewer(
                DBUtils.getDefaultContext(container, true),
                "Target DDL",
                null,
                ddl[0],
                false, false);
        }
    }

    private DatabaseMappingObject getSelectedMapping()
    {
        IStructuredSelection selection = (IStructuredSelection) mappingViewer.getSelection();
        return selection.isEmpty() ? null : (DatabaseMappingObject) selection.getFirstElement();
    }

    @Override
    public void activatePage()
    {
        final DatabaseConsumerSettings settings = getDatabaseConsumerSettings();

        DBSObjectContainer producerContainer = null;
        for (DataTransferPipe pipe : getWizard().getSettings().getDataPipes()) {
            if (pipe.getProducer() != null) {
                DBSObject producerObject = pipe.getProducer().getDatabaseObject();
                if (producerObject instanceof DBSDataContainer) {
                    DBSObject container = producerObject.getParentObject();
                    if (container instanceof DBSObjectContainer) {
                        producerContainer = (DBSObjectContainer) container;
                    }
                }
            }
        }

        settings.loadNode(getWizard().getRunnableContext(), producerContainer);
        DBNDatabaseNode containerNode = settings.getContainerNode();
        if (containerNode != null) {
            try {
                containerPanel.checkValidContainerNode(containerNode);
                containerPanel.setContainerInfo(containerNode);
            } catch (DBException e) {
                setErrorMessage(e.getMessage());
            }
        }

        {
            List<DatabaseMappingContainer> model = new ArrayList<>();

            for (DataTransferPipe pipe : getWizard().getSettings().getDataPipes()) {
                if (pipe.getProducer() == null) {
                    continue;
                }
                DBSDataContainer sourceObject = (DBSDataContainer)pipe.getProducer().getDatabaseObject();
                DatabaseMappingContainer mapping = settings.getDataMapping(sourceObject);
                if (mapping == null) {
                    if (pipe.getConsumer() instanceof DatabaseTransferConsumer && ((DatabaseTransferConsumer)pipe.getConsumer()).getTargetObject() != null) {
                        try {
                            mapping = new DatabaseMappingContainer(
                                getWizard().getRunnableContext(),
                                getDatabaseConsumerSettings(),
                                sourceObject,
                                ((DatabaseTransferConsumer)pipe.getConsumer()).getTargetObject());
                        } catch (DBException e) {
                            setMessage(e.getMessage(), IMessageProvider.ERROR);
                            mapping = new DatabaseMappingContainer(getDatabaseConsumerSettings(), sourceObject);
                        }
                    } else {
                        mapping = new DatabaseMappingContainer(getDatabaseConsumerSettings(), sourceObject);
                    }
                    settings.addDataMappings(getWizard().getRunnableContext(), sourceObject, mapping);
                }
                model.add(mapping);
            }
            mappingViewer.setInput(model);

            Tree table = mappingViewer.getTree();
            int totalWidth = table.getClientArea().width;
            TreeColumn[] columns = table.getColumns();
            columns[0].setWidth(totalWidth * 40 / 100);
            columns[1].setWidth(totalWidth * 40 / 100);
            columns[2].setWidth(totalWidth * 20 / 100);
        }

        updatePageCompletion();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        final DatabaseConsumerSettings settings = getDatabaseConsumerSettings();
        if (settings.getContainerNode() == null) {
            setErrorMessage("Set target container");
            return false;
        }
        if (!settings.isCompleted(getWizard().getSettings().getDataPipes())) {
            setErrorMessage("Set all tables mappings");
            return false;
        } else {
            setErrorMessage(null);
            return true;
        }
    }

    @Override
    protected void updatePageCompletion() {
        super.updatePageCompletion();
        updateAutoAssign();
    }

}