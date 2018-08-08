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
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StreamProducerPagePreview extends ActiveWizardPage<DataTransferWizard> {

    private static final Log log = Log.getLog(StreamProducerPagePreview.class);

    private List<DataTransferPipe> pipeList = new ArrayList<>();
    private Table tableList;
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

        SashForm previewSash = new SashForm(composite, SWT.VERTICAL);
        previewSash.setLayoutData(new GridData(GridData.FILL_BOTH));
        {
            Group mappingGroup = new Group(previewSash, SWT.NONE);
            mappingGroup.setText(DTMessages.data_transfer_wizard_settings_group_column_mappings);
            mappingGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
            mappingGroup.setLayout(new GridLayout(1, false));

            SashForm mapSash = new SashForm(mappingGroup, SWT.HORIZONTAL);
            mapSash.setLayoutData(new GridData(GridData.FILL_BOTH));

            {
                Composite tableComposite = UIUtils.createPlaceholder(mapSash, 1);
                tableComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
                UIUtils.createControlLabel(tableComposite, DTMessages.data_transfer_wizard_settings_group_preview_table);

                tableList = new Table(tableComposite, SWT.BORDER | SWT.SINGLE);
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
            Group previewGroup = new Group(previewSash, SWT.NONE);
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
        pipeList.clear();
        for (DataTransferPipe pipe : getWizard().getSettings().getDataPipes()) {
            if (pipe.getConsumer() != null && pipe.getConsumer().getDatabaseObject() != null) {
                pipeList.add(pipe);
                TableItem tableItem = new TableItem(tableList, SWT.NONE);
                tableItem.setData(pipe);
                tableItem.setImage(DBeaverIcons.getImage(getWizard().getSettings().getConsumer().getIcon()));
                tableItem.setText(pipe.getConsumer().getObjectName());
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
        TableItem tableItem = tableList.getItem(tableList.getSelectionIndex());
        String objectName = tableItem.getText();

        DataTransferPipe pipe = (DataTransferPipe) tableItem.getData();
        if (pipe.getConsumer() != null && objectName.equals(pipe.getConsumer().getObjectName())) {
            if (currentObject == pipe.getConsumer().getDatabaseObject()) {
                // No changes
                return;
            }
            currentObject = pipe.getConsumer().getDatabaseObject();
        }

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
        DataTransferPipe currentPipe = getCurrentPipe();
        StreamTransferProducer currentProducer = (StreamTransferProducer) currentPipe.getProducer();

        try {
            getWizard().getContainer().run(true, true, mon -> {
                IDataTransferProcessor importer = processor.getInstance();

                DBRProgressMonitor monitor = new DefaultProgressMonitor(mon);
                monitor.beginTask("Load mappings", 3);
                try {
                    monitor.subTask("Load attributes form target object");
                    for (DBSEntityAttribute attr : CommonUtils.safeCollection(entity.getAttributes(monitor))) {
                        if (DBUtils.isPseudoAttribute(attr) || DBUtils.isHiddenObject(attr)) {
                            continue;
                        }
                        entityMapping.getAttributeMapping(attr);
                    }
                    monitor.worked(1);

                    // Load header and mappings
                    monitor.subTask("Load attribute mappings");
                    if (importer instanceof IStreamDataImporter) {
                        loadStreamMappings((IStreamDataImporter)importer, entity, currentProducer);
                    }

                    UIUtils.syncExec(() -> updateAttributeMappings(entityMapping));
                    monitor.worked(1);

                    // Load preview
                    monitor.subTask("Load import preview");
                    if (importer instanceof IStreamDataImporter) {
                        loadImportPreview(monitor, (IStreamDataImporter)importer, entity, currentProducer);
                    }
                    monitor.worked(1);

                    monitor.done();

                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            DBUserInterface.getInstance().showError("Load entity meta", "Can't load entity attributes", e.getTargetException());
            return;
        } catch (InterruptedException e) {
            return;
        }


        UIUtils.asyncExec(() -> {
            UIUtils.packColumns(mappingsTable, true);
            UIUtils.packColumns(previewTable, false);
        });
    }

    private void updateAttributeMappings(StreamProducerSettings.EntityMapping entityMapping) {
        for (StreamProducerSettings.AttributeMapping am : entityMapping.getAttributeMappings()) {
            // Create mapping item
            TableItem mappingItem = new TableItem(mappingsTable, SWT.NONE);
            mappingItem.setData(am);
            mappingItem.setImage(DBeaverIcons.getImage(DBValueFormatting.getObjectImage(am.getTargetAttribute())));
            mappingItem.setText(0, am.getTargetAttributeName());
            String sourceName = "<none>";
            if (!CommonUtils.isEmpty(am.getSourceAttributeName())) {
                sourceName = am.getSourceAttributeName();
            } else if (am.getSourceAttributeIndex() >= 0) {
                sourceName = String.valueOf(am.getSourceAttributeIndex());
            }
            mappingItem.setText(1, sourceName);
            mappingItem.setText(2, am.getMappingType().getTitle());

            // Create preview column
            UIUtils.createTableColumn(previewTable, SWT.LEFT, am.getTargetAttributeName());
        }
    }

    public DataTransferPipe getCurrentPipe() {
        return pipeList.get(tableList.getSelectionIndex());
    }

    private void loadStreamMappings(IStreamDataImporter importer, DBSEntity entity, StreamTransferProducer currentProducer) throws DBException {
        File inputFile = currentProducer.getInputFile();

        final StreamProducerSettings settings = getWizard().getPageSettings(this, StreamProducerSettings.class);
        final Map<Object, Object> processorProperties = getWizard().getSettings().getProcessorProperties();

        List<StreamDataImporterColumnInfo> columnInfos;
        try (InputStream is = new FileInputStream(inputFile)) {
            importer.init(new StreamDataImporterSite(settings, entity, processorProperties, 1));
            columnInfos = importer.readColumnsInfo(is);
            importer.dispose();
        } catch (IOException e) {
            throw new DBException("IO error", e);
        }

        // Map source columns
        StreamProducerSettings.EntityMapping entityMapping = settings.getEntityMapping(entity);
        List<StreamProducerSettings.AttributeMapping> attributeMappings = entityMapping.getAttributeMappings();
        for (StreamDataImporterColumnInfo columnInfo : columnInfos) {
            boolean mappingFound = false;
            if (columnInfo.getColumnName() != null) {
                for (StreamProducerSettings.AttributeMapping attr : attributeMappings) {
                    if (CommonUtils.equalObjects(attr.getTargetAttributeName(), columnInfo.getColumnName())) {
                        if (attr.getMappingType() == StreamProducerSettings.AttributeMapping.MappingType.NONE) {
                            // Set source name only if it wasn't set
                            attr.setSourceAttributeName(columnInfo.getColumnName());
                            attr.setSourceAttributeIndex(columnInfo.getColumnIndex());
                            attr.setMappingType(StreamProducerSettings.AttributeMapping.MappingType.IMPORT);
                        }
                        mappingFound = true;
                        break;
                    }
                }
            }
            if (!mappingFound) {
                if (columnInfo.getColumnIndex() >= 0 && columnInfo.getColumnIndex() < attributeMappings.size()) {
                    StreamProducerSettings.AttributeMapping attr = attributeMappings.get(columnInfo.getColumnIndex());
                    if (attr.getMappingType() == StreamProducerSettings.AttributeMapping.MappingType.NONE) {
                        if (!CommonUtils.isEmpty(columnInfo.getColumnName())) {
                            attr.setSourceAttributeName(columnInfo.getColumnName());
                        }
                        attr.setSourceAttributeIndex(columnInfo.getColumnIndex());
                        attr.setMappingType(StreamProducerSettings.AttributeMapping.MappingType.IMPORT);
                    }
                }
            }
        }
    }

    private void loadImportPreview(DBRProgressMonitor monitor, IStreamDataImporter importer, DBSEntity entity, StreamTransferProducer currentProducer) throws DBException {
        final StreamProducerSettings settings = getWizard().getPageSettings(this, StreamProducerSettings.class);
        final Map<Object, Object> processorProperties = getWizard().getSettings().getProcessorProperties();

        PreviewConsumer previewConsumer = new PreviewConsumer(entity);

        try {
            importer.init(new StreamDataImporterSite(settings, entity, processorProperties, 10));
            currentProducer.transferData(monitor, previewConsumer, importer, settings);
            importer.dispose();
        } finally {
            previewConsumer.close();
        }
    }

    @Override
    public void deactivatePage()
    {
        super.deactivatePage();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        final StreamProducerSettings settings = getWizard().getPageSettings(this, StreamProducerSettings.class);
        for (DataTransferPipe pipe : getWizard().getSettings().getDataPipes()) {
            DBSObject databaseObject = pipe.getConsumer().getDatabaseObject();
            if (!(databaseObject instanceof DBSEntity)) {
                return false;
            }
            StreamProducerSettings.EntityMapping entityMapping = settings.getEntityMapping((DBSEntity) databaseObject);
            if (!entityMapping.isComplete()) {
                return false;
            }
        }
        return true;
    }

    private static class PreviewConsumer implements IDataTransferConsumer {

        private List<Object[]> rows = new ArrayList<>();
        private DBSObject sampleObject;

        public PreviewConsumer(DBSObject sampleObject) {
            this.sampleObject = sampleObject;
        }

        public List<Object[]> getRows() {
            return rows;
        }

        @Override
        public void initTransfer(DBSObject sourceObject, IDataTransferSettings settings, boolean isBinary, IDataTransferProcessor processor, Map processorProperties) {

        }

        @Override
        public void startTransfer(DBRProgressMonitor monitor) throws DBException {

        }

        @Override
        public void finishTransfer(DBRProgressMonitor monitor, boolean last) {

        }

        @Override
        public void fetchStart(DBCSession session, DBCResultSet resultSet, long offset, long maxRows) throws DBCException {

        }

        @Override
        public void fetchRow(DBCSession session, DBCResultSet resultSet) throws DBCException {

        }

        @Override
        public void fetchEnd(DBCSession session, DBCResultSet resultSet) throws DBCException {

        }

        @Override
        public void close() {

        }

        @Override
        public DBSObject getDatabaseObject() {
            return sampleObject;
        }

        @Override
        public String getObjectName() {
            return DBUtils.getObjectFullName(sampleObject, DBPEvaluationContext.DML);
        }
    }

}