/*
 * Copyright (C) 2010-2012 Serge Rieder
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
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

    static final Log log = LogFactory.getLog(DatabaseConsumerPageMapping.class);

    public static final String TARGET_NAME_SKIP = "[skip]";
    public static final String TARGET_NAME_BROWSE = "[browse]";
    private TreeViewer mappingViewer;

    private static abstract class MappingLabelProvider extends CellLabelProvider {
        @Override
        public void update(ViewerCell cell)
        {
        }
    }

    public DatabaseConsumerPageMapping() {
        super("Entities mapping");
        setTitle("Entities mapping");
        setDescription("Map entities transfer");
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

            final Label containerIcon = new Label(containerPanel, SWT.NONE);
            containerIcon.setImage(DBIcon.TYPE_UNKNOWN.getImage());

            final Text containerName = new Text(containerPanel, SWT.BORDER | SWT.READ_ONLY);
            containerName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

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
                            DBSObjectContainer.class);
                        if (node instanceof DBNDatabaseNode) {
                            settings.setContainerNode((DBNDatabaseNode) node);
                            containerIcon.setImage(node.getNodeIconDefault());
                            containerName.setText(node.getNodeFullName());
                            mappingViewer.setSelection(mappingViewer.getSelection());
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

            mappingViewer.addSelectionChangedListener(new ISelectionChangedListener() {
                @Override
                public void selectionChanged(SelectionChangedEvent event)
                {
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

        setControl(composite);
    }

    private void createMappingsTree(Composite composite)
    {
        // Mapping table
        mappingViewer = new TreeViewer(composite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        mappingViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        mappingViewer.getTree().setLinesVisible(true);
        mappingViewer.getTree().setHeaderVisible(true);

        TreeViewerColumn columnSource = new TreeViewerColumn(mappingViewer, SWT.LEFT);
        columnSource.setLabelProvider(new MappingLabelProvider() {
            @Override
            public void update(ViewerCell cell)
            {
                DatabaseMappingObject mapping = (DatabaseMappingObject) cell.getElement();
                cell.setText(mapping.getSourceName());
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
                    cell.setBackground(DBeaverUI.getSharedTextColors().getColor(SharedTextColors.COLOR_BACK_DELETED));
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
                    return TARGET_NAME_SKIP;
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
                    setMappingTarget((DatabaseMappingObject) element, CommonUtils.toString(value));
                    mappingViewer.refresh();
                    updatePageCompletion();
                    setErrorMessage(null);
                } catch (DBException e) {
                    setErrorMessage(e.getMessage());
                }
            }
        });
        //TreeViewerEditor.create(mappingViewer, new TreeViewerFocusCellManager(), ColumnViewerEditor.TABBING_CYCLE_IN_ROW);

        TreeViewerColumn columnType = new TreeViewerColumn(mappingViewer, SWT.LEFT);
        columnType.setLabelProvider(new MappingLabelProvider() {
            @Override
            public void update(ViewerCell cell)
            {
                DatabaseMappingObject mapping = (DatabaseMappingObject) cell.getElement();
                String text = "";
                switch (mapping.getMappingType()) {
                    case unspecified: text = "?"; break;
                    case existing: text = "select"; break;
                    case create: text = "new"; break;
                    case skip: text = "skip"; break;
                }
                cell.setText(text);
                super.update(cell);
            }
        });
        columnType.getColumn().setText("Type");

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
                    return ((DatabaseMappingContainer) parentElement).getAttributeMappings(
                        getWizard().getContainer()).toArray();
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
        items.add(TARGET_NAME_SKIP);
        if (element instanceof DatabaseMappingContainer) {
            items.add(TARGET_NAME_BROWSE);
            if (settings.getContainerNode() != null && settings.getContainerNode().getObject() instanceof DBSObjectContainer) {
                // container's tables
                DBSObjectContainer container = (DBSObjectContainer)settings.getContainerNode().getObject();
                for (DBSObject child : container.getChildren(VoidProgressMonitor.INSTANCE)) {
                    if (child instanceof DBSDataManipulator) {
                        items.add(child.getName());
                    }
                }

            }
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
        CustomComboBoxCellEditor editor = new CustomComboBoxCellEditor(
            mappingViewer.getTree(),
            items.toArray(new String[items.size()]),
            SWT.DROP_DOWN | (allowsCreate ? SWT.NONE : SWT.READ_ONLY));
        return editor;
    }

    private void setMappingTarget(DatabaseMappingObject mapping, String name) throws DBException
    {
        if (name.equals(TARGET_NAME_SKIP)) {
            if (mapping instanceof DatabaseMappingAttribute) {
                ((DatabaseMappingAttribute)mapping).setMappingType(DatabaseMappingType.skip);
            } else {
                ((DatabaseMappingContainer)mapping).setMappingType(getWizard().getContainer(), DatabaseMappingType.skip);
            }
        } else if (name.equals(TARGET_NAME_BROWSE)) {
            mapExistingTable((DatabaseMappingContainer) mapping);
        } else {
            final DatabaseConsumerSettings settings = getWizard().getPageSettings(this, DatabaseConsumerSettings.class);
            if (mapping instanceof DatabaseMappingContainer) {
                DatabaseMappingContainer containerMapping = (DatabaseMappingContainer)mapping;
                if (settings.getContainerNode() != null && settings.getContainerNode().getObject() instanceof DBSObjectContainer) {
                    // container's tables
                    DBSObjectContainer container = (DBSObjectContainer)settings.getContainerNode().getObject();
                    for (DBSObject child : container.getChildren(VoidProgressMonitor.INSTANCE)) {
                        if (child instanceof DBSDataManipulator && name.equals(child.getName())) {
                            containerMapping.setMappingType(getWizard().getContainer(), DatabaseMappingType.existing);
                            containerMapping.setTarget((DBSDataManipulator)child);
                            return;
                        }
                    }
                }
                containerMapping.setMappingType(getWizard().getContainer(), DatabaseMappingType.create);
                ((DatabaseMappingContainer) mapping).setTargetName(name);
            } else {
                DatabaseMappingAttribute attrMapping = (DatabaseMappingAttribute) mapping;
                if (attrMapping.getParent().getTarget() instanceof DBSEntity) {
                    DBSEntity parentEntity = (DBSEntity)attrMapping.getParent().getTarget();
                    for (DBSEntityAttribute attr : parentEntity.getAttributes(VoidProgressMonitor.INSTANCE)) {
                        if (name.equals(attr.getName())) {
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
                DBSObjectContainer.class, DBSDataManipulator.class);
            if (node != null && node instanceof DBSWrapper) {
                DBSObject object = ((DBSWrapper) node).getObject();
                try {
                    if (object instanceof DBSDataManipulator) {
                        mapping.setTarget((DBSDataManipulator) object);
                        mapping.setMappingType(getWizard().getContainer(), DatabaseMappingType.existing);
                        mapColumns(mapping);
                    } else {
                        mapping.setTarget(null);
                        mapping.setMappingType(getWizard().getContainer(), DatabaseMappingType.unspecified);
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
                mapping.setMappingType(getWizard().getContainer(), DatabaseMappingType.create);
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
        ColumnsMappingDialog dialog = new ColumnsMappingDialog(getWizard(), mapping);
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
                    dataMappings.put(sourceObject, new DatabaseMappingContainer(sourceObject));
                }
            }
            mappingViewer.setInput(dataMappings.values());

            Tree table = mappingViewer.getTree();
            int totalWidth = table.getClientArea().width;
            TreeColumn[] columns = table.getColumns();
            columns[0].setWidth(totalWidth * 40 / 100);
            columns[1].setWidth(totalWidth * 40 / 100);
            columns[2].setWidth(totalWidth * 10 / 100);
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