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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.gis.DBGeometry;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DataExporterGeoJSON implements IStreamDataExporter {

    private static final Log log = Log.getLog(DataExporterGeoJSON.class);

    private static final String PROP_GEO_DATA_TYPES = "geoDataTypes";
    private static final String PROP_GEOMETRY_COLUMN_NAMES = "geometryColumnNames";

    private static final String DEFAULT_GEO_DATA_TYPES = "geometry,geojson,geography,geom";
    private static final String DEFAULT_GEOMETRY_COLUMN_NAMES = "geometry,geom,the_geom,geography,geojson,shape,wkb_geometry";

    private static final Gson GEOJSON_GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .create();

    @Nullable
    private PrintWriter writer;
    @Nullable
    private JsonWriter jsonWriter;
    @Nullable
    private List<DBDAttributeBinding> columns;
    private int geometryIndex = -1;
    @Nullable
    private IStreamDataExporterSite site;
    private String[] geoDataTypes;
    private String[] geometryColumnNames;

    @Override
    public void init(@NotNull IStreamDataExporterSite site) throws DBException {
        this.site = site;
        this.writer = site.getWriter();
        try {
            this.jsonWriter = GEOJSON_GSON.newJsonWriter(this.writer);
        } catch (IOException e) {
            log.error("Failed to initialize JSON writer", e);
            throw new DBException("Failed to initialize JSON writer", e);
        }
        // Configure JsonWriter for the exact format expected by tests
        this.jsonWriter.setIndent(""); // No indentation for compact output
        this.jsonWriter.setSerializeNulls(false);
        this.geometryIndex = -1;
        this.columns = null;

        // Initialize configuration from properties using modern Java features
        var geoTypesProperty = CommonUtils.toString(site.getProperties().get(PROP_GEO_DATA_TYPES), DEFAULT_GEO_DATA_TYPES);
        this.geoDataTypes = Arrays.stream(geoTypesProperty.toLowerCase().split(","))
            .map(String::trim)
            .toArray(String[]::new);

        var geoColumnsProperty = CommonUtils.toString(site.getProperties().get(PROP_GEOMETRY_COLUMN_NAMES), DEFAULT_GEOMETRY_COLUMN_NAMES);
        this.geometryColumnNames = Arrays.stream(geoColumnsProperty.toLowerCase().split(","))
            .map(String::trim)
            .toArray(String[]::new);
    }

    @Override
    public void exportHeader(@NotNull DBCSession session) throws IOException {
        if (site == null) {
            throw new IllegalStateException("Exporter not initialized");
        }
        this.columns = List.of(site.getAttributes());

        // Detect geometry column by type
        for (int i = 0; i < columns.size(); i++) {
            String typeName = columns.get(i).getTypeName();
            if (typeName != null) {
                String typeLower = typeName.toLowerCase();
                for (String geoType : geoDataTypes) {
                    if (typeLower.contains(geoType)) {
                        geometryIndex = i;
                        break;
                    }
                }
            }
            if (geometryIndex >= 0) break;
        }
        // Fallback detection by column name list
        if (geometryIndex < 0) {
            for (int i = 0; i < columns.size(); i++) {
                String colName = columns.get(i).getName();
                if (colName != null) {
                    String colNameLower = colName.trim().toLowerCase();
                    for (String geoColumnName : geometryColumnNames) {
                        if (colNameLower.equals(geoColumnName)) {
                            geometryIndex = i;
                            break;
                        }
                    }
                }
                if (geometryIndex >= 0) break;
            }
        }

        // Start FeatureCollection
        jsonWriter.beginObject();
        jsonWriter.name("type").value("FeatureCollection");
        jsonWriter.name("features");
        jsonWriter.beginArray();
    }

    @Override
    public void exportRow(@NotNull DBCSession session, @NotNull DBCResultSet resultSet, @NotNull Object[] row) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name("type").value("Feature");
        jsonWriter.name("geometry");
        writeGeometry(geometryIndex >= 0 ? row[geometryIndex] : null);
        jsonWriter.name("properties");
        jsonWriter.beginObject();
        for (int i = 0; i < columns.size(); i++) {
            if (i == geometryIndex) {
                continue; // skip geometry column
            }
            String name = columns.get(i).getName();
            jsonWriter.name(name);
            writeValue(row[i]);
        }
        jsonWriter.endObject(); // properties
        jsonWriter.endObject(); // feature
    }

    private void writeGeometry(@Nullable Object geometryValue) throws IOException {
        if (geometryValue == null) {
            jsonWriter.nullValue();
            return;
        }
        if (geometryValue instanceof DBGeometry dbGeometry) {
            writeGeometry(dbGeometry.getRawValue());
            return;
        }
        if (geometryValue instanceof String s) {
            parseAndWriteGeoJSONString(s);
            return;
        }
        if (geometryValue instanceof Map<?, ?> map) {
            // raw map structure
            writeMap((Map<String, Object>) map);
            return;
        }
        // Allow arrays/lists as full geometry (e.g. raw coordinates)
        if (geometryValue instanceof List<?> list) {
            writeList((List<Object>) list);
            return;
        }
        // Fallback to plain string
        jsonWriter.value(geometryValue.toString());
    }

    private void parseAndWriteGeoJSONString(@NotNull String geoString) throws IOException {
        try {
            Object parsed = GEOJSON_GSON.fromJson(geoString, Object.class);
            writeValue(parsed); // handles null + composite structures
        } catch (Exception e) {
            log.error("Error parsing geometry JSON string: " + geoString, e);
            jsonWriter.nullValue();
        }
    }

    private void writeMap(@NotNull Map<String, Object> map) throws IOException {
        jsonWriter.beginObject();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            jsonWriter.name(entry.getKey());
            writeValue(entry.getValue());
        }
        jsonWriter.endObject();
    }

    private void writeList(@NotNull List<Object> list) throws IOException {
        jsonWriter.beginArray();
        for (Object item : list) {
            writeValue(item);
        }
        jsonWriter.endArray();
    }

    private void writeValue(@Nullable Object value) throws IOException {
        if (value == null) {
            jsonWriter.nullValue();
        } else if (value instanceof Map<?, ?> map) {
            writeMap((Map<String, Object>) map);
        } else if (value instanceof List<?> list) {
            writeList((List<Object>) list);
        } else if (value instanceof String s) {
            jsonWriter.value(s);
        } else if (value instanceof Number n) {
            jsonWriter.value(n);
        } else if (value instanceof Boolean b) {
            jsonWriter.value(b);
        } else if (value instanceof DBGeometry g) {
            writeGeometry(g);
        } else {
            jsonWriter.value(value.toString());
        }
    }

    @Override
    public void exportFooter(@NotNull DBRProgressMonitor monitor) throws IOException {
        jsonWriter.endArray(); // features
        jsonWriter.endObject(); // FeatureCollection
        jsonWriter.flush();
        if (writer != null) {
            writer.flush();
        }
    }

    @Override
    public void dispose() {
        try {
            if (jsonWriter != null) {
                jsonWriter.close();
            }
        } catch (IOException e) {
            log.error("Error closing JSON writer", e);
        }
        if (writer != null) {
            writer.close();
        }
    }
}
