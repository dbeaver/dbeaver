/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.transfer.database.page;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBWorkbench;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseProducerSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

public class DatabaseProducerPageInputObjects extends ActiveWizardPage<DataTransferWizard> {

    private Table mappingTable;
    private DBNDatabaseNode lastSelection;

    public DatabaseProducerPageInputObjects() {
        super("Input objects");
        setTitle("Select input objects");
        setDescription("Choose database objects to import");
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        DataTransferSettings settings = getWizard().getSettings();

        {
            Group tablesGroup = UIUtils.createControlGroup(composite, DTMessages.data_transfer_wizard_mappings_name, 3, GridData.FILL_BOTH, 0);

            mappingTable = new Table(tablesGroup, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            mappingTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            mappingTable.setHeaderVisible(true);
            mappingTable.setLinesVisible(true);

            UIUtils.createTableColumn(mappingTable, SWT.LEFT, DTMessages.data_transfer_wizard_final_column_source);
            UIUtils.createTableColumn(mappingTable, SWT.LEFT, DTMessages.data_transfer_wizard_final_column_target);

            for (DataTransferPipe pipe : settings.getDataPipes()) {
                TableItem item = new TableItem(mappingTable, SWT.NONE);
                item.setData(pipe);
                updateItemData(item, pipe);
            }

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

        setControl(composite);

    }

    private void updateItemData(TableItem item, DataTransferPipe pipe) {
        DataTransferSettings settings = getWizard().getSettings();

        if (pipe.getProducer() == null || pipe.getProducer().getDatabaseObject() == null) {
            item.setImage(0, null);
            item.setText(0, "<none>");
        } else {
            item.setImage(0, DBeaverIcons.getImage(settings.getProducer().getIcon()));
            item.setText(0, DBUtils.getObjectFullName(pipe.getProducer().getDatabaseObject(), DBPEvaluationContext.DML));
        }
        if (pipe.getConsumer() == null || pipe.getConsumer().getObjectName() == null) {
            item.setImage(1, null);
            item.setText(1, "<none>");
        } else {
            item.setImage(1, DBeaverIcons.getImage(settings.getConsumer().getIcon()));
            item.setText(1, pipe.getConsumer().getObjectName());
        }
    }

    @Override
    public void activatePage()
    {
        final DatabaseProducerSettings settings = getWizard().getPageSettings(this, DatabaseProducerSettings.class);

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

    protected boolean chooseEntity(DataTransferPipe pipe)
    {
        DataTransferSettings settings = getWizard().getSettings();

        final DBNModel navigatorModel = DBWorkbench.getPlatform().getNavigatorModel();
        final DBNNode rootNode = DBWorkbench.getPlatform().getLiveProjects().size() == 1 ?
            navigatorModel.getRoot().getProject(DBWorkbench.getPlatform().getProjectManager().getActiveProject()) : navigatorModel.getRoot();
        boolean chooseConsumer = settings.isConsumerOptional();
        DBNNode node = DBUserInterface.getInstance().selectObject(
            UIUtils.getActiveWorkbenchShell(),
            chooseConsumer ?
                "Select target entity for '" + pipe.getProducer().getDatabaseObject().getName()  + "'" :
                "Select source container for '" + pipe.getConsumer().getObjectName() + "'",
            rootNode,
            lastSelection,
            new Class[] {DBSObjectContainer.class, DBSDataContainer.class},
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

}