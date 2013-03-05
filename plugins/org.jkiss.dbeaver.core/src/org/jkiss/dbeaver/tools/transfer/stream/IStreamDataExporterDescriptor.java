package org.jkiss.dbeaver.tools.transfer.stream;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;

import java.util.List;

/**
 * Stream exporter descriptor
 */
public interface IStreamDataExporterDescriptor extends IDataTransferProcessor {

    DBDDisplayFormat getExportFormat();

    String getFileExtension();

    List<IPropertyDescriptor> getProperties();

    IStreamDataExporter createExporter() throws DBException;
}
