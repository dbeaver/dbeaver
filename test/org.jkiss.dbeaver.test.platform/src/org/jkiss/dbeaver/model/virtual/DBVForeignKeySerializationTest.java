/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.virtual;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

public class DBVForeignKeySerializationTest extends DBeaverUnitTest {

    @Test
    void repeatedSourceColumnsSurviveSerialization() throws Exception {
        List<List<String>> mappings = List.of(
            List.of("c", "c1"),
            List.of("c", "c2"),
            List.of("c", "c3")
        );
        DBVModel model = createModel(List.of(
            Map.of("attribute", "c", "ref-attribute", "c1"),
            Map.of("attribute", "c", "ref-attribute", "c2"),
            Map.of("attribute", "c", "ref-attribute", "c3")
        ));
        try {
            Assertions.assertEquals(mappings, getMappings(model));

            Map<String, Object> saved = serialize(model);
            Map<String, Object> savedForeignKey = getSavedForeignKey(saved);
            Assertions.assertEquals(Map.of("c", "c1"), JSONUtils.getObject(savedForeignKey, "attributes"));
            Assertions.assertEquals(3, JSONUtils.getObjectList(savedForeignKey, "attribute-mappings").size());

            DBVModel restored = new DBVModel("test", JSONUtils.getObject(saved, "test"));
            try {
                Assertions.assertEquals(mappings, getMappings(restored));
            } finally {
                restored.dispose();
            }
        } finally {
            model.dispose();
        }
    }

    @Test
    void loadsExistingObjectBasedMappings() throws Exception {
        DBVModel model = createModel(Map.of("source", "target", "other", "another"));
        try {
            Assertions.assertEquals(2, getMappings(model).size());
            Assertions.assertTrue(getMappings(model).contains(List.of("source", "target")));
            Assertions.assertTrue(getMappings(model).contains(List.of("other", "another")));

            Map<String, Object> savedForeignKey = getSavedForeignKey(serialize(model));
            Assertions.assertEquals(
                Map.of("source", "target", "other", "another"),
                JSONUtils.getObject(savedForeignKey, "attributes"));
            Assertions.assertFalse(savedForeignKey.containsKey("attribute-mappings"));
        } finally {
            model.dispose();
        }
    }

    private static Map<String, Object> serialize(DBVModel model) throws Exception {
        StringWriter output = new StringWriter();
        try (JsonWriter json = new JsonWriter(output)) {
            json.beginObject();
            model.serialize(new VoidProgressMonitor(), json);
            json.endObject();
        }
        return JSONUtils.parseMap(new Gson(), new StringReader(output.toString()));
    }

    private static Map<String, Object> getSavedForeignKey(Map<String, Object> saved) {
        Map<String, Object> schema = JSONUtils.getObject(JSONUtils.getObject(saved, "test"), "schema");
        return JSONUtils.getObjectList(JSONUtils.getObject(schema, ":source"), "foreign-keys").getFirst();
    }

    private static DBVModel createModel(Object attributes) {
        return new DBVModel("test", Map.of(
            "schema", Map.of(
                ":source", Map.of(
                    "foreign-keys", List.of(Map.of(
                        "entity", "dbv-fk-serialization-test-target",
                        "constraint", "PRIMARY",
                        "attributes", attributes
                    ))
                )
            )
        ));
    }

    private static List<List<String>> getMappings(DBVModel model) {
        return model.getContainer("schema", false).getEntity("source", false).getForeignKeys().getFirst()
            .getAttributes().stream()
            .map(column -> List.of(column.getAttributeName(), column.getRefAttributeName()))
            .toList();
    }
}
