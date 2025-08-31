/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DataExporterGeoJSON implements IStreamDataExporter {

    private OutputStreamWriter writer;
    private PrintWriter out;
    private List<DBDAttributeBinding> columns;
    private boolean firstFeature = true;
    private int geometryIndex = -1;
    private IStreamDataExporterSite site;

    @Override
    public void init(IStreamDataExporterSite site) {
        this.site = site;
        this.writer = new OutputStreamWriter(site.getOutputStream(), StandardCharsets.UTF_8);
        this.out = new PrintWriter(this.writer);
        this.firstFeature = true;
        this.geometryIndex = -1;
        this.columns = null;
    }

    @Override
    public void exportHeader(DBCSession session) throws IOException {
        DBDAttributeBinding[] attrs = site.getAttributes();
        this.columns = List.of(attrs);
        // Find geometry column index by type (case-insensitive, contains "geometry", "geojson", "geography", or "geom")
        for (int i = 0; i < columns.size(); i++) {
            String typeName = columns.get(i).getTypeName();
            if (typeName != null) {
                String typeLower = typeName.toLowerCase();
                if (typeLower.contains("geometry") || typeLower.contains("geojson") || typeLower.contains("geography") || typeLower.contains("geom")) {
                    geometryIndex = i;
                    break;
                }
            }
        }
        // Fallback: detect geometry column by common column names if type detection failed
        if (geometryIndex < 0) {
            for (int i = 0; i < columns.size(); i++) {
                String colName = columns.get(i).getName();
                if (colName != null) {
                    String n = colName.trim().toLowerCase();
                    if (n.equals("geometry") || n.equals("geom") || n.equals("the_geom") || n.equals("geography") || n.equals("geojson") || n.equals("shape") || n.equals("wkb_geometry")) {
                        geometryIndex = i;
                        break;
                    }
                }
            }
        }
        out.write("{\"type\": \"FeatureCollection\", \"features\": [\n");
        out.flush();
        firstFeature = true;
    }

    @Override
    public void exportRow(DBCSession session, DBCResultSet resultSet, Object[] row) throws IOException {
        if (!firstFeature) {
            out.write(",\n");
        }
        firstFeature = false;
        out.write("    {\n");
        out.write("      \"type\": \"Feature\",\n");
        // Geometry
        out.write("      \"geometry\": ");
        if (geometryIndex >= 0 && row[geometryIndex] != null) {
            String geomStr = row[geometryIndex].toString();
            if (geomStr.trim().startsWith("{") || geomStr.trim().startsWith("[")) {
                out.write(geomStr);
            } else {
                out.write("null");
            }
        } else {
            out.write("null");
        }
        out.write(",\n");
        // Properties
        out.write("      \"properties\": {\n");
        boolean firstProp = true;
        for (int i = 0; i < columns.size(); i++) {
            if (i == geometryIndex) continue;
            if (!firstProp) out.write(",\n");
            firstProp = false;
            String name = columns.get(i).getName();
            Object value = row[i];
            out.write("        \"" + JSONUtils.escapeJsonString(name) + "\": ");
            if (value == null) {
                out.write("null");
            } else {
                out.write("\"" + JSONUtils.escapeJsonString(value.toString()) + "\"");
            }
        }
        out.write("\n      }\n");
        out.write("    }");
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) throws IOException {
        out.write("\n  ]\n}");
        out.flush();
    }

    @Override
    public void dispose() {
        // No resources to clean up
    }
}

