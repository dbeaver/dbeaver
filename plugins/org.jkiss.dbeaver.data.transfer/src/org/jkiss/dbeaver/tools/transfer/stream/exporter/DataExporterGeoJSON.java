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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
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

    // Configuration properties
    private static final String PROP_GEO_DATA_TYPES = "geoDataTypes";
    private static final String PROP_GEOMETRY_COLUMN_NAMES = "geometryColumnNames";

    // Default values
    private static final String DEFAULT_GEO_DATA_TYPES = "geometry,geojson,geography,geom";
    private static final String DEFAULT_GEOMETRY_COLUMN_NAMES = "geometry,geom,the_geom,geography,geojson,shape,wkb_geometry";

    // Gson instance configured for GeoJSON output
    private static final Gson GEOJSON_GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .create();

    private PrintWriter writer;
    private JsonWriter jsonWriter;
    private List<DBDAttributeBinding> columns;
    private boolean firstFeature = true;
    private int geometryIndex = -1;
    private IStreamDataExporterSite site;
    private String[] geoDataTypes;
    private String[] geometryColumnNames;

    @Override
    public void init(IStreamDataExporterSite site) throws DBException {
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
        this.firstFeature = true;
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
    public void exportHeader(DBCSession session) throws IOException {
        var attrs = site.getAttributes();
        this.columns = List.of(attrs);

        // Find geometry column index by type using configurable type names
        for (int i = 0; i < columns.size(); i++) {
            var typeName = columns.get(i).getTypeName();
            if (typeName != null) {
                var typeLower = typeName.toLowerCase();
                for (var geoType : geoDataTypes) {
                    if (typeLower.contains(geoType)) {
                        geometryIndex = i;
                        break;
                    }
                }
                if (geometryIndex >= 0) break;
            }
        }

        // Fallback: detect geometry column by configurable column names if type detection failed
        if (geometryIndex < 0) {
            for (int i = 0; i < columns.size(); i++) {
                var colName = columns.get(i).getName();
                if (colName != null) {
                    var colNameLower = colName.trim().toLowerCase();
                    for (var geoColumnName : geometryColumnNames) {
                        if (colNameLower.equals(geoColumnName)) {
                            geometryIndex = i;
                            break;
                        }
                    }
                    if (geometryIndex >= 0) break;
                }
            }
        }

        // Start GeoJSON FeatureCollection using JsonWriter in stream mode
        jsonWriter.beginObject();
        jsonWriter.name("type").value("FeatureCollection");
        jsonWriter.name("features");
        jsonWriter.beginArray();

        firstFeature = true;
    }

    @Override
    public void exportRow(DBCSession session, DBCResultSet resultSet, Object[] row) throws IOException {
        // Start Feature
        jsonWriter.beginObject();
        jsonWriter.name("type").value("Feature");

        // Geometry
        jsonWriter.name("geometry");
        var geometryValue = (geometryIndex >= 0) ? row[geometryIndex] : null;

        switch (geometryValue) {
            case DBGeometry geometry -> {
                var gisData = geometry.getRawValue();
                switch (gisData) {
                    case Map<?, ?> map -> writeMap((Map<String, Object>) map);
                    case String geoString -> {
                        try {
                            var parsed = GEOJSON_GSON.fromJson(geoString, Map.class);
                            if (parsed instanceof Map<?, ?> parsedMap) {
                                writeMap((Map<String, Object>) parsedMap);
                            } else {
                                jsonWriter.nullValue();
                            }
                        } catch (Exception e) {
                            log.error("Error parsing geometry JSON string: " + geoString, e);
                            jsonWriter.nullValue();
                        }
                    }
                    case null, default -> jsonWriter.nullValue();
                }
            }
            case Map<?, ?> map -> writeMap((Map<String, Object>) map);
            case String geoString -> {
                try {
                    var parsed = GEOJSON_GSON.fromJson(geoString, Map.class);
                    if (parsed instanceof Map<?, ?> parsedMap) {
                        writeMap((Map<String, Object>) parsedMap);
                    } else {
                        jsonWriter.nullValue();
                    }
                } catch (Exception e) {
                    log.error("Error parsing geometry JSON string: " + geoString, e);
                    jsonWriter.nullValue();
                }
            }
            case null, default -> jsonWriter.nullValue();
        }

        // Properties
        jsonWriter.name("properties");
        jsonWriter.beginObject();
        for (int i = 0; i < columns.size(); i++) {
            if (i == geometryIndex) continue; // Skip geometry column in properties

            var name = columns.get(i).getName();
            var value = row[i];

            jsonWriter.name(name);
            switch (value) {
                case null -> jsonWriter.nullValue();
                case Number number -> jsonWriter.value(number);
                case Boolean bool -> jsonWriter.value(bool);
                default -> jsonWriter.value(value.toString());
            }
        }
        jsonWriter.endObject(); // end properties

        jsonWriter.endObject(); // end feature

        firstFeature = false;
    }

    private void writeMap(Map<String, Object> map) throws IOException {
        jsonWriter.beginObject();
        for (var entry : map.entrySet()) {
            jsonWriter.name(entry.getKey());
            writeValue(entry.getValue());
        }
        jsonWriter.endObject();
    }

    private void writeList(List<Object> list) throws IOException {
        jsonWriter.beginArray();
        for (var item : list) {
            writeValue(item);
        }
        jsonWriter.endArray();
    }

    private void writeValue(Object value) throws IOException {
        switch (value) {
            case null -> jsonWriter.nullValue();
            case Map<?, ?> map -> writeMap((Map<String, Object>) map);
            case List<?> list -> writeList((List<Object>) list);
            case String string -> jsonWriter.value(string);
            case Number number -> jsonWriter.value(number);
            case Boolean bool -> jsonWriter.value(bool);
            default -> jsonWriter.value(value.toString());
        }
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) throws IOException {
        jsonWriter.endArray(); // end features array
        jsonWriter.endObject(); // end FeatureCollection
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
