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
package org.jkiss.dbeaver.tools.transfer.ui.pages.stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCResultSetMetaData;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataImporter;
import org.jkiss.dbeaver.tools.transfer.stream.StreamDataImporterColumnInfo;
import org.jkiss.dbeaver.tools.transfer.stream.StreamProducerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferProducer;
import org.jkiss.dbeaver.tools.transfer.ui.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTableEditor;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.utils.CommonUtils;

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
    private boolean activated;

    public StreamProducerPagePreview() {
        super(DTMessages.data_transfer_wizard_page_preview_name);
        setTitle(DTMessages.data_transfer_wizard_page_preview_title);
        setDescription(DTMessages.data_transfer_wizard_page_preview_description);
        setPageComplete(false);
    }

    private StreamProducerSettings getProducerSettings() {
        return getWizard().getPageSettings(StreamProducerPagePreview.this, StreamProducerSettings.class);
    }

    private StreamProducerSettings.EntityMapping getCurrentEntityMappings() {
        final DBSEntity entity = (DBSEntity) currentObject;
        final StreamProducerSettings settings = getProducerSettings();
        return settings.getEntityMapping(entity);
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
            Composite mappingGroup = new Composite(previewSash, SWT.NONE);
            mappingGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
            mappingGroup.setLayout(new GridLayout(1, false));
            UIUtils.createControlLabel(mappingGroup, DTMessages.data_transfer_wizard_settings_group_column_mappings);

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

                final CustomTableEditor tableEditor = new CustomTableEditor(mappingsTable) {
                    @Override
                    protected Control createEditor(Table table, final int index, final TableItem item) {
                        StreamProducerSettings.AttributeMapping am = (StreamProducerSettings.AttributeMapping) item.getData();
                        StreamProducerSettings.EntityMapping entityMappings = getCurrentEntityMappings();

                        if (index == 1) {
                            // Source column
                            if (am.getMappingType() == StreamProducerSettings.AttributeMapping.MappingType.DEFAULT_VALUE) {
                                Text textValue = new Text(table, SWT.BORDER);
                                textValue.setText(CommonUtils.notEmpty(am.getDefaultValue()));
                                return textValue;
                            } else {
                                final CCombo sourceCombo = new CCombo(table, SWT.DROP_DOWN | SWT.READ_ONLY);
                                sourceCombo.add("");
                                for (StreamDataImporterColumnInfo ci : entityMappings.getStreamColumns()) {
                                    String columnName = ci.getColumnName();
                                    if (CommonUtils.isEmpty(columnName)) {
                                        columnName = String.valueOf(ci.getColumnIndex());
                                    }
                                    sourceCombo.add(columnName);
                                }
                                if (!CommonUtils.isEmpty(am.getSourceAttributeName())) {
                                    sourceCombo.setText(am.getSourceAttributeName());
                                } else if (am.getSourceAttributeIndex() >= 0) {
                                    sourceCombo.setText(String.valueOf(am.getSourceAttributeIndex()));
                                } else {
                                    sourceCombo.select(0);
                                }

                                return sourceCombo;
                            }
                        } else if (index == 2) {
                            // Mapping type

                            final CCombo mappingCombo = new CCombo(table, SWT.DROP_DOWN | SWT.READ_ONLY);
                            for (StreamProducerSettings.AttributeMapping.MappingType mapping : StreamProducerSettings.AttributeMapping.MappingType.values()) {
                                if (mapping == StreamProducerSettings.AttributeMapping.MappingType.NONE) {
                                    continue;
                                }
                                mappingCombo.add(mapping.getTitle());
                            }
                            if (am.getMappingType() == StreamProducerSettings.AttributeMapping.MappingType.NONE) {
                                mappingCombo.setText(StreamProducerSettings.AttributeMapping.MappingType.SKIP.getTitle());
                            } else {
                                mappingCombo.setText(am.getMappingType().getTitle());
                            }
                            return mappingCombo;
                        }
                        return null;
                    }

                    @Override
                    protected void saveEditorValue(Control control, int index, TableItem item) {
                        StreamProducerSettings.AttributeMapping am = (StreamProducerSettings.AttributeMapping) item.getData();
                        if (index == 1) {
                            if (am.getMappingType() == StreamProducerSettings.AttributeMapping.MappingType.DEFAULT_VALUE) {
                                String newValue = ((Text)control).getText();
                                if (CommonUtils.equalObjects(newValue, am.getDefaultValue())) {
                                    return;
                                }
                                am.setDefaultValue(newValue);
                            } else {
                                final CCombo sourceCombo = (CCombo) control;
                                if (sourceCombo.getSelectionIndex() == 0) {
                                    if (am.getMappingType() == StreamProducerSettings.AttributeMapping.MappingType.SKIP) {
                                        return;
                                    }
                                    am.setSourceAttributeIndex(-1);
                                    am.setSourceAttributeName(null);
                                    am.setMappingType(StreamProducerSettings.AttributeMapping.MappingType.SKIP);
                                } else {
                                    String srcAttrName = sourceCombo.getText();
                                    StreamDataImporterColumnInfo streamColumn = getCurrentEntityMappings().getStreamColumn(srcAttrName);
                                    if (streamColumn == null) {
                                        // Index?
                                        int selectionIndex = sourceCombo.getSelectionIndex();
                                        if (selectionIndex >=0) {
                                            streamColumn = getCurrentEntityMappings().getStreamColumns().get(selectionIndex - 1);
                                        } else {
                                            return;
                                        }
                                    }
                                    if (CommonUtils.equalObjects(am.getSourceAttributeName(), streamColumn.getColumnName()) &&
                                        am.getSourceAttributeIndex() == streamColumn.getColumnIndex()) {
                                        return;
                                    }
                                    am.setSourceAttributeName(streamColumn.getColumnName());
                                    am.setSourceAttributeIndex(streamColumn.getColumnIndex());
                                    am.setMappingType(StreamProducerSettings.AttributeMapping.MappingType.IMPORT);
                                }
                            }

                        } else if (index == 2) {
                            final CCombo mappingCombo = (CCombo) control;
                            StreamProducerSettings.AttributeMapping.MappingType newMapping = null;
                            String newTypeTitle = mappingCombo.getText();
                            for (StreamProducerSettings.AttributeMapping.MappingType mapping : StreamProducerSettings.AttributeMapping.MappingType.values()) {
                                if (newTypeTitle.equals(mapping.getTitle())) {
                                    newMapping = mapping;
                                    break;
                                }
                            }
                            if (newMapping != null && newMapping != am.getMappingType()) {
                                am.setMappingType(newMapping);
                            } else {
                                return;
                            }
                        }
                        UIUtils.asyncExec(() -> {
                            updateAttributeMappingItem(am, item);
                            updatePageCompletion();
                            refreshPreviewTable(getCurrentEntityMappings());
                        });
                    }
                };

            }

            mapSash.setWeights(new int[] { 300, 700 } );
        }

        {
            Composite previewGroup = new Composite(previewSash, SWT.NONE);
            previewGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
            previewGroup.setLayout(new GridLayout(1, false));
            UIUtils.createControlLabel(previewGroup, DTMessages.data_transfer_wizard_settings_group_preview);

            previewTable = new Table(previewGroup, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            previewTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            previewTable.setHeaderVisible(true);
            previewTable.setLinesVisible(true);
        }

        setControl(composite);

        determinePageCompletion();
    }

    @Override
    public void activatePage() {
        activated = true;
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
        int selectionIndex = tableList.getSelectionIndex();
        if (selectionIndex < 0) {
            return;
        }
        TableItem tableItem = tableList.getItem(selectionIndex);
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

        final DBSEntity entity = (DBSEntity) currentObject;
        StreamProducerSettings.EntityMapping entityMapping = getCurrentEntityMappings();
        DataTransferPipe currentPipe = getCurrentPipe();
        StreamTransferProducer currentProducer = (StreamTransferProducer) currentPipe.getProducer();

        Throwable error = null;
        try {
            getWizard().getContainer().run(true, true, mon -> {
                IDataTransferProcessor importer = processor.getInstance();

                DBRProgressMonitor monitor = new DefaultProgressMonitor(mon);
                monitor.beginTask("Load mappings", 4);
                try {
                    monitor.subTask("Load attributes form target object");
                    for (DBSEntityAttribute attr : CommonUtils.safeCollection(entity.getAttributes(monitor))) {
                        if (DBUtils.isPseudoAttribute(attr) || DBUtils.isHiddenObject(attr)) {
                            continue;
                        }
                        entityMapping.getAttributeMapping(attr);
                    }
                    monitor.worked(1);

                    monitor.subTask("Update mappings from stream");
                    getProducerSettings().updateMappingsFromStream(getWizard().getSettings());
                    monitor.worked(1);

                    UIUtils.syncExec(() -> updateAttributeMappings(entityMapping));
                    monitor.worked(1);

                    // Load preview
                    monitor.subTask("Load import preview");
                    if (importer instanceof IStreamDataImporter) {
                        loadImportPreview(monitor, (IStreamDataImporter)importer, entity, currentProducer);
                    }
                    monitor.worked(1);

                    monitor.done();

                } catch (Throwable e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            error = e.getTargetException();
        } catch (InterruptedException e) {
            // Ignore
        }
        determinePageCompletion();

        Throwable finalError = error;
        UIUtils.asyncExec(() -> {
            UIUtils.packColumns(mappingsTable, true);
            UIUtils.packColumns(previewTable, false);

            if (finalError != null) {
                log.error(finalError);
                DBWorkbench.getPlatformUI().showError("Load entity meta", "Can't load entity attributes", finalError);
            }
        });

    }

    private void refreshPreviewTable(StreamProducerSettings.EntityMapping entityMapping) {
        previewTable.removeAll();
        for (TableColumn column : previewTable.getColumns()) {
            column.dispose();
        }
        for (StreamProducerSettings.AttributeMapping am : entityMapping.getAttributeMappings()) {
            if (!am.isValuable()) {
                continue;
            }
            // Create preview column
            UIUtils.createTableColumn(previewTable, SWT.LEFT, am.getTargetAttributeName());
        }

        DataTransferProcessorDescriptor processor = getWizard().getSettings().getProcessor();
        final DBSEntity entity = (DBSEntity) currentObject;
        StreamTransferProducer currentProducer = (StreamTransferProducer) getCurrentPipe().getProducer();

        Throwable error = null;
        try {
            getWizard().getContainer().run(true, true, mon -> {
                try {
                    IDataTransferProcessor importer = processor.getInstance();

                    DBRProgressMonitor monitor = new DefaultProgressMonitor(mon);
                    monitor.beginTask("Load preview", 1);

                    // Load preview
                    monitor.subTask("Load import preview");
                    if (importer instanceof IStreamDataImporter) {
                        loadImportPreview(monitor, (IStreamDataImporter)importer, entity, currentProducer);
                    }
                    monitor.worked(1);

                    monitor.done();
                } catch (Throwable e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            error = e.getTargetException();
        } catch (InterruptedException e) {
            // Ignore
        }
        Throwable finalError = error;
        UIUtils.asyncExec(() -> {
            UIUtils.packColumns(previewTable, false);
            if (finalError != null) {
                DBWorkbench.getPlatformUI().showError("OReview data", "Can't load preview data", finalError);
            }
        });
    }

    private void updateAttributeMappings(StreamProducerSettings.EntityMapping entityMapping) {
        for (StreamProducerSettings.AttributeMapping am : entityMapping.getAttributeMappings()) {
            // Create mapping item

            TableItem mappingItem = new TableItem(mappingsTable, SWT.NONE);
            mappingItem.setData(am);
            mappingItem.setImage(DBeaverIcons.getImage(DBValueFormatting.getObjectImage(am.getTargetAttribute())));
            updateAttributeMappingItem(am, mappingItem);

            if (am.isValuable()) {
                // Create preview column
                UIUtils.createTableColumn(previewTable, SWT.LEFT, am.getTargetAttributeName());
            }
        }
    }

    private void updateAttributeMappingItem(StreamProducerSettings.AttributeMapping am, TableItem mappingItem) {
        mappingItem.setText(0, am.getTargetAttributeName());
        StreamProducerSettings.AttributeMapping.MappingType mappingType = am.getMappingType();
        String sourceName = "";
        switch (mappingType) {
            case IMPORT: {
                if (!CommonUtils.isEmpty(am.getSourceAttributeName())) {
                    sourceName = am.getSourceAttributeName();
                } else if (am.getSourceAttributeIndex() >= 0) {
                    sourceName = String.valueOf(am.getSourceAttributeIndex());
                }
                break;
            }
            case DEFAULT_VALUE: {
                sourceName = CommonUtils.notEmpty(am.getDefaultValue());
                break;
            }
            case SKIP: {
                sourceName = "-";
                break;
            }
        }
        mappingItem.setText(1, sourceName);
        mappingItem.setText(2, mappingType.getTitle());
    }

    public DataTransferPipe getCurrentPipe() {
        return pipeList.get(tableList.getSelectionIndex());
    }

    private void loadImportPreview(DBRProgressMonitor monitor, IStreamDataImporter importer, DBSEntity entity, StreamTransferProducer currentProducer) throws DBException {
        final StreamProducerSettings settings = getProducerSettings();

        PreviewConsumer previewConsumer = new PreviewConsumer(settings, entity);

        settings.setProcessorProperties(getWizard().getSettings().getProcessorProperties());
        settings.setMaxRows(10);

        try {
            currentProducer.transferData(monitor, previewConsumer, importer, settings, null);
        } finally {
            previewConsumer.close();
            settings.setMaxRows(-1);
        }

        List<Object[]> rows = previewConsumer.getRows();
        List<String[]> strRows = new ArrayList<>(rows.size());
        try (DBCSession session = DBUtils.openUtilSession(monitor, previewConsumer.getEntityMapping().getEntity(), "Generate preview values")) {
            List<StreamProducerSettings.AttributeMapping> attributeMappings = previewConsumer.getEntityMapping().getValuableAttributeMappings();
            for (Object[] row : rows) {
                String[] strRow = new String[row.length];
                for (int i = 0; i < attributeMappings.size(); i++) {
                    StreamProducerSettings.AttributeMapping attr = attributeMappings.get(i);
                    Object srcValue = row[i];
                    Object value = attr.getTargetValueHandler().getValueFromObject(session, attr.getTargetAttribute(), srcValue, false, activated);
                    String valueStr = attr.getTargetValueHandler().getValueDisplayString(attr.getTargetAttribute(), value, DBDDisplayFormat.UI);
                    strRow[i] = valueStr;
                }
                strRows.add(strRow);
            }
        }

        UIUtils.asyncExec(() -> {
            previewTable.removeAll();
            for (String[] row : strRows) {
                TableItem previewItem = new TableItem(previewTable, SWT.NONE);
                for (int i = 0; i < row.length; i++) {
                    if (row[i] != null) {
                        previewItem.setText(i, row[i]);
                    }
                }
            }
        });
    }

    @Override
    public void deactivatePage()
    {
        super.deactivatePage();

        // Pass properties to producer settings
        final StreamProducerSettings settings = getProducerSettings();
        settings.setProcessorProperties(getWizard().getSettings().getProcessorProperties());
    }

    @Override
    protected boolean determinePageCompletion()
    {
        if (!activated) {
            return false;
        }
        final StreamProducerSettings settings = getProducerSettings();
        List<DataTransferPipe> dataPipes = getWizard().getSettings().getDataPipes();
        if (dataPipes.isEmpty()) {
            setErrorMessage("No entities specified");
            return false;
        }
        for (DataTransferPipe pipe : dataPipes) {
            DBSObject databaseObject = pipe.getConsumer().getDatabaseObject();
            if (!(databaseObject instanceof DBSEntity)) {
                setErrorMessage("Wrong input object");
                return false;
            }
            StreamProducerSettings.EntityMapping entityMapping = settings.getEntityMapping((DBSEntity) databaseObject);
            if (!entityMapping.isComplete()) {
                setErrorMessage("Set mappings for all columns");
                return false;
            }
        }
        setErrorMessage(null);
        return true;
    }

    private static class PreviewConsumer implements IDataTransferConsumer {

        private List<Object[]> rows = new ArrayList<>();
        private StreamProducerSettings settings;
        private DBSEntity sampleObject;
        private final StreamProducerSettings.EntityMapping entityMapping;
        private DBCResultSetMetaData meta;
        private final List<StreamProducerSettings.AttributeMapping> attributes;

        public PreviewConsumer(StreamProducerSettings settings, DBSEntity sampleObject) {
            this.settings = settings;
            this.sampleObject = sampleObject;
            this.entityMapping = this.settings.getEntityMapping(sampleObject);
            this.attributes = entityMapping.getValuableAttributeMappings();
        }

        public StreamProducerSettings.EntityMapping getEntityMapping() {
            return entityMapping;
        }

        public List<Object[]> getRows() {
            return rows;
        }

        @Override
        public void initTransfer(DBSObject sourceObject, IDataTransferSettings settings, TransferParameters parameters, IDataTransferProcessor processor, Map processorProperties) {

        }

        @Override
        public void startTransfer(DBRProgressMonitor monitor) throws DBException {

        }

        @Override
        public void finishTransfer(DBRProgressMonitor monitor, boolean last) {

        }

        @Override
        public void fetchStart(DBCSession session, DBCResultSet resultSet, long offset, long maxRows) throws DBCException {
            meta = resultSet.getMeta();
            if (meta.getAttributes().size() != entityMapping.getValuableAttributeMappings().size()) {
                throw new DBCException("Corrupted stream source metadata. Attribute number (" + meta.getAttributes().size() + ") doesn't match target entity attribute number (" + entityMapping.getAttributeMappings().size() + ")");
            }
        }

        @Override
        public void fetchRow(DBCSession session, DBCResultSet resultSet) throws DBCException {
            Object[] row = new Object[attributes.size()];
            for (int i = 0; i < attributes.size(); i++) {
                switch (attributes.get(i).getMappingType()) {
                    case DEFAULT_VALUE:
                        row[i] = attributes.get(i).getDefaultValue();
                        break;
                    default:
                        if (attributes.get(i).getMappingType() != StreamProducerSettings.AttributeMapping.MappingType.IMPORT) {
                            row[i] = null;
                        } else {
                            row[i] = resultSet.getAttributeValue(i);
                        }
                        break;
                }
            }
            rows.add(row);
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

        @Override
        public DBPImage getObjectIcon() {
            return null;
        }

        @Override
        public String getObjectContainerName() {
            return "N/A";
        }

        @Override
        public DBPImage getObjectContainerIcon() {
            return UIIcon.SQL_PREVIEW;
        }

    }

}