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
package org.jkiss.dbeaver.tools.transfer.stream;

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.meta.DBSerializable;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.sql.SQLQueryContainer;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.serialize.DBPObjectSerializer;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI.UserChoiceResponse;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.tools.transfer.DTUtils;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferEventProcessor;
import org.jkiss.dbeaver.tools.transfer.internal.DTActivator;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferEventProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings.BlobFileConflictBehavior;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings.ConsumerRuntimeParameters;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings.DataFileConflictBehavior;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.Base64;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.io.ByteOrderMark;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Stream transfer consumer
 */
@DBSerializable("streamTransferConsumer")
public class StreamTransferConsumer implements IDataTransferConsumer<StreamConsumerSettings, IStreamDataExporter> {

    private static final Log log = Log.getLog(StreamTransferConsumer.class);

    private static final String LOB_DIRECTORY_NAME = "files"; //$NON-NLS-1$
    private static final String PROP_FORMAT = "format"; //$NON-NLS-1$

    public static final String NODE_ID = "streamTransferConsumer";

    public static final String VARIABLE_DATASOURCE = "datasource";
    public static final String VARIABLE_CATALOG = "catalog";
    public static final String VARIABLE_SCHEMA = "schema";
    public static final String VARIABLE_TABLE = "table";
    public static final String VARIABLE_TIMESTAMP = "timestamp";
    public static final String VARIABLE_INDEX = "index";
    public static final String VARIABLE_DATE = "date";
    public static final String VARIABLE_PROJECT = "project";
    public static final String VARIABLE_CONN_TYPE = "connectionType";
    public static final String VARIABLE_FILE = "file";
    public static final String VARIABLE_SCRIPT_FILE = "scriptFilename";

    public static final String VARIABLE_YEAR = "year";
    public static final String VARIABLE_MONTH = "month";
    public static final String VARIABLE_DAY = "day";
    public static final String VARIABLE_HOUR = "hour";
    public static final String VARIABLE_MINUTE = "minute";

    public static final String[][] VARIABLES = {
        {VARIABLE_DATASOURCE, "source database datasource"},
        {VARIABLE_CATALOG, "source database catalog"},
        {VARIABLE_SCHEMA, "source database schema"},
        {VARIABLE_TABLE, "source database table"},
        {VARIABLE_INDEX, "index of current file (if split is used)"},
        {VARIABLE_PROJECT, "source database project"},
        {VARIABLE_CONN_TYPE, "source database connection type"},
        {VARIABLE_FILE, "output file path"},
        {VARIABLE_SCRIPT_FILE, "source script filename"},
        {VARIABLE_TIMESTAMP, "current timestamp"},
        {VARIABLE_DATE, "current date"},
        {VARIABLE_YEAR, "current year"},
        {VARIABLE_MONTH, "current month"},
        {VARIABLE_DAY, "current day"},
        {VARIABLE_HOUR, "current hour"},
        {VARIABLE_MINUTE, "current minute"},
    };

    public static final int OUT_FILE_BUFFER_SIZE = 100000;

    private IStreamDataExporter processor;
    private StreamConsumerSettings settings;
    private ConsumerRuntimeParameters runtimeParameters;
    private DBSDataContainer dataContainer;

    private OutputStream outputStream;
    private ZipOutputStream zipStream;
    private PrintWriter writer;
    private int multiFileNumber;
    private long bytesWritten = 0;

    private DBDAttributeBinding[] columnMetas;
    private DBDAttributeBinding[] columnBindings;
    private File lobDirectory;
    private long lobCount;
    private File outputFile;
    private StreamExportSite exportSite;
    private Map<String, Object> processorProperties;
    private StringWriter outputBuffer;
    private boolean initialized = false;
    private boolean firstRow = true;
    private TransferParameters parameters;

    private final List<File> outputFiles = new ArrayList<>();
    private StatOutputStream statStream;
    
    public StreamTransferConsumer() {
    }

    protected long getBytesWritten() {
        return statStream == null ? 0 : statStream.getBytesWritten();
    }

    @Override
    public void fetchStart(DBCSession session, DBCResultSet resultSet, long offset, long maxRows) throws DBCException {
        if (!initialized) {
            // Can be invoked multiple times in case of per-segment transfer
            initExporter(session);
        }

        // Prepare columns
        columnMetas = DBUtils.getAttributeBindings(session, dataContainer, resultSet.getMeta());
        if (processor instanceof IDocumentDataExporter) {
            columnBindings = DBUtils.injectAndFilterAttributeBindings(session.getDataSource(), dataContainer, columnMetas, true);
        } else {
            columnBindings = DBUtils.makeLeafAttributeBindings(session, dataContainer, resultSet);
        }

        final StreamMappingContainer mapping = settings.getDataMapping(dataContainer);
        if (mapping != null && mapping.isComplete()) {
            // That's a dirty way of doing things ...
            columnBindings = Arrays.stream(columnBindings)
                .filter(attr -> {
                    final StreamMappingAttribute attribute = mapping.getAttribute(attr);
                    return attribute == null || attribute.getMappingType() == StreamMappingType.export;
                })
                .toArray(DBDAttributeBinding[]::new);
        }

        if (!initialized) {
            /*// For multi-streams export header only once
            if (!settings.isUseSingleFile() || parameters.orderNumber == 0) */{
                exportHeaderInFile(session);
            }
        }

        initialized = true;
    }

    @Override
    public void fetchRow(DBCSession session, DBCResultSet resultSet) throws DBCException {
        try {
            // Check for file split
            if (settings.isSplitOutFiles() && !parameters.isBinary && !firstRow) {
                writer.flush();
                if (bytesWritten >= settings.getMaxOutFileSize()) {
                    // First add footer for the previous file
                    exportFooterInFile(session.getProgressMonitor());
                    // Make new file with the header
                    createNewOutFile();
                    exportHeaderInFile(session);
                }
            }

            // Get values
            Object[] srcRow = fetchRow(session, resultSet, columnMetas);
            Object[] targetRow;
            targetRow = new Object[columnBindings.length];
            for (int i = 0; i < columnBindings.length; i++) {
                DBDAttributeBinding column = columnBindings[i];
                Object value = DBUtils.getAttributeValue(column, columnMetas, srcRow);
                if (value instanceof DBDContent) {
                    // Check for binary type export
                    if (!ContentUtils.isTextContent((DBDContent) value)) {
                        switch (settings.getLobExtractType()) {
                            case SKIP:
                                // Set it it null
                                value = null;
                                break;
                            case INLINE:
                                // Just pass content to exporter
                                break;
                            case FILES:
                                if (!settings.isOutputClipboard()) {
                                    // Save content to file and pass file reference to exporter
                                    value = saveContentToFile(session.getProgressMonitor(), (DBDContent) value);
                                }
                                break;
                        }
                    }
                }
                targetRow[i] = value;
            }
            // Export row
            processor.exportRow(session, resultSet, targetRow);
            firstRow = false;
        } catch (IOException e) {
            throw new DBCException("IO error", e);
        } catch (Throwable e) {
            throw new DBCException("Error while exporting table row", e);
        }
    }

    private void exportHeaderInFile(@NotNull DBCSession session) throws DBCException {
        try {
            processor.exportHeader(session);
        } catch (DBException e) {
            log.warn("Error while exporting table header", e);
        } catch (IOException e) {
            throw new DBCException("IO error", e);
        }
    }

    private void exportFooterInFile(@NotNull DBRProgressMonitor monitor) {
        if (processor != null) {
            try {
                processor.exportFooter(monitor);
            } catch (Exception e) {
                log.warn("Error while exporting table footer", e);
            }
        }
    }

    @Override
    public void fetchEnd(DBCSession session, DBCResultSet resultSet) throws DBCException {
    }

    @Override
    public void close() {
        columnBindings = null;
    }
    
    private boolean resolveOverwriteBlobFileConflict(@NotNull String fileName) {
        BlobFileConflictBehavior behavior = runtimeParameters.blobFileConflictBehavior;
    
        if (behavior == BlobFileConflictBehavior.ASK) {
            List<String> forAllLabels = settings.isUseSingleFile()
                ? List.of(DTMessages.data_transfer_file_conflict_behavior_apply_to_all)
                : List.of(
                    DTMessages.data_transfer_file_conflict_behavior_apply_to_all,
                    DTMessages.data_transfer_file_conflict_behavior_apply_to_all_for_current_object
                );
            UserChoiceResponse response = DBWorkbench.getPlatformUI().showUserChoice(
                DTMessages.data_transfer_blob_file_conflict_title, NLS.bind(DTMessages.data_transfer_file_conflict_ask_message, fileName),
                List.of(
                    BlobFileConflictBehavior.PATCHNAME.title,
                    BlobFileConflictBehavior.OVERWRITE.title,
                    DTMessages.data_transfer_file_conflict_cancel
                ),
                forAllLabels, runtimeParameters.blobFileConflictPreviousChoice, 1
            );
            if (response.choiceIndex < 0) {
                throw new RuntimeException("Blob file name conflict behavior is not specified while " + fileName + " already exists");
            }
            if (response.choiceIndex > 1) {
                throw new RuntimeException("User cancel during existing file resolution for blob " + fileName);
            }
            behavior = new BlobFileConflictBehavior[] {
                BlobFileConflictBehavior.PATCHNAME,
                BlobFileConflictBehavior.OVERWRITE
            }[response.choiceIndex];
            
            runtimeParameters.blobFileConflictPreviousChoice = response.choiceIndex;
            if (response.forAllChoiceIndex != null) {
                runtimeParameters.blobFileConflictBehavior = behavior;
                runtimeParameters.dontDropBlobFileConflictBehavior = response.forAllChoiceIndex == 0;
            }
        }
        
        return behavior == BlobFileConflictBehavior.OVERWRITE;
    }

    private File saveContentToFile(DBRProgressMonitor monitor, DBDContent content)
        throws IOException, DBCException {
        DBDContentStorage contents = content.getContents(monitor);
        if (DBUtils.isNullValue(contents)) {
            return null;
        }
        if (lobDirectory == null) {
            lobDirectory = new File(getOutputFolder(), LOB_DIRECTORY_NAME);
            if (!lobDirectory.exists()) {
                if (!lobDirectory.mkdir()) {
                    throw new IOException("Can't create directory for CONTENT files: " + lobDirectory.getAbsolutePath());
                }
            }
        }
        lobCount++;
        Boolean extractImages = (Boolean) processorProperties.get(StreamConsumerSettings.PROP_EXTRACT_IMAGES);
        String fileExt = (extractImages != null && extractImages) ? ".jpg" : ".data";
        File lobFile = makeLobFileName(null, fileExt);
        if (lobFile.isFile()) {
            if (!resolveOverwriteBlobFileConflict(lobFile.getName())) {
                lobFile = makeLobFileName("-" + System.currentTimeMillis(), fileExt);
            }
        }
        
        try (InputStream cs = contents.getContentStream()) {
            ContentUtils.saveContentToFile(cs, lobFile, monitor);
        }

        return lobFile;
    }
    
    private File makeLobFileName(String suffix, String fileExt) {
        String name = outputFile.getName() + "-" + lobCount;
        if (CommonUtils.isNotEmpty(suffix)) {
            name += suffix;
        }
        return new File(lobDirectory, name + fileExt); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void initExporter(DBCSession session) throws DBCException {
        if (settings.getFormatterProfile() != null && session instanceof DBDFormatSettingsExt) {
            ((DBDFormatSettingsExt)session).setDataFormatterProfile(settings.getFormatterProfile());
        }

        exportSite = new StreamExportSite();

        // Open output streams
        boolean outputClipboard = settings.isOutputClipboard();
        if (parameters.isBinary || !outputClipboard) {
            outputFile = makeOutputFile();
            outputFiles.add(outputFile);
        } else {
            outputFile = null;
        }

        try {
            if (outputClipboard) {
                this.outputBuffer = new StringWriter(2048);
                this.writer = new PrintWriter(this.outputBuffer, true);
            } else {
                openOutputStreams();
            }
        } catch (IOException e) {
            closeExporter();
            throw new DBCException("Data transfer IO error", e);
        }

        try {
            // init exporter
            processor.init(exportSite);
        } catch (DBException e) {
            throw new DBCException("Can't initialize data exporter", e);
        }
    }

    private void closeExporter() {
        if (exportSite != null) {
            try {
                exportSite.flush();
            } catch (IOException e) {
                log.debug(e);
            }
        }

        if (processor != null) {
            // Dispose exporter
            try {
                processor.dispose();
            } catch (Exception e) {
                log.debug(e);
            }
            processor = null;
        }
        closeOutputStreams();
    }
    
    private DataFileConflictBehavior prepareDataFileConflictBehavior(String fileName) {
        DataFileConflictBehavior behavior = runtimeParameters.dataFileConflictBehavior;
        
        if (behavior == DataFileConflictBehavior.ASK) {
            List<String> forAllLabels = settings.isUseSingleFile()
                ? List.of()
                : List.of(DTMessages.data_transfer_file_conflict_behavior_apply_to_all);
            UserChoiceResponse response = DBWorkbench.getPlatformUI().showUserChoice(
                DTMessages.data_transfer_file_conflict_ask_title, NLS.bind(DTMessages.data_transfer_file_conflict_ask_message, fileName),
                Arrays.asList(
                    processor instanceof IAppendableDataExporter ? DataFileConflictBehavior.APPEND.title : null,
                    DataFileConflictBehavior.PATCHNAME.title,
                    DataFileConflictBehavior.OVERWRITE.title,
                    DTMessages.data_transfer_file_conflict_cancel
                ),
                forAllLabels, runtimeParameters.dataFileConflictPreviousChoice, 2
            );
            if (response.choiceIndex > 2) {
                throw new RuntimeException("User cancel during existing file resolution for data " + fileName);
            }
            if (response.choiceIndex < 0) {
                throw new RuntimeException("Data file name conflict behavior is not specified while " + fileName + " already exists");
            }
            behavior = new DataFileConflictBehavior[] {
                DataFileConflictBehavior.APPEND,
                DataFileConflictBehavior.PATCHNAME,
                DataFileConflictBehavior.OVERWRITE
            }[response.choiceIndex];
            
            runtimeParameters.dataFileConflictPreviousChoice = response.choiceIndex;
            if (response.forAllChoiceIndex != null || settings.isUseSingleFile()) {
                runtimeParameters.dataFileConflictBehavior = behavior;
            }
        }

        if (settings.isUseSingleFile() && parameters.orderNumber > 0) { 
            // all consequent sources in a session should be appended  to the first file 
            behavior = DataFileConflictBehavior.APPEND;
        }

        if (behavior == DataFileConflictBehavior.APPEND) {
            if (processor instanceof IAppendableDataExporter) {
                try {
                    ((IAppendableDataExporter) processor).importData(exportSite);
                } catch (DBException e) {
                    log.warn("Error importing existing data for appending, data loss might occur", e);
                }
                if (((IAppendableDataExporter) processor).shouldTruncateOutputFileBeforeExport()) {
                    // appendable but not patchable file should be overwritten after the old data was preloaded
                    behavior = DataFileConflictBehavior.OVERWRITE;
                }
            } else {
                // if we still want to append but the file is non-appendable, so it should be patchnamed
                behavior = DataFileConflictBehavior.PATCHNAME;                
            }
        }
        
        return behavior;
    }
    
    private void openOutputStreams() throws IOException {
        final boolean truncate;
        
        if (outputFile.isFile()) {
            DataFileConflictBehavior behavior = prepareDataFileConflictBehavior(outputFile.getName());
            switch (behavior) {
                case APPEND:
                    truncate = false;
                    break;
                case PATCHNAME:
                    truncate = false;
                    outputFile = makeOutputFile("-" + System.currentTimeMillis());
                    break;
                case OVERWRITE:
                    truncate = true;
                    break;
                default:
                    throw new RuntimeException("Unexpected data file conflict behavior " + behavior);
            }
        } else {
            truncate = true;
        }

        this.outputStream = new BufferedOutputStream(new FileOutputStream(outputFile, !truncate), OUT_FILE_BUFFER_SIZE);
        this.outputStream = this.statStream = new StatOutputStream(outputStream);

        if (settings.isCompressResults()) {
            this.zipStream = new ZipOutputStream(this.outputStream);
            this.zipStream.putNextEntry(new ZipEntry(getOutputFileName()));
            this.outputStream = zipStream;
        }

        // If we need to split files - use stream wrapper to calculate file size
        if (settings.isSplitOutFiles()) {
            this.outputStream = new OutputStreamStatProxy(this.outputStream);
        }

        // Check for BOM and write it to the stream
        if (!parameters.isBinary && settings.isOutputEncodingBOM()) {
            try {
                final ByteOrderMark bom = ByteOrderMark.fromCharset(settings.getOutputEncoding());
                outputStream.write(bom.getBytes());
                outputStream.flush();
            } catch (IllegalArgumentException e) {
                log.debug("Error writing byte order mask", e);
            }
        }

        if (!parameters.isBinary) {
            this.writer = new PrintWriter(new OutputStreamWriter(this.outputStream, settings.getOutputEncoding()), true);
        }
    }

    private void closeOutputStreams() {
        if (this.writer != null) {
            this.writer.flush();
        }

        // Finish zip stream
        if (zipStream != null) {
            try {
                zipStream.closeEntry();
            } catch (IOException e) {
                log.debug(e);
            }
            try {
                zipStream.finish();
            } catch (IOException e) {
                log.debug(e);
            }
            zipStream = null;
        }

        if (outputStream != null) {
            try {
                outputStream.flush();
            } catch (IOException e) {
                log.debug(e);
            }
            ContentUtils.close(outputStream);
            outputStream = null;
        }
    }

    private void createNewOutFile() throws IOException {
        closeOutputStreams();

        bytesWritten = 0;
        multiFileNumber++;
        outputFile = makeOutputFile();
        outputFiles.add(outputFile);

        openOutputStreams();
    }

    @Override
    public void initTransfer(
        @NotNull DBSObject sourceObject,
        @NotNull StreamConsumerSettings settings,
        @NotNull TransferParameters parameters,
        @NotNull IStreamDataExporter processor,
        @NotNull Map<String, Object> processorProperties
    ) {
        this.dataContainer = (DBSDataContainer) sourceObject;
        this.parameters = parameters;
        this.processor = processor;
        this.settings = settings;
        this.processorProperties = processorProperties;
        
        if (runtimeParameters == null) {
            runtimeParameters = settings.prepareRuntimeParameters();
        } else {
            runtimeParameters.initForConsumer();
        }
    }
    
    @Override
    public void setRuntimeParameters(Object runtimeParameters) {
        if (runtimeParameters instanceof ConsumerRuntimeParameters) {
            this.runtimeParameters = (ConsumerRuntimeParameters) runtimeParameters;
        } else {
            throw new IllegalStateException("Unsupported stream transfer consumer runtime parameters " + runtimeParameters);
        }
    }

    @Override
    public void startTransfer(DBRProgressMonitor monitor) {
        // do nothing
    }

    @Override
    public void finishTransfer(DBRProgressMonitor monitor, boolean last) {
        if (!last) {
            exportFooterInFile(monitor);

            closeExporter();
            return;
        }

        if (!parameters.isBinary && settings.isOutputClipboard()) {
            if (outputBuffer != null) {
                String strContents = outputBuffer.toString();
                DBWorkbench.getPlatformUI().copyTextToClipboard(strContents, parameters.isHTML);
                outputBuffer = null;
            }
        }

        final DataTransferRegistry registry = DataTransferRegistry.getInstance();
        for (Map.Entry<String, Map<String, Object>> entry : settings.getEventProcessors().entrySet()) {
            final DataTransferEventProcessorDescriptor descriptor = registry.getEventProcessorById(entry.getKey());
            if (descriptor == null) {
                log.debug("Can't find event processor '" + entry.getKey() + "'");
                continue;
            }
            try {
                final IDataTransferEventProcessor<StreamTransferConsumer> processor = descriptor.create();
                processor.processEvent(monitor, IDataTransferEventProcessor.Event.FINISH, this, entry.getValue());
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Transfer event processor", "Error executing data transfer event processor '" + entry.getKey() + "'", e);
                log.error("Error executing event processor '" + entry.getKey() + "'", e);
            }
        }
    }

    @Override
    public Object getTargetObject() {
        return null;
    }

    @Nullable
    @Override
    public Object getTargetObjectContainer() {
        return null;
    }

    @Override
    public String getObjectName() {
        return settings.isOutputClipboard() ? "Clipboard" : makeOutputFile().getName();
    }

    @Override
    public DBPImage getObjectIcon() {
        return null;
    }

    @Override
    public String getObjectContainerName() {
        return settings.isOutputClipboard() ? "Clipboard" : makeOutputFile().getParentFile().getAbsolutePath();
    }

    @Override
    public DBPImage getObjectContainerIcon() {
        return settings.isOutputClipboard() ? DBIcon.TYPE_TEXT : DBIcon.TREE_FOLDER;
    }

    @Override
    public boolean isConfigurationComplete() {
        return true;
    }

    public boolean isBeforeFirstRow() {
        return firstRow;
    }

    @NotNull
    public String getOutputFolder() {
        return translatePattern(settings.getOutputFolder(), null);
    }

    @NotNull
    public List<File> getOutputFiles() {
        return outputFiles;
    }

    @NotNull
    public String getOutputFileName() {
        return getOutputFileName(null);
    }
    
    @NotNull
    private String getOutputFileName(@Nullable String suffix) {
        Object extension = processorProperties == null ? null : processorProperties.get(StreamConsumerSettings.PROP_FILE_EXTENSION);
        String fileName = CommonUtils.notNull(
            runtimeParameters.outputFileNameToReuse, 
            translatePattern(settings.getOutputFilePattern(), null).trim()
        );
        // Can't rememeber why did we need this. It breaks file names in case of multiple tables export (#6911)
        // if (parameters.orderNumber > 0 && !settings.isUseSingleFile()) {
        //    fileName += "_" + String.valueOf(parameters.orderNumber + 1);
        //}
        if (CommonUtils.isNotEmpty(suffix)) {
            fileName += suffix;
        }
        if (settings.isUseSingleFile() && suffix != null) {
            runtimeParameters.outputFileNameToReuse = fileName;
        }

        if (multiFileNumber > 0) {
            fileName += "_" + (multiFileNumber + 1);
        }
        if (extension != null) {
            return fileName + "." + extension;
        } else {
            return fileName;
        }
    }

    @NotNull
    public File makeOutputFile() {
        return makeOutputFile(null);
    }
    
    @NotNull
    private File makeOutputFile(@Nullable String suffix) {
        final File file = makeOutputFile(suffix, getOutputFolder());

        if (!file.exists()) {
            try (FileOutputStream ignored = new FileOutputStream(file)) {
                return file;
            } catch (IOException ignored) {
                return makeOutputFile(suffix, getFallbackOutputFolder());
            } finally {
                file.delete();
            }
        }
        return file;
    }

    @NotNull
    private File makeOutputFile(@Nullable String suffix, @NotNull String outputFolder) {
        File dir = new File(outputFolder);
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("Can't create output directory '" + dir.getAbsolutePath() + "'");
        }
        String fileName = getOutputFileName(suffix);
        if (settings.isCompressResults()) {
            fileName += ".zip";
        }
        return new File(dir, fileName);
    }

    public String translatePattern(String pattern, final File targetFile) {
        final Date ts;
        if (parameters.startTimestamp != null) {
            // Use saved timestamp (#7352)
            ts = parameters.startTimestamp;
        } else {
            ts = new Date();
        }

        return GeneralUtils.replaceVariables(pattern, name -> {
            switch (name) {
                case VARIABLE_DATASOURCE: {
                    if (settings.isUseSingleFile()) {
                        return "";
                    }
                    return stripObjectName(dataContainer.getDataSource().getContainer().getName());
                }
                case VARIABLE_CATALOG: {
                    if (settings.isUseSingleFile()) {
                        return "";
                    }
                    DBSCatalog catalog = DBUtils.getParentOfType(DBSCatalog.class, dataContainer);
                    return catalog == null ? "" : stripObjectName(catalog.getName());
                }
                case VARIABLE_SCHEMA: {
                    if (settings.isUseSingleFile()) {
                        return "";
                    }
                    DBSSchema schema = DBUtils.getParentOfType(DBSSchema.class, dataContainer);
                    if (schema != null) {
                        return stripObjectName(schema.getName());
                    }
                    // Try catalog (#7506)
                    DBSCatalog catalog = DBUtils.getParentOfType(DBSCatalog.class, dataContainer);
                    return catalog == null ? "" : stripObjectName(catalog.getName());
                }
                case VARIABLE_TABLE: {
                    if (settings.isUseSingleFile()) {
                        return DTConstants.DEFAULT_TABLE_NAME_EXPORT;
                    }
                    if (dataContainer == null) {
                        return null;
                    }
                    String tableName;
                    if (dataContainer instanceof SQLQueryContainer) {
                        tableName = DTUtils.getTableNameFromQueryContainer(dataContainer.getDataSource(), (SQLQueryContainer) dataContainer);
                        if (CommonUtils.isEmpty(tableName)) {
                            tableName = DTUtils.getTargetContainersNameFromQuery((SQLQueryContainer) dataContainer);
                        }
                    } else {
                        tableName = DTUtils.getTableName(dataContainer.getDataSource(), dataContainer, true);
                    }
                    if (CommonUtils.isEmpty(tableName)) {
                        if (parameters.orderNumber > 0) {
                            tableName = DTConstants.DEFAULT_TABLE_NAME_EXPORT + "_" + parameters.orderNumber;
                        } else {
                            tableName = DTConstants.DEFAULT_TABLE_NAME_EXPORT;
                        }
                    }
                    return stripObjectName(tableName);
                }
                case VARIABLE_TIMESTAMP:
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(settings.getOutputTimestampPattern());
                        return sdf.format(ts);
                    } catch (Exception e) {
                        log.error(e);
                        return "BAD_TIMESTAMP";
                    }
                case VARIABLE_DATE:
                    return RuntimeUtils.getCurrentDate();
                case VARIABLE_YEAR:
                    return new SimpleDateFormat("yyyy").format(ts);
                case VARIABLE_MONTH:
                    return new SimpleDateFormat("MM").format(ts);
                case VARIABLE_DAY:
                    return new SimpleDateFormat("dd").format(ts);
                case VARIABLE_HOUR:
                    return new SimpleDateFormat("HH").format(ts);
                case VARIABLE_MINUTE:
                    return new SimpleDateFormat("mm").format(ts);
                case VARIABLE_INDEX:
                    return String.valueOf(parameters.orderNumber + 1);
                case VARIABLE_PROJECT: {
                    if (dataContainer == null) {
                        return null;
                    }
                    DBPProject project = DBUtils.getObjectOwnerProject(dataContainer);
                    return project == null ? "" : project.getName();
                }
                case VARIABLE_FILE:
                    return targetFile == null ? "" : targetFile.getAbsolutePath();
                case VARIABLE_SCRIPT_FILE: {
                    final SQLQueryContainer container = DBUtils.getAdapter(SQLQueryContainer.class, dataContainer);
                    if (container != null) {
                        final File file = container.getScriptContext().getSourceFile();
                        if (file != null) {
                            String filename = file.getName();
                            if (filename.indexOf('.') >= 0) {
                                filename = filename.substring(0, filename.lastIndexOf('.'));
                            }
                            return filename;
                        }
                    }
                    break;
                }
                case VARIABLE_CONN_TYPE:
                    if (dataContainer == null) {
                        return null;
                    }
                    return dataContainer.getDataSource().getContainer().getConnectionConfiguration().getConnectionType().getId();
            }
            final SQLQueryContainer container = DBUtils.getAdapter(SQLQueryContainer.class, dataContainer);
            if (container != null) {
                return CommonUtils.toString(container.getQueryParameters().get(name));
            }
            return null;
        });
    }

    private static String stripObjectName(String name) {
        StringBuilder result = new StringBuilder();
        boolean lastUnd = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                result.append(c);
                lastUnd = false;
            } else if (!lastUnd) {
                result.append('_');
                lastUnd = true;
            }
            if (result.length() >= 64) {
                break;
            }
        }
        return result.toString();
    }

    @Override
    public DBSObject getDatabaseObject() {
        return null;
    }

    public static Object[] fetchRow(DBCSession session, DBCResultSet resultSet, DBDAttributeBinding[] attributes) throws DBCException {
        int columnCount = attributes.length; // Column count without virtual columns

        Object[] row = new Object[columnCount];
        for (int i = 0 ; i < columnCount; i++) {
            DBDAttributeBinding attribute = attributes[i];
            DBSAttributeBase metaAttr = attribute.getMetaAttribute();
            if (metaAttr == null) {
                continue;
            }
            try {
                row[i] = attribute.getValueHandler().fetchValueObject(session, resultSet, metaAttr, attribute.getOrdinalPosition());
            } catch (Exception e) {
                log.debug("Error fetching '" + metaAttr.getName() + "' value: " + e.getMessage());
            }
        }
        return row;
    }

    @NotNull
    public StreamConsumerSettings getSettings() {
        return settings;
    }

    @NotNull
    private static String getFallbackOutputFolder() {
        final DBPPreferenceStore prefs = DTActivator.getDefault().getPreferences();
        final String value = prefs.getString(DTConstants.PREF_FALLBACK_OUTPUT_DIRECTORY);

        if (CommonUtils.isEmpty(value)) {
            return DTConstants.DEFAULT_FALLBACK_OUTPUT_DIRECTORY;
        } else {
            return value;
        }
    }

    private class StreamExportSite implements IStreamDataExporterSite {
        @Override
        public DBPNamedObject getSource() {
            return dataContainer;
        }

        @Override
        public DBDDisplayFormat getExportFormat() {
            Object formatProp = processorProperties.get(PROP_FORMAT);
            if (formatProp != null) {
               return DBDDisplayFormat.valueOf(formatProp.toString().toUpperCase(Locale.ENGLISH));
            }
            return settings.getValueFormat();
        }

        @Override
        public Map<String, Object> getProperties() {
            return processorProperties;
        }

        @Override
        public DBDAttributeBinding[] getAttributes() {
            return columnBindings;
        }

        @Override
        public OutputStream getOutputStream() {
            return outputStream;
        }

        @Nullable
        @Override
        public File getOutputFile() {
            return outputFile;
        }

        @Override
        public PrintWriter getWriter() {
            return writer;
        }

        @Override
        public void flush() throws IOException {
            if (writer != null) {
                writer.flush();
            }
            if (outputStream != null) {
                outputStream.flush();
            }
        }

        @Override
        public void writeBinaryData(@NotNull DBDContentStorage cs) throws IOException {
            if (parameters.isBinary) {
                try (final InputStream stream = cs.getContentStream()) {
                    IOUtils.copyStream(stream, exportSite.getOutputStream());
                }
            } else {
                try (final InputStream stream = cs.getContentStream()) {
                    exportSite.flush();
                    final DBPDataSource dataSource = dataContainer.getDataSource();
                    switch (settings.getLobEncoding()) {
                        case BASE64: {
                            Base64.encode(stream, cs.getContentLength(), writer);
                            break;
                        }
                        case HEX: {
                            writer.write("0x"); //$NON-NLS-1$
                            byte[] buffer = new byte[5000];
                            for (; ; ) {
                                int count = stream.read(buffer);
                                if (count <= 0) {
                                    break;
                                }
                                GeneralUtils.writeBytesAsHex(writer, buffer, 0, count);
                            }
                            break;
                        }
                        case NATIVE: {
                            if (dataSource != null) {
                                ByteArrayOutputStream buffer = new ByteArrayOutputStream((int) cs.getContentLength());
                                IOUtils.copyStream(stream, buffer);

                                final byte[] bytes = buffer.toByteArray();
                                final String binaryString = dataSource.getSQLDialect().getNativeBinaryFormatter().toString(bytes, 0, bytes.length);
                                writer.write(binaryString);
                                break;
                            }
                        }
                        default: {
                            // Binary stream
                            try (Reader reader = new InputStreamReader(stream, cs.getCharset())) {
                                IOUtils.copyText(reader, writer);
                            }
                            break;
                        }
                    }
                }
            }
        }

        @NotNull
        @Override
        public String getOutputEncoding() {
            return settings == null ? StandardCharsets.UTF_8.displayName() : settings.getOutputEncoding();
        }
    }

    private class OutputStreamStatProxy extends OutputStream {
        private final OutputStream out;
        OutputStreamStatProxy(OutputStream outputStream) {
            this.out = outputStream;
        }

        @Override
        public void write(int b) throws IOException {
            this.out.write(b);
            bytesWritten++;
        }

        @Override
        public void write(@NotNull byte[] b) throws IOException {
            this.out.write(b);
            bytesWritten += b.length;
        }

        @Override
        public void write(@NotNull byte[] b, int off, int len) throws IOException {
            this.out.write(b, off, len);
            bytesWritten += len;
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            out.close();
        }

    }

    public static class ObjectSerializer implements DBPObjectSerializer<DBTTask, StreamTransferConsumer> {

        @Override
        public void serializeObject(DBRRunnableContext runnableContext, DBTTask context, StreamTransferConsumer object, Map<String, Object> state) {
        }

        @Override
        public StreamTransferConsumer deserializeObject(DBRRunnableContext runnableContext, DBTTask objectContext, Map<String, Object> state) {
            return new StreamTransferConsumer();
        }
    }

}
