package org.jkiss.dbeaver.data.transfer.exporter;

import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.StreamExporterSite;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DataExporterGeoJSON implements IStreamDataExporter {
    @Override
    public void exportData(DBRProgressMonitor monitor, StreamExporterSite site, DBCSession session, DBCResultSet resultSet, List<DBDAttributeBinding> columns, OutputStream outputStream) throws Exception {
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        writer.write("{\"type\": \"FeatureCollection\", \"features\": [\n");
        boolean first = true;
        while (resultSet.nextRow()) {
            if (!first) writer.write(",\n");
            first = false;
            writer.write("{\"type\": \"Feature\", \"properties\": {");
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) writer.write(",");
                DBDAttributeBinding col = columns.get(i);
                Object value = resultSet.getAttributeValue(col);
                writer.write("\"" + col.getName() + "\": \"" + (value != null ? value.toString() : "") + "\"");
            }
            writer.write("}}");
        }
        writer.write("\n]}");
        writer.flush();
    }

    @Override
    public String getDescription() {
        return "GeoJSON Exporter";
    }

    @Override
    public String getFileExtension() {
        return "geojson";
    }
}