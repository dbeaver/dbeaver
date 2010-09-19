/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.wizard;

import net.sf.jkiss.utils.Base64;
import net.sf.jkiss.utils.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IResultSetProvider;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDColumnBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.ui.export.IDataExporter;
import org.jkiss.dbeaver.ui.export.IDataExporterSite;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Data export job
 */
public class DataExportJob extends AbstractJob {

    static final Log log = LogFactory.getLog(DataExportJob.class);

    private DataExportSettings settings;

    public DataExportJob(DataExportSettings settings)
    {
        super("Export data");
        this.settings = settings;

        setUser(true);
    }

    @Override
    public boolean belongsTo(Object family)
    {
        return family == settings;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {

        for (; ;) {
            IResultSetProvider dataProvider = settings.acquireDataProvider();
            if (dataProvider == null) {
                break;
            }
            extractData(monitor, dataProvider);
        }

        return Status.OK_STATUS;
    }

    private void extractData(DBRProgressMonitor monitor, IResultSetProvider dataProvider)
    {
        setName("Export data from \"" + dataProvider.getResultSetSource().getName() + "\"");

        String contextTask = "Export data";
        DBCExecutionContext context = settings.isOpenNewConnections() ?
            dataProvider.getDataSource().openIsolatedContext(monitor, contextTask) :
            dataProvider.getDataSource().openContext(monitor, contextTask);
        try {

            ExporterSite site = new ExporterSite(dataProvider);
            site.makeExport(context);

        } catch (Exception e) {
            new DataExportErrorJob(e).schedule();
        } finally {
            context.close();
        }
    }

    private class ExporterSite implements IDataExporterSite, DBDDataReceiver {

        private static final String LOB_DIRECTORY_NAME = "files";

        private IResultSetProvider dataProvider;
        private IDataExporter dataExporter;
        private OutputStream outputStream;
        private PrintWriter writer;
        private List<DBDColumnBinding> metaColumns = new ArrayList<DBDColumnBinding>();
        private Object[] row;
        private File lobDirectory;
        private long lobCount;
        private File outputFile;

        private ExporterSite(IResultSetProvider dataProvider)
        {
            this.dataProvider = dataProvider;
        }

        public DBPNamedObject getSource()
        {
            return dataProvider.getResultSetSource();
        }

        public Map<String, Object> getProperties()
        {
            return settings.getExtractorProperties();
        }

        public List<DBDColumnBinding> getColumns()
        {
            return metaColumns;
        }

        public OutputStream getOutputStream()
        {
            return outputStream;
        }

        public PrintWriter getWriter()
        {
            return writer;
        }

        public void flush() throws IOException
        {
            writer.flush();
            outputStream.flush();
        }

        public void writeBinaryData(InputStream stream, long streamLength) throws IOException
        {
            flush();
            switch (settings.getLobEncoding()) {
                case BASE64:
                {
                    Base64.encode(stream, streamLength, writer);
                    break;
                }
                case HEX:
                {
                    writer.write("0x");
                    byte[] buffer = new byte[5000];
                    for (;;) {
                        int count = stream.read(buffer);
                        if (count <= 0) {
                            break;
                        }
                        ContentUtils.writeBytesAsHex(writer, buffer, 0, count);
                    }
                    break;
                }
                default:
                    // Binary stream
                    IOUtils.copyStream(stream, outputStream, 5000);
                    break;
            }
        }

        public void fetchStart(DBRProgressMonitor monitor, DBCResultSet resultSet) throws DBCException
        {
            // Prepare columns
            List<DBCColumnMetaData> columns = resultSet.getResultSetMetaData().getColumns();
            for (DBCColumnMetaData column : columns) {
                DBDColumnBinding columnBinding = DBUtils.getColumnBinding(dataProvider.getDataSource(), column);
/*
                if (settings.getLobExtractType() == DataExportSettings.LobExtractType.SKIP &&
                    DBDContent.class.isAssignableFrom(columnBinding.getValueHandler().getValueObjectType()))
                {

                }
*/
                metaColumns.add(columnBinding);
            }
            row = new Object[metaColumns.size()];

            try {
                dataExporter.exportHeader(monitor);
            } catch (DBException e) {
                log.warn("Error while exporting table header", e);
            } catch (IOException e) {
                throw new DBCException("IO error", e);
            }
        }

        public void fetchRow(DBRProgressMonitor monitor, DBCResultSet resultSet) throws DBCException
        {
            try {
                // Get values
                for (int i = 0; i < metaColumns.size(); i++) {
                    DBDColumnBinding column = metaColumns.get(i);
                    Object value = column.getValueHandler().getValueObject(monitor, resultSet, column.getColumn(), i);
                    if (value instanceof DBDContent) {
                        // Check for binary type export
                        if (!ContentUtils.isTextContent((DBDContent)value)) {
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
                                    value = saveContentToFile(monitor, (DBDContent)value);
                                    break;
                            }
                        }
                    }
                    row[i] = value;
                }
                // Export row
                dataExporter.exportRow(monitor, row);
            } catch (DBException e) {
                log.warn("Error while exporting table row", e);
            } catch (IOException e) {
                throw new DBCException("IO error", e);
            }
        }

        public void fetchEnd(DBRProgressMonitor monitor) throws DBCException
        {
            try {
                dataExporter.exportFooter(monitor);
            } catch (DBException e) {
                log.warn("Error while exporting table footer", e);
            } catch (IOException e) {
                throw new DBCException("IO error", e);
            }
        }

        private File saveContentToFile(DBRProgressMonitor monitor, DBDContent content)
            throws IOException, DBCException
        {
            if (lobDirectory == null) {
                lobDirectory = new File(settings.getOutputFolder(), LOB_DIRECTORY_NAME);
                if (!lobDirectory.exists()) {
                    if (!lobDirectory.mkdir()) {
                        throw new IOException("Could not create directory for LOB files: " + lobDirectory.getAbsolutePath());
                    }
                }
            }
            lobCount++;
            File lobFile = new File(lobDirectory, outputFile.getName() + "-" + lobCount + ".data");
            ContentUtils.saveContentToFile(content.getContents(monitor).getContentStream(), lobFile, monitor);
            return lobFile;
        }

        public void makeExport(DBCExecutionContext context)
            throws DBException, IOException
        {
            // Create exporter
            try {
                dataExporter = settings.getDataExporter().createExporter();
            } catch (Exception e) {
                throw new DBException("Could not create data exporter", e);
            }

            // Open output streams
            outputFile = settings.makeOutputFile(getSource());
            this.outputStream = new BufferedOutputStream(
                new FileOutputStream(outputFile),
                10000);
            try {
                ZipOutputStream zipStream = null;
                if (settings.isCompressResults()) {
                    zipStream = new ZipOutputStream(this.outputStream);
                    zipStream.putNextEntry(new ZipEntry(settings.getOutputFileName(getSource())));
                    this.outputStream = zipStream;
                }
                this.writer = new PrintWriter(new OutputStreamWriter(this.outputStream, settings.getOutputEncoding()), true);

                try {
                    // Check for BOM
                    if (settings.isOutputEncodingBOM()) {
                        byte[] bom = ContentUtils.getCharsetBOM(settings.getOutputEncoding());
                        if (bom != null) {
                            outputStream.write(bom);
                            outputStream.flush();
                        }
                    }

                    // init exporter
                    dataExporter.init(this);

                    // Perform export
                    if (settings.getExtractType() == DataExportSettings.ExtractType.SINGLE_QUERY) {
                        // Just do it in single query
                        this.dataProvider.extractData(context, this, -1, -1);
                    } else {
                        // Read all data by segments
                        int offset = 0;
                        int segmentSize = settings.getSegmentSize();
                        for (;;) {
                            int rowCount = this.dataProvider.extractData(context, this, offset, segmentSize);
                            if (rowCount < segmentSize) {
                                // Done
                                break;
                            }
                            offset += rowCount;
                        }
                    }

                } finally {
                    try {
                        this.flush();
                    } catch (IOException e) {
                        log.debug(e);
                    }
                    // Dispose exporter
                    dataExporter.dispose();
                    dataExporter = null;

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
                    }
                    if (this.writer != null) {
                        ContentUtils.close(this.writer);
                        this.writer = null;
                    }
                }
            } finally {
                ContentUtils.close(outputStream);
                outputStream = null;
            }
        }
    }
}
