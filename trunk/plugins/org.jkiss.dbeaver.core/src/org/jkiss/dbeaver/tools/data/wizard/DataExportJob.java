/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 * eugene.fradkin@gmail.com
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
package org.jkiss.dbeaver.tools.data.wizard;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.tools.data.IDataExporter;
import org.jkiss.dbeaver.tools.data.IDataExporterSite;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.Base64;
import org.jkiss.utils.IOUtils;

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
        super(CoreMessages.dialog_export_wizard_job_name);
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
            DataExportProvider dataProvider = settings.acquireDataProvider();
            if (dataProvider == null) {
                break;
            }
            extractData(monitor, dataProvider);
        }

        return Status.OK_STATUS;
    }

    private void extractData(DBRProgressMonitor monitor, DataExportProvider dataProvider)
    {
        final DBSDataContainer dataContainer = dataProvider.getDataContainer();
        setName(NLS.bind(CoreMessages.dialog_export_wizard_job_container_name, dataContainer.getName()));

        String contextTask = CoreMessages.dialog_export_wizard_job_task_export;
        DBCExecutionContext context = settings.isOpenNewConnections() ?
            dataContainer.getDataSource().openIsolatedContext(monitor, DBCExecutionPurpose.UTIL, contextTask) :
            dataContainer.getDataSource().openContext(monitor, DBCExecutionPurpose.UTIL, contextTask);
        try {
            if (settings.getFormatterProfile() != null) {
                context.setDataFormatterProfile(settings.getFormatterProfile());
            }
            ExporterSite site = new ExporterSite(dataProvider);
            site.makeExport(context);

        } catch (Exception e) {
            new DataExportErrorJob(e).schedule();
        } finally {
            context.close();
        }
    }

    private class ExporterSite implements IDataExporterSite, DBDDataReceiver {

        private static final String LOB_DIRECTORY_NAME = "files"; //$NON-NLS-1$

        private DataExportProvider dataProvider;
        private IDataExporter dataExporter;
        private OutputStream outputStream;
        private PrintWriter writer;
        private List<DBDAttributeBinding> metaColumns;
        private Object[] row;
        private File lobDirectory;
        private long lobCount;
        private File outputFile;

        private ExporterSite(DataExportProvider dataProvider)
        {
            this.dataProvider = dataProvider;
        }

        @Override
        public DBPNamedObject getSource()
        {
            return dataProvider.getDataContainer();
        }

        @Override
        public DBDDisplayFormat getExportFormat()
        {
            return settings.getDataExporter().getExportFormat();
        }

        @Override
        public Map<Object, Object> getProperties()
        {
            return settings.getExtractorProperties();
        }

        @Override
        public List<DBDAttributeBinding> getAttributes()
        {
            return metaColumns;
        }

        @Override
        public OutputStream getOutputStream()
        {
            return outputStream;
        }

        @Override
        public PrintWriter getWriter()
        {
            return writer;
        }

        @Override
        public void flush() throws IOException
        {
            writer.flush();
            outputStream.flush();
        }

        @Override
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
                    writer.write("0x"); //$NON-NLS-1$
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

        @Override
        public void fetchStart(DBCExecutionContext context, DBCResultSet resultSet) throws DBCException
        {
            // Prepare columns
            metaColumns = new ArrayList<DBDAttributeBinding>();
            List<DBCAttributeMetaData> attributes = resultSet.getResultSetMetaData().getAttributes();
            for (DBCAttributeMetaData attribute : attributes) {
                DBDAttributeBinding columnBinding = DBUtils.getColumnBinding(context, attribute);
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
                dataExporter.exportHeader(context.getProgressMonitor());
            } catch (DBException e) {
                log.warn("Error while exporting table header", e);
            } catch (IOException e) {
                throw new DBCException("IO error", e);
            }
        }

        @Override
        public void fetchRow(DBCExecutionContext context, DBCResultSet resultSet) throws DBCException
        {
            try {
                // Get values
                for (int i = 0; i < metaColumns.size(); i++) {
                    DBDAttributeBinding column = metaColumns.get(i);
                    Object value = column.getValueHandler().fetchValueObject(context, resultSet, column.getAttribute(), i);
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
                                    value = saveContentToFile(context.getProgressMonitor(), (DBDContent)value);
                                    break;
                            }
                        }
                    }
                    row[i] = value;
                }
                // Export row
                dataExporter.exportRow(context.getProgressMonitor(), row);
            } catch (DBException e) {
                throw new DBCException("Error while exporting table row", e);
            } catch (IOException e) {
                throw new DBCException("IO error", e);
            }
        }

        @Override
        public void fetchEnd(DBCExecutionContext context) throws DBCException
        {
            try {
                dataExporter.exportFooter(context.getProgressMonitor());
            } catch (DBException e) {
                log.warn("Error while exporting table footer", e);
            } catch (IOException e) {
                throw new DBCException("IO error", e);
            }
        }

        @Override
        public void close()
        {
            metaColumns = null;
            row = null;
        }

        private File saveContentToFile(DBRProgressMonitor monitor, DBDContent content)
            throws IOException, DBCException
        {
            DBDContentStorage contents = content.getContents(monitor);
            if (contents == null) {
                log.warn("Null value content");
                return null;
            }
            if (lobDirectory == null) {
                lobDirectory = new File(settings.getOutputFolder(), LOB_DIRECTORY_NAME);
                if (!lobDirectory.exists()) {
                    if (!lobDirectory.mkdir()) {
                        throw new IOException("Could not create directory for LOB files: " + lobDirectory.getAbsolutePath());
                    }
                }
            }
            lobCount++;
            Boolean extractImages = (Boolean) settings.getExtractorProperties().get("extractImages");
            String fileExt = (extractImages != null && extractImages) ? ".jpg" : ".data";
            File lobFile = new File(lobDirectory, outputFile.getName() + "-" + lobCount + fileExt); //$NON-NLS-1$ //$NON-NLS-2$
            ContentUtils.saveContentToFile(contents.getContentStream(), lobFile, monitor);
            return lobFile;
        }

        public void makeExport(DBCExecutionContext context)
            throws DBException, IOException
        {
            DBRProgressMonitor monitor = context.getProgressMonitor();

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

                    long totalRows = 0;
                    if (settings.isQueryRowCount() && (dataProvider.getDataContainer().getSupportedFeatures() & DBSDataContainer.DATA_COUNT) != 0) {
                        monitor.beginTask(CoreMessages.dialog_export_wizard_job_task_retrieve, 1);
                        totalRows = dataProvider.getDataContainer().countData(context, dataProvider.getDataFilter());
                        monitor.done();
                    }

                    // init exporter
                    dataExporter.init(this);

                    monitor.beginTask(CoreMessages.dialog_export_wizard_job_task_export_table_data, (int)totalRows);

                    // Perform export
                    if (settings.getExtractType() == DataExportSettings.ExtractType.SINGLE_QUERY) {
                        // Just do it in single query
                        this.dataProvider.getDataContainer().readData(context, this, dataProvider.getDataFilter(), -1, -1);
                    } else {
                        // Read all data by segments
                        long offset = 0;
                        int segmentSize = settings.getSegmentSize();
                        for (;;) {
                            long rowCount = this.dataProvider.getDataContainer().readData(
                                context, this, dataProvider.getDataFilter(), offset, segmentSize);
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
                    monitor.done();
                }
            } finally {
                ContentUtils.close(outputStream);
                outputStream = null;
            }
        }
    }
}
