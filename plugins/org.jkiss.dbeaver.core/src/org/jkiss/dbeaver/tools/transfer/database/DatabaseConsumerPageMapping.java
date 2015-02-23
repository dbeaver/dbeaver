/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.tools.transfer.database;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.resources.IProject;
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.SharedTextColors;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomComboBoxCellEditor;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.BrowseObjectDialog;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Map;

public class DatabaseConsumerPageMapping extends ActiveWizardPage<DataTransferWizard> {

    static final Log log = Log.getLog(DatabaseConsumerPageMapping.class);

    public static final String TARGET_NAME_BROWSE = "[browse]";
    private TreeViewer mappingViewer;

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
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        final DatabaseConsumerSettings settings = getWizard().getPageSettings(this, DatabaseConsumerSettings.class);

        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout gl = new GridLayout();
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        composite.setLayout(gl);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            // Target container
            Composite containerPanel = new Composite(composite, SWT.NONE);
            containerPanel.setLayout(new GridLayout(4, false));
            containerPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            UIUtils.createControlLabel(containerPanel, "Target container");

            DBNDatabaseNode containerNode = settings.getContainerNode();
            final Label containerIcon = new Label(containerPanel, SWT.NONE);
            containerIcon.setImage(DBIcon.TYPE_UNKNOWN.getImage());
            if (containerNode != null) containerIcon.setImage(containerNode.getNodeIconDefault());

            final Text containerName = new Text(containerPanel, SWT.BORDER | SWT.READ_ONLY);
            containerName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            if (containerNode != null) containerName.setText(settings.getContainerFullName());

            Button browseButton = new Button(containerPanel, SWT.PUSH);
            browseButton.setImage(DBIcon.TREE_FOLDER.getImage());
            browseButton.setText("...");
            browseButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    IProject activeProject = DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
                    if (activeProject != null) {
                        final DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
                        final DBNProject rootNode = navigatorModel.getRoot().getProject(
                            activeProject);
                        DBNNode selectedNode = settings.getContainerNode();
                        if (selectedNode == null && !settings.getDataMappings().isEmpty()) {
                            // Use first source object as cur selection (it's better than nothing)
                            DBSDataContainer firstSource = settings.getDataMappings().keySet().iterator().next();
                            selectedNode = navigatorModel.getNodeByObject(firstSource);
                            while (selectedNode != null) {
                                if (selectedNode instanceof DBSWrapper && ((DBSWrapper) selectedNode).getObject() instanceof DBSObjectContainer) {
                                    break;
                                } else {
                                    selectedNode = selectedNode.getParentNode();
                                }
                            }
                        }
                        DBNNode node = BrowseObjectDialog.selectObject(
                            getShell(),
                            "Choose container",
                            rootNode.getDatabases(),
                            selectedNode,
                            new Class[] {DBSObjectContainer.class},
                            null);
                        if (node instanceof DBNDatabaseNode) {
                            settings.setContainerNode((DBNDatabaseNode) node);
                            containerIcon.setImage(node.getNodeIconDefault());
                            containerName.setText(settings.getContainerFullName());
                            // Reset mappings
                            for (DatabaseMappingContainer mappingContainer : settings.getDataMappings().values()) {
                                if (mappingContainer.getMappingType() == DatabaseMappingType.create) {
                                    try {
                                        mappingContainer.refreshMappingType(getContainer(), DatabaseMappingType.create);
                                    } catch (DBException e1) {
                                        log.error(e1);
                                    }
                                }
                            }
                            mappingViewer.refresh();
                        }
                    }
                }
            });
        }

        createMappingsTree(composite);

        {
            // Control buttons
            Composite buttonsPanel = new Composite(composite, SWT.NONE);
            buttonsPanel.setLayout(new GridLayout(3, false));
            buttonsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            final Button mapTableButton = new Button(buttonsPanel, SWT.PUSH);
            mapTableButton.setImage(DBIcon.TREE_TABLE.getImage());
            mapTableButton.setText("Existing table ...");
            mapTableButton.setEnabled(false);
            mapTableButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    mapExistingTable((DatabaseMappingContainer) getSelectedMapping());
                }
            });

            final Button createNewButton = new Button(buttonsPanel, SWT.PUSH);
            createNewButton.setImage(DBIcon.TREE_VIEW.getImage());
            createNewButton.setText("Create new ...");
            createNewButton.setEnabled(false);
            createNewButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    mapNewTable((DatabaseMappingContainer) getSelectedMapping());
                }
            });

            final Button columnsButton = new Button(buttonsPanel, SWT.PUSH);
            columnsButton.setImage(DBIcon.TREE_COLUMNS.getImage());
            columnsButton.setText("Columns' mappings ...");
            columnsButton.setEnabled(false);
            columnsButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    mapColumns((DatabaseMappingContainer) getSelectedMapping());
                }
            });

            mappingViewer.getTree().addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    try {
                        if (e.character == SWT.DEL) {
                            for (TreeItem item : mappingViewer.getTree().getSelection()) {
                                Object data = item.getData();
                                if (data instanceof DatabaseMappingAttribute) {
                                    DatabaseMappingAttribute attribute = (DatabaseMappingAttribute) data;
                                    attribute.setMappingType(DatabaseMappingType.skip);
                                } else if (data instanceof DatabaseMappingContainer) {
                                    DatabaseMappingContainer container = (DatabaseMappingContainer) data;
                                    container.refreshMappingType(getContainer(), DatabaseMappingType.skip);
                                }
                            }
                            mappingViewer.refresh();
                        } else if (e.character == SWT.SPACE) {
                            for (TreeItem item : mappingViewer.getTree().getSelection()) {
                                Object data = item.getData();
                                if (data instanceof DatabaseMappingAttribute) {
                                    DatabaseMappingAttribute attribute = (DatabaseMappingAttribute) item.getData();
                                    attribute.setMappingType(DatabaseMappingType.existing);
                                    attribute.updateMappingType(VoidProgressMonitor.INSTANCE);
                                } else if (data instanceof DatabaseMappingContainer) {
                                    DatabaseMappingContainer container = (DatabaseMappingContainer) data;
                                    setMappingTarget(container, container.getSource().getName());
                                }
                            }
                            mappingViewer.refresh();
                        }
                    } catch (DBException e1) {
                        log.error(e1);
                    }
                }
            });
            mappingViewer.addSelectionChangedListener(new ISelectionChangedListener() {
                @Override
                public void selectionChanged(SelectionChangedEvent event) {
                    DatabaseMappingObject mapping = getSelectedMapping();
                    mapTableButton.setEnabled(mapping instanceof DatabaseMappingContainer);
                    createNewButton.setEnabled(mapping instanceof DatabaseMappingContainer && settings.getContainerNode() != null);
                    columnsButton.setEnabled(mapping instanceof DatabaseMappingContainer && mapping.getMappingType() != DatabaseMappingType.unspecified);
                }
            });
            mappingViewer.addDoubleClickListener(new IDoubleClickListener() {
                @Override
                public void doubleClick(DoubleClickEvent event)
                {
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
                cell.setText(DBUtils.getObjectFullName(mapping.getSource()));
                cell.setImage(mapping.getIcon());
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
                    cell.setBackground(DBeaverUI.getSharedTextColors().getColor(SharedTextColors.COLOR_WARNING));
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
                    CellEditor targetEditor = createTargetEditor(element);
                    setErrorMessage(null);
                    return targetEditor;
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
                    return mapping.getSource().getName();
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
            protected void setValue(Object element, Object value)
            {
                try {
                    final DatabaseConsumerSettings settings = getWizard().getPageSettings(DatabaseConsumerPageMapping.this, DatabaseConsumerSettings.class);
                    String name = CommonUtils.toString(value);
                    DBPDataSource dataSource = settings.getTargetDataSource((DatabaseMappingObject) element);
                    if (!name.equals(DatabaseMappingAttribute.TARGET_NAME_SKIP) && !name.equals(TARGET_NAME_BROWSE) && dataSource != null) {
                        name = DBObjectNameCaseTransformer.transformName(dataSource, name);
                    }
                    setMappingTarget((DatabaseMappingObject) element, name);
                    mappingViewer.refresh();
                    updatePageCompletion();
                    setErrorMessage(null);
                } catch (DBException e) {
                    setErrorMessage(e.getMessage());
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
                String text = "";
                switch (mapping.getMappingType()) {
                    case unspecified:
                        text = "?";
                        break;
                    case existing:
                        text = "existing";
                        break;
                    case create:
                        text = "new";
                        break;
                    case skip:
                        text = "skip";
                        break;
                }
                cell.setText(text);
                super.update(cell);
            }
        });
        columnMapping.getColumn().setText("Mapping");

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
                    return ((DatabaseMappingContainer) parentElement).getAttributeMappings(getContainer()).toArray();
                }
                return null;
            }
        });
    }

    private CellEditor createTargetEditor(Object element) throws DBException
    {
        final DatabaseConsumerSettings settings = getWizard().getPageSettings(this, DatabaseConsumerSettings.class);
        boolean allowsCreate = true;
        java.util.List<String> items = new ArrayList<String>();
        if (element instanceof DatabaseMappingContainer) {
            if (settings.getContainerNode() == null) {
                allowsCreate = false;
            }
            if (settings.getContainer() != null) {
                // container's tables
                DBSObjectContainer container = settings.getContainer();
                for (DBSObject child : container.getChildren(VoidProgressMonitor.INSTANCE)) {
                    if (child instanceof DBSDataManipulator) {
                        items.add(child.getName());
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
                for (DBSEntityAttribute attr : parentEntity.getAttributes(VoidProgressMonitor.INSTANCE)) {
                    items.add(attr.getName());
                }
            }

        }
        items.add(DatabaseMappingAttribute.TARGET_NAME_SKIP);
        CustomComboBoxCellEditor editor = new CustomComboBoxCellEditor(
            mappingViewer.getTree(),
            items.toArray(new String[items.size()]),
            SWT.DROP_DOWN | (allowsCreate ? SWT.NONE : SWT.READ_ONLY));
        return editor;
    }

    private void setMappingTarget(DatabaseMappingObject mapping, String name) throws DBException
    {
        if (name.equals(DatabaseMappingAttribute.TARGET_NAME_SKIP)) {
            if (mapping instanceof DatabaseMappingAttribute) {
                ((DatabaseMappingAttribute)mapping).setMappingType(DatabaseMappingType.skip);
            } else {
                ((DatabaseMappingContainer)mapping).refreshMappingType(getWizard().getContainer(), DatabaseMappingType.skip);
            }
        } else if (name.equals(TARGET_NAME_BROWSE)) {
            mapExistingTable((DatabaseMappingContainer) mapping);
        } else {
            final DatabaseConsumerSettings settings = getWizard().getPageSettings(this, DatabaseConsumerSettings.class);
            if (mapping instanceof DatabaseMappingContainer) {
                DatabaseMappingContainer containerMapping = (DatabaseMappingContainer)mapping;
                if (settings.getContainer() != null) {
                    // container's tables
                    DBSObjectContainer container = settings.getContainer();
                    for (DBSObject child : container.getChildren(VoidProgressMonitor.INSTANCE)) {
                        if (child instanceof DBSDataManipulator && name.equalsIgnoreCase(child.getName())) {
                            containerMapping.setTarget((DBSDataManipulator)child);
                            containerMapping.refreshMappingType(getWizard().getContainer(), DatabaseMappingType.existing);
                            return;
                        }
                    }
                }
                containerMapping.refreshMappingType(getWizard().getContainer(), DatabaseMappingType.create);
                ((DatabaseMappingContainer) mapping).setTargetName(name);
            } else {
                DatabaseMappingAttribute attrMapping = (DatabaseMappingAttribute) mapping;
                if (attrMapping.getParent().getTarget() instanceof DBSEntity) {
                    DBSEntity parentEntity = (DBSEntity)attrMapping.getParent().getTarget();
                    for (DBSEntityAttribute attr : parentEntity.getAttributes(VoidProgressMonitor.INSTANCE)) {
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

    private void mapExistingTable(DatabaseMappingContainer mapping)
    {
        final DatabaseConsumerSettings settings = getWizard().getPageSettings(this, DatabaseConsumerSettings.class);
        IProject activeProject = DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
        if (activeProject != null) {
            DBNNode rootNode = settings.getContainerNode();
            if (rootNode == null) {
                rootNode = DBeaverCore.getInstance().getNavigatorModel().getRoot().getProject(
                    activeProject).getDatabases();
            }
            DBNNode selectedNode = rootNode;
            if (mapping.getTarget() != null) {
                selectedNode = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(mapping.getTarget());
            }
            DBNNode node = BrowseObjectDialog.selectObject(
                getShell(),
                "Choose target table",
                rootNode,
                selectedNode,
                new Class[] {DBSObjectContainer.class, DBSDataManipulator.class},
                new Class[] {DBSDataManipulator.class});
            if (node != null && node instanceof DBSWrapper) {
                DBSObject object = ((DBSWrapper) node).getObject();
                try {
                    if (object instanceof DBSDataManipulator) {
                        mapping.setTarget((DBSDataManipulator) object);
                        mapping.refreshMappingType(getWizard().getContainer(), DatabaseMappingType.existing);
                        mapColumns(mapping);
                    } else {
                        mapping.setTarget(null);
                        mapping.refreshMappingType(getWizard().getContainer(), DatabaseMappingType.unspecified);
                    }
                    setErrorMessage(null);
                } catch (DBException e) {
                    log.error(e);
                    setErrorMessage(e.getMessage());
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
            mapping.getMappingType() == DatabaseMappingType.create ? mapping.getTargetName() : "");
        if (!CommonUtils.isEmpty(tableName)) {
            try {
                mapping.setTargetName(tableName);
                mapping.refreshMappingType(getWizard().getContainer(), DatabaseMappingType.create);
                mappingViewer.refresh();
                updatePageCompletion();
            } catch (DBException e) {
                log.error(e);
                setErrorMessage(e.getMessage());
            }
        }
    }

    private void mapColumns(DatabaseMappingContainer mapping)
    {
        ColumnsMappingDialog dialog = new ColumnsMappingDialog(
            getWizard(),
            getWizard().getPageSettings(this, DatabaseConsumerSettings.class),
            mapping);
        if (dialog.open() == IDialogConstants.OK_ID) {
            mappingViewer.refresh();
            updatePageCompletion();
        }

    }

    DatabaseMappingObject getSelectedMapping()
    {
        IStructuredSelection selection = (IStructuredSelection) mappingViewer.getSelection();
        return selection.isEmpty() ? null : (DatabaseMappingObject) selection.getFirstElement();
    }

    @Override
    public void activatePage()
    {
        if (mappingViewer.getInput() == null) {
            final DatabaseConsumerSettings settings = getWizard().getPageSettings(this, DatabaseConsumerSettings.class);

            Map<DBSDataContainer,DatabaseMappingContainer> dataMappings = settings.getDataMappings();
            for (DataTransferPipe pipe : getWizard().getSettings().getDataPipes()) {
                if (pipe.getProducer() == null) {
                    continue;
                }
                DBSDataContainer sourceObject = (DBSDataContainer)pipe.getProducer().getSourceObject();
                if (!dataMappings.containsKey(sourceObject)) {
                    DatabaseMappingContainer mapping;
                    if (pipe.getConsumer() instanceof DatabaseTransferConsumer && ((DatabaseTransferConsumer)pipe.getConsumer()).getTargetObject() != null) {
                        try {
                            mapping = new DatabaseMappingContainer(getContainer(), sourceObject, ((DatabaseTransferConsumer)pipe.getConsumer()).getTargetObject());
                        } catch (DBException e) {
                            setMessage(e.getMessage(), IMessageProvider.ERROR);
                            mapping = new DatabaseMappingContainer(sourceObject);
                        }
                    } else {
                        mapping = new DatabaseMappingContainer(sourceObject);
                    }
                    dataMappings.put(sourceObject, mapping);
                }
            }
            mappingViewer.setInput(dataMappings.values());

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
        final DatabaseConsumerSettings settings = getWizard().getPageSettings(this, DatabaseConsumerSettings.class);
        return settings.isCompleted(getWizard().getSettings().getDataPipes());
    }

}