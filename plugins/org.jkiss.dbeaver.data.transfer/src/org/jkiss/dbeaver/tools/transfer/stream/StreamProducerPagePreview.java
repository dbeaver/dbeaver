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
package org.jkiss.dbeaver.tools.transfer.stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;

public class StreamProducerPagePreview extends ActiveWizardPage<DataTransferWizard> {

    private static final Log log = Log.getLog(StreamProducerPagePreview.class);

    private org.eclipse.swt.widgets.List tableList;
    private Table mappingsTable;
    private Table previewTable;

    private DBSObject currentObject;

    public StreamProducerPagePreview() {
        super(DTMessages.data_transfer_wizard_page_preview_name);
        setTitle(DTMessages.data_transfer_wizard_page_preview_title);
        setDescription(DTMessages.data_transfer_wizard_page_preview_description);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {

        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Group mappingGroup = new Group(composite, SWT.NONE);
            mappingGroup.setText(DTMessages.data_transfer_wizard_settings_group_column_mappings);
            mappingGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
            mappingGroup.setLayout(new GridLayout(1, false));

            SashForm mapSash = new SashForm(mappingGroup, SWT.HORIZONTAL);
            mapSash.setLayoutData(new GridData(GridData.FILL_BOTH));

            {
                Composite tableComposite = UIUtils.createPlaceholder(mapSash, 1);
                tableComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
                UIUtils.createControlLabel(tableComposite, DTMessages.data_transfer_wizard_settings_group_preview_table);

                tableList = new org.eclipse.swt.widgets.List(tableComposite, SWT.BORDER | SWT.SINGLE | SWT.READ_ONLY);
                tableList.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        loadEntityMappingPreview();
                    }
                });
                tableList.setLayoutData(new GridData(GridData.FILL_BOTH));
            }

            {
                Composite mappingComposite = UIUtils.createPlaceholder(mapSash, 1);
                mappingComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
                UIUtils.createControlLabel(mappingComposite, DTMessages.data_transfer_wizard_settings_group_preview_columns);
                mappingsTable = new Table(mappingComposite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
                mappingsTable.setLayoutData(new GridData(GridData.FILL_BOTH));
                mappingsTable.setHeaderVisible(true);
                mappingsTable.setLinesVisible(true);
                UIUtils.createTableColumn(mappingsTable, SWT.LEFT, DTMessages.data_transfer_wizard_final_column_target);
                UIUtils.createTableColumn(mappingsTable, SWT.LEFT, DTMessages.data_transfer_wizard_final_column_source);
                UIUtils.createTableColumn(mappingsTable, SWT.LEFT, DTMessages.data_transfer_wizard_settings_column_mapping_type);
            }

            mapSash.setWeights(new int[] { 300, 700 } );
        }

        {
            Group previewGroup = new Group(composite, SWT.NONE);
            previewGroup.setText(DTMessages.data_transfer_wizard_settings_group_preview);
            previewGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
            previewGroup.setLayout(new GridLayout(1, false));

            previewTable = new Table(previewGroup, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            previewTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            previewTable.setHeaderVisible(true);
            previewTable.setLinesVisible(true);
        }

        setControl(composite);
    }

    @Override
    public void activatePage() {
        tableList.removeAll();
        for (DataTransferPipe pipe : getWizard().getSettings().getDataPipes()) {
            if (pipe.getConsumer() != null && pipe.getConsumer().getDatabaseObject() != null) {
                tableList.add(pipe.getConsumer().getObjectName());
                if (currentObject != null && currentObject == pipe.getConsumer().getDatabaseObject()) {
                    tableList.select(tableList.getItemCount() - 1);
                }
            }
        }
        if (tableList.getSelectionIndex() < 0) {
            tableList.select(0);
        }
        currentObject = null;
        loadEntityMappingPreview();

        updatePageCompletion();
    }

    private void loadEntityMappingPreview() {
        String objectName = tableList.getItem(tableList.getSelectionIndex());
        DBSObject newCurrentObject = null;
        for (DataTransferPipe pipe : getWizard().getSettings().getDataPipes()) {
            if (pipe.getConsumer() != null && objectName.equals(pipe.getConsumer().getObjectName())) {
                if (currentObject == pipe.getConsumer().getDatabaseObject()) {
                    // No changes
                    return;
                }
                newCurrentObject = pipe.getConsumer().getDatabaseObject();
                break;
            }
        }
        currentObject = newCurrentObject;

        previewTable.removeAll();
        for (TableColumn column : previewTable.getColumns()) {
            column.dispose();
        }
        mappingsTable.removeAll();

        if (currentObject == null) {
            log.error("Can't find object '" + objectName + "'");
            return;
        }

        if (!(currentObject instanceof DBSEntity)) {
            log.error("Object '" + objectName + "' is not an entity. Can't map");
            return;
        }

        DataTransferProcessorDescriptor processor = getWizard().getSettings().getProcessor();
        final StreamProducerSettings settings = getWizard().getPageSettings(this, StreamProducerSettings.class);

        final DBSEntity entity = (DBSEntity) currentObject;
        StreamProducerSettings.EntityMapping entityMapping = settings.getEntityMapping(entity);

        try {
            getWizard().getContainer().run(true, true, mon -> {
                DBRProgressMonitor monitor = new DefaultProgressMonitor(mon);
                try {
                    for (DBSEntityAttribute attr : CommonUtils.safeCollection(entity.getAttributes(monitor))) {
                        StreamProducerSettings.AttributeMapping am = entityMapping.getAttributeMapping(attr);
                    }

                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            DBUserInterface.getInstance().showError("Load entity meta", "Can't load entity attributes", e);
            return;
        } catch (InterruptedException e) {
            return;
        }

        for (StreamProducerSettings.AttributeMapping am : entityMapping.getAttributeMappings()) {
            // Create mapping item
            TableItem mappingItem = new TableItem(mappingsTable, SWT.NONE);
            mappingItem.setData(am);
            mappingItem.setImage(DBeaverIcons.getImage(DBValueFormatting.getObjectImage(am.getTargetAttribute())));
            mappingItem.setText(0, am.getTargetAttributeName());
            mappingItem.setText(1, CommonUtils.isEmpty(am.getSourceAttributeName()) ? "<none>" : am.getSourceAttributeName());

            // Create preview column
            UIUtils.createTableColumn(previewTable, SWT.LEFT, am.getTargetAttributeName());
        }

        UIUtils.asyncExec(() -> {
            UIUtils.packColumns(mappingsTable, true);
            UIUtils.packColumns(previewTable, false);
        });
    }

    @Override
    public void deactivatePage()
    {
        super.deactivatePage();
    }

    @Override
    protected boolean determinePageCompletion()
    {

        return true;
    }

}