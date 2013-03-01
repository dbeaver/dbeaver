package org.jkiss.dbeaver.tools.transfer.stream;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;

import java.util.List;

/**
 * Stream exporter descriptor
 */
public interface IStreamDataExporterDescriptor {

    String getId();

    String getName();

    DBDDisplayFormat getExportFormat();

    String getFileExtension();

    List<IPropertyDescriptor> getProperties();

    IStreamDataExporter createExporter() throws DBException;
}
