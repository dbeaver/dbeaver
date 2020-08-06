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
package org.jkiss.dbeaver.tools.transfer.stream;

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.meta.DBSerializable;
import org.jkiss.dbeaver.model.runtime.DBRProcessDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.serialize.DBPObjectSerializer;
import org.jkiss.dbeaver.tools.transfer.DTUtils;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.Base64;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Stream transfer consumer
 */
@DBSerializable("streamTransferConsumer")
public class StreamTransferConsumer implements IDataTransferConsumer<StreamConsumerSettings, IStreamDataExporter> {

    private static final Log log = Log.getLog(StreamTransferConsumer.class);

    private static final String LOB_DIRECTORY_NAME = "files"; //$NON-NLS-1$

    public static final String VARIABLE_DATASOURCE = "datasource";
    public static final String VARIABLE_CATALOG = "catalog";
    public static final String VARIABLE_SCHEMA = "schema";
    public static final String VARIABLE_TABLE = "table";
    public static final String VARIABLE_TIMESTAMP = "timestamp";
    public static final String VARIABLE_INDEX = "index";
    public static final String VARIABLE_DATE = "date";
    public static final String VARIABLE_PROJECT = "project";
    public static final String VARIABLE_FILE = "file";

    public static final int OUT_FILE_BUFFER_SIZE = 100000;

    private IStreamDataExporter processor;
    private StreamConsumerSettings settings;
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
    private TransferParameters parameters;

    public StreamTransferConsumer() {
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

        if (!initialized) {
            /*// For multi-streams export header only once
            if (!settings.isUseSingleFile() || parameters.orderNumber == 0) */{
                try {
                    processor.exportHeader(session);
                } catch (DBException e) {
                    log.warn("Error while exporting table header", e);
                } catch (IOException e) {
                    throw new DBCException("IO error", e);
                }
            }
        }

        initialized = true;
    }

    @Override
    public void fetchRow(DBCSession session, DBCResultSet resultSet) throws DBCException {
        try {
            // Get values
            Object[] srcRow = fetchRow(session, resultSet, columnMetas);
            Object[] targetRow;
            if (processor instanceof IDocumentDataExporter) {
                targetRow = srcRow;
            } else {
                targetRow = new Object[columnBindings.length];
                for (int i = 0; i < columnBindings.length; i++) {
                    DBDAttributeBinding column = columnBindings[i];
                    Object value = DBUtils.getAttributeValue(column, columnMetas, srcRow);
                    if (value instanceof DBDContent && !settings.isOutputClipboard()) {
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
                                    // Save content to file and pass file reference to exporter
                                    value = saveContentToFile(session.getProgressMonitor(), (DBDContent) value);
                                    break;
                            }
                        }
                    }
                    targetRow[i] = value;
                }
            }
            // Export row
            processor.exportRow(session, resultSet, targetRow);

            // Check for file split
            if (settings.isSplitOutFiles() && !parameters.isBinary) {
                writer.flush();
                if (bytesWritten >= settings.getMaxOutFileSize()) {
                    // Make new file
                    createNewOutFile();
                }
            }
        } catch (IOException e) {
            throw new DBCException("IO error", e);
        } catch (Throwable e) {
            throw new DBCException("Error while exporting table row", e);
        }
    }

    @Override
    public void fetchEnd(DBCSession session, DBCResultSet resultSet) throws DBCException {
    }

    @Override
    public void close() {
        columnBindings = null;
    }

    private File saveContentToFile(DBRProgressMonitor monitor, DBDContent content)
        throws IOException, DBCException {
        DBDContentStorage contents = content.getContents(monitor);
        if (contents == null) {
            log.warn("Null value content");
            return null;
        }
        if (lobDirectory == null) {
            lobDirectory = new File(settings.getOutputFolder(), LOB_DIRECTORY_NAME);
            if (!lobDirectory.exists()) {
                if (!lobDirectory.mkdir()) {
                    throw new IOException("Can't create directory for CONTENT files: " + lobDirectory.getAbsolutePath());
                }
            }
        }
        lobCount++;
        Boolean extractImages = (Boolean) processorProperties.get(StreamConsumerSettings.PROP_EXTRACT_IMAGES);
        String fileExt = (extractImages != null && extractImages) ? ".jpg" : ".data";
        File lobFile = new File(lobDirectory, outputFile.getName() + "-" + lobCount + fileExt); //$NON-NLS-1$ //$NON-NLS-2$
        try (InputStream cs = contents.getContentStream()) {
            ContentUtils.saveContentToFile(cs, lobFile, monitor);
        }
        return lobFile;
    }

    private void initExporter(DBCSession session) throws DBCException {
        if (settings.getFormatterProfile() != null) {
            session.setDataFormatterProfile(settings.getFormatterProfile());
        }

        exportSite = new StreamExportSite();

        // Open output streams
        boolean outputClipboard = settings.isOutputClipboard();
        outputFile = !parameters.isBinary && outputClipboard ? null : makeOutputFile();
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

    private void openOutputStreams() throws IOException {
        this.outputStream = new BufferedOutputStream(
            new FileOutputStream(outputFile, settings.isUseSingleFile()),
            OUT_FILE_BUFFER_SIZE);
        if (settings.isCompressResults()) {
            this.zipStream = new ZipOutputStream(this.outputStream);
            this.zipStream.putNextEntry(new ZipEntry(getOutputFileName()));
            this.outputStream = zipStream;
        }

        // If we need to split files - use stream wrapper to calculate fiel size
        if (settings.isSplitOutFiles()) {
            this.outputStream = new OutputStreamStatProxy(this.outputStream);
        }

        // Check for BOM and write it to the stream
        if (!parameters.isBinary && settings.isOutputEncodingBOM()) {
            byte[] bom = GeneralUtils.getCharsetBOM(settings.getOutputEncoding());
            if (bom != null) {
                outputStream.write(bom);
                outputStream.flush();
            }
        }

        if (!parameters.isBinary) {
            this.writer = new PrintWriter(new OutputStreamWriter(this.outputStream, settings.getOutputEncoding()), true);
        }
    }

    private void closeOutputStreams() {
        if (this.writer != null) {
            this.writer.flush();
            ContentUtils.close(this.writer);
            this.writer = null;
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

        openOutputStreams();
    }

    @Override
    public void initTransfer(DBSObject sourceObject, StreamConsumerSettings settings, TransferParameters parameters, IStreamDataExporter processor, Map<String, Object> processorProperties) {
        this.dataContainer = (DBSDataContainer) sourceObject;
        this.parameters = parameters;
        this.processor = processor;
        this.settings = settings;
        this.processorProperties = processorProperties;
    }

    @Override
    public void startTransfer(DBRProgressMonitor monitor) {
        // do nothing
    }

    @Override
    public void finishTransfer(DBRProgressMonitor monitor, boolean last) {
        if (!last) {
            if (processor != null) {
                try {
                    processor.exportFooter(monitor);
                } catch (Exception e) {
                    log.warn("Error while exporting table footer", e);
                }
            }

            closeExporter();

            if (!settings.isOutputClipboard() && settings.isExecuteProcessOnFinish()) {
                executeFinishCommand();
            }

            return;
        }

        if (!parameters.isBinary && settings.isOutputClipboard()) {
            if (outputBuffer != null) {
                String strContents = outputBuffer.toString();
                DBWorkbench.getPlatformUI().copyTextToClipboard(strContents, parameters.isHTML);
                outputBuffer = null;
            }
        } else {
            if (settings.isOpenFolderOnFinish()) {
                // Last one
                DBWorkbench.getPlatformUI().executeShellProgram(settings.getOutputFolder());
            }
        }
    }

    @Nullable
    @Override
    public Object getTargetObjectContainer() {
        return null;
    }

    private void executeFinishCommand() {
        String commandLine = translatePattern(
            settings.getFinishProcessCommand(),
            outputFile);
        DBRShellCommand command = new DBRShellCommand(commandLine);
        DBRProcessDescriptor processDescriptor = new DBRProcessDescriptor(command);
        try {
            processDescriptor.execute();
        } catch (DBException e) {

            DBWorkbench.getPlatformUI().showError(DTMessages.stream_transfer_consumer_title_run_process,
                    NLS.bind(DTMessages.stream_transfer_consumer_message_error_running_process, commandLine), e);
        }
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

    public String getOutputFileName() {
        Object extension = processorProperties == null ? null : processorProperties.get(StreamConsumerSettings.PROP_FILE_EXTENSION);
        String fileName = translatePattern(
            settings.getOutputFilePattern(),
            null).trim();
        // Can't rememeber why did we need this. It breaks file names in case of multiple tables export (#6911)
//        if (parameters.orderNumber > 0 && !settings.isUseSingleFile()) {
//            fileName += "_" + String.valueOf(parameters.orderNumber + 1);
//        }
        if (multiFileNumber > 0) {
            fileName += "_" + (multiFileNumber + 1);
        }
        if (extension != null) {
            return fileName + "." + extension;
        } else {
            return fileName;
        }
    }

    public File makeOutputFile() {
        File dir = new File(settings.getOutputFolder());
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("Can't create output directory '" + dir.getAbsolutePath() + "'");
        }
        String fileName = getOutputFileName();
        if (settings.isCompressResults()) {
            fileName += ".zip";
        }
        return new File(dir, fileName);
    }

    private String translatePattern(String pattern, final File targetFile) {
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
                        return "export";
                    }
                    if (dataContainer == null) {
                        return null;
                    }
                    String tableName = DTUtils.getTableName(dataContainer.getDataSource(), dataContainer, true);
                    return stripObjectName(tableName);
                }
                case VARIABLE_TIMESTAMP:
                    Date ts;
                    if (parameters.startTimestamp != null) {
                        // Use saved timestamp (#7352)
                        ts = parameters.startTimestamp;
                    } else {
                        ts = new Date();
                    }
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(settings.getOutputTimestampPattern());
                        return sdf.format(ts);
                    } catch (Exception e) {
                        log.error(e);
                        return "BAD_TIMESTAMP";
                    }
                case VARIABLE_DATE:
                    return RuntimeUtils.getCurrentDate();
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
        int columnCount = resultSet.getMeta().getAttributes().size(); // Column count without virtual columns

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

    private class StreamExportSite implements IStreamDataExporterSite {
        @Override
        public DBPNamedObject getSource() {
            return dataContainer;
        }

        @Override
        public DBDDisplayFormat getExportFormat() {
            DBDDisplayFormat format = DBDDisplayFormat.UI;
            Object formatProp = processorProperties.get(StreamConsumerSettings.PROP_FORMAT);
            if (formatProp != null) {
                format = DBDDisplayFormat.valueOf(formatProp.toString().toUpperCase(Locale.ENGLISH));
            }
            return format;
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
