/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.pages.DataTransferPageNodeSettings;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DatabaseProducerPageInputObjects extends DataTransferPageNodeSettings {

    private Table mappingTable;
    private DBNDatabaseNode lastSelection;

    public DatabaseProducerPageInputObjects() {
        super(DTUIMessages.database_producer_page_input_objects_name);
        setTitle(DTUIMessages.database_producer_page_input_objects_title);
        setDescription(DTUIMessages.database_producer_page_input_objects_description);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = UIUtils.createComposite(parent, 1);

        DataTransferSettings settings = getWizard().getSettings();

        {
            Composite tablesGroup = UIUtils.createComposite(composite, 1);
            tablesGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
            UIUtils.createControlLabel(tablesGroup, DTMessages.data_transfer_wizard_mappings_name);

            mappingTable = new Table(tablesGroup, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            mappingTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            mappingTable.setHeaderVisible(true);
            mappingTable.setLinesVisible(true);

            UIUtils.createTableColumn(mappingTable, SWT.LEFT, DTUIMessages.data_transfer_wizard_final_column_source);
            UIUtils.createTableColumn(mappingTable, SWT.LEFT, DTUIMessages.data_transfer_wizard_final_column_target);

            mappingTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (mappingTable.getSelectionIndex() < 0) {
                        return;
                    }
                    TableItem item = mappingTable.getItem(mappingTable.getSelectionIndex());
                    DataTransferPipe pipe = (DataTransferPipe) item.getData();
                    if (chooseEntity(pipe)) {
                        updateItemData(item, pipe);
                        updatePageCompletion();
                    }
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                    widgetSelected(e);
                }
            });
            UIUtils.asyncExec(() -> UIUtils.packColumns(mappingTable, true));
        }
        {
            Composite controlGroup = UIUtils.createComposite(composite, 1);
            Button autoAssignButton = new Button(controlGroup, SWT.PUSH);
            autoAssignButton.setImage(DBeaverIcons.getImage(UIIcon.ASTERISK));
            autoAssignButton.setText(DTMessages.data_transfer_db_consumer_auto_assign);
            autoAssignButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    autoAssignMappings();
                }
            });
        }

        setControl(composite);

    }

    private void autoAssignMappings() {
        DBSObjectContainer objectContainer = chooseEntityContainer();
        if (objectContainer == null) {
            setMessage(DTUIMessages.database_producer_page_input_objects_error_message_auto_assign_failed, WARNING);
            return;
        }

        java.util.List<DBSObject> containerObjects = new ArrayList<>();
        try {
            getWizard().getContainer().run(true, true, mon -> {
                try {
                    Collection<? extends DBSObject> children = objectContainer.getChildren(new DefaultProgressMonitor(mon));
                    if (children != null) {
                        containerObjects.addAll(children);
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(DTUIMessages.database_producer_page_input_objects_title_assign_error,
                    DTUIMessages.database_producer_page_input_objects_message_error_reading_container_objects, e);
        } catch (InterruptedException e) {
            // ignore
        }
        if (CommonUtils.isEmpty(containerObjects)) {
            setMessage(DTUIMessages.database_producer_page_input_objects_error_message_auto_assign_failed, WARNING);
        } else {
            autoAssignMappings(containerObjects);
        }
    }

    private void autoAssignMappings(List<DBSObject> containerObjects) {
        boolean chooseConsumer = getWizard().getSettings().isConsumerOptional();
        boolean success = false;

        for (TableItem item : mappingTable.getItems()) {
            DataTransferPipe pipe = (DataTransferPipe) item.getData();
            if ((chooseConsumer && (pipe.getConsumer() == null || pipe.getConsumer().getDatabaseObject() == null)) ||
                (!chooseConsumer && (pipe.getProducer() == null || pipe.getProducer().getDatabaseObject() == null))) {
                DBSObject objectToMap = chooseConsumer ? pipe.getProducer().getDatabaseObject() : pipe.getConsumer().getDatabaseObject() ;
                if (objectToMap == null) {
                    continue;
                }

                DBSObject object = DBUtils.findObject(containerObjects, objectToMap.getName());
                if (object != null) {
                    success = true;
                    if (chooseConsumer) {
                        if (object instanceof DBSDataManipulator) {
                            pipe.setConsumer(new DatabaseTransferConsumer((DBSDataManipulator) object));
                        }
                    } else {
                        if (object instanceof DBSDataContainer) {
                            pipe.setProducer(new DatabaseTransferProducer((DBSDataContainer) object));
                        }
                    }
                    updateItemData(item, pipe);
                }

            }
        }
        if (!success) {
            setMessage(DTUIMessages.database_producer_page_input_objects_error_message_auto_assign_failed, WARNING);
        }
        updatePageCompletion();
    }

    private void updateItemData(TableItem item, DataTransferPipe pipe) {
        setErrorMessage(null);
        DataTransferSettings settings = getWizard().getSettings();

        if (pipe.getProducer() == null || pipe.getProducer().getDatabaseObject() == null) {
            item.setImage(0, null);
            item.setText(0, DTUIMessages.database_producer_page_input_objects_item_text_none);
        } else {
            item.setImage(0, DBeaverIcons.getImage(settings.getProducer().getIcon()));
            item.setText(0, DBUtils.getObjectFullName(pipe.getProducer().getDatabaseObject(), DBPEvaluationContext.DML));
        }
        if (pipe.getConsumer() == null || pipe.getConsumer().getObjectName() == null) {
            item.setImage(1, null);
            item.setText(1, DTUIMessages.database_producer_page_input_objects_item_text_none);
        } else {
            item.setImage(1, DBeaverIcons.getImage(settings.getConsumer().getIcon()));
            item.setText(1, pipe.getConsumer().getObjectName());
        }
    }

    @Override
    public void activatePage()
    {
        //final DatabaseProducerSettings settings = getWizard().getPageSettings(this, DatabaseProducerSettings.class);
        DataTransferSettings settings = getWizard().getSettings();

        mappingTable.removeAll();
        for (DataTransferPipe pipe : settings.getDataPipes()) {
            TableItem item = new TableItem(mappingTable, SWT.NONE);
            item.setData(pipe);
            updateItemData(item, pipe);
        }

        updatePageCompletion();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        for (DataTransferPipe pipe : getWizard().getSettings().getDataPipes()) {
            if (pipe.getConsumer() == null || pipe.getProducer() == null || pipe.getProducer().getDatabaseObject() == null) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private DBSObjectContainer chooseEntityContainer() {
        DataTransferSettings settings = getWizard().getSettings();

        final DBNModel navigatorModel = DBWorkbench.getPlatform().getNavigatorModel();
        final DBNNode rootNode = DBWorkbench.getPlatform().getWorkspace().getProjects().size() == 1 ?
            navigatorModel.getRoot().getProjectNode(DBWorkbench.getPlatform().getWorkspace().getActiveProject()) : navigatorModel.getRoot();
        boolean chooseConsumer = settings.isConsumerOptional();
        DBNNode node = DBWorkbench.getPlatformUI().selectObject(
            UIUtils.getActiveWorkbenchShell(),
            DTUIMessages.database_producer_page_input_objects_node_select_table,
            rootNode,
            lastSelection,
            new Class[] {DBSInstance.class, DBSObjectContainer.class},
            new Class[] {DBSObjectContainer.class},
            null);
        if (!(node instanceof DBNDatabaseNode)) {
            return null;
        }
        lastSelection = (DBNDatabaseNode) node;
        DBSObject object = lastSelection.getObject();
        if (!(object instanceof DBSObjectContainer)) {
            object = DBUtils.getAdapter(DBSObjectContainer.class, ((DBSWrapper) node).getObject());
        }
        return (DBSObjectContainer) object;
    }

    protected boolean chooseEntity(DataTransferPipe pipe)
    {
        DataTransferSettings settings = getWizard().getSettings();

        final DBNModel navigatorModel = DBWorkbench.getPlatform().getNavigatorModel();
        final DBNNode rootNode = DBWorkbench.getPlatform().getWorkspace().getProjects().size() == 1 ?
            navigatorModel.getRoot().getProjectNode(DBWorkbench.getPlatform().getWorkspace().getActiveProject()) : navigatorModel.getRoot();
        boolean chooseConsumer = settings.isConsumerOptional();
        DBNNode node = DBWorkbench.getPlatformUI().selectObject(
            UIUtils.getActiveWorkbenchShell(),
            chooseConsumer ?
                NLS.bind(DTUIMessages.database_producer_page_input_objects_node_select_target, pipe.getProducer().getDatabaseObject().getName()):
                NLS.bind(DTUIMessages.database_producer_page_input_objects_node_select_source, pipe.getConsumer().getObjectName()),
            rootNode,
            lastSelection,
            new Class[] {DBSInstance.class, DBSObjectContainer.class, DBSDataContainer.class},
            new Class[] {chooseConsumer ? DBSDataManipulator.class : DBSDataContainer.class}, null);
        if (node instanceof DBNDatabaseNode) {
            lastSelection = (DBNDatabaseNode) node;
            DBSObject object = ((DBNDatabaseNode) node).getObject();

            if (chooseConsumer) {
                if (object instanceof DBSDataManipulator) {
                    pipe.setConsumer(new DatabaseTransferConsumer((DBSDataManipulator) object));
                }
            } else {
                if (object instanceof DBSDataContainer) {
                    pipe.setProducer(new DatabaseTransferProducer((DBSDataContainer) object));
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isPageApplicable() {
        return isProducerOfType(DatabaseTransferProducer.class);
    }

}