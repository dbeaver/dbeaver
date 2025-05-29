
package org.jkiss.dbeaver.tools.transfer.stream.exporter;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.model.data.json.JSONUtils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DataExporterGeoJSON implements IStreamDataExporter {

    private OutputStreamWriter writer;
    private List<DBDAttributeBinding> columns;
    private boolean firstFeature = true;
    private int geometryIndex = -1;
    private IStreamDataExporterSite site;

    @Override
    public void init(IStreamDataExporterSite site) {
        this.site = site;
        this.writer = new OutputStreamWriter(site.getOutputStream(), StandardCharsets.UTF_8);
        this.firstFeature = true;
        this.geometryIndex = -1;
        this.columns = null;
    }

    @Override
    public void exportHeader(DBCSession session) throws IOException {
        DBDAttributeBinding[] attrs = site.getAttributes();
        if (attrs == null) {
            throw new IllegalStateException("No attributes provided for export");
        }
        this.columns = List.of(attrs);
        // Find geometry column index (case-insensitive)
        for (int i = 0; i < columns.size(); i++) {
            if ("geometry".equalsIgnoreCase(columns.get(i).getName())) {
                geometryIndex = i;
                break;
            }
        }
        writer.write("{\"type\": \"FeatureCollection\", \"features\": [\n");
    }

    @Override
    public void exportRow(DBCSession session, DBCResultSet resultSet, Object[] row) throws IOException {
        if (!firstFeature) writer.write(",\n");
        firstFeature = false;

        writer.write("{\"type\": \"Feature\", ");

        // Geometry
        if (geometryIndex >= 0 && row[geometryIndex] != null) {
            writer.write("\"geometry\": ");
            writer.write(row[geometryIndex].toString());
            writer.write(", ");
        } else {
            writer.write("\"geometry\": null, ");
        }

        // Properties
        writer.write("\"properties\": {");
        boolean firstProp = true;
        for (int i = 0; i < columns.size(); i++) {
            if (i == geometryIndex) continue;
            if (!firstProp) writer.write(",");
            firstProp = false;
            String name = columns.get(i).getName();
            Object value = row[i];
            writer.write("\"" + JSONUtils.escapeJsonString(name) + "\": ");
            if (value == null) {
                writer.write("null");
            } else {
                writer.write("\"" + JSONUtils.escapeJsonString(value.toString()) + "\"");
            }
        }
        writer.write("}}");
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) throws IOException {
        writer.write("\n]}");
        writer.flush();
    }

    @Override
    public void dispose() {
        // No resources to clean up
    }
}