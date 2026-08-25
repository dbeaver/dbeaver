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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.utils.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class DataSourceSerializerModernTest {
    @Test
    public void testFoldersWithSameNameAreReadWithoutChangingFormat() throws IOException {
        List<Pair<String, Map<String, Object>>> folders = readFolders("""
            {
              "connections": {},
              "folders": {
                "Parent": {},
                "New folder": { "parent": "Parent" },
                "New folder": {}
              }
            }
            """);

        Assertions.assertEquals(3, folders.size());
        Assertions.assertEquals("Parent", folders.get(0).getFirst());
        Assertions.assertEquals("New folder", folders.get(1).getFirst());
        Assertions.assertEquals("Parent", folders.get(1).getSecond().get(RegistryConstants.ATTR_PARENT));
        Assertions.assertEquals("New folder", folders.get(2).getFirst());
        Assertions.assertTrue(folders.get(2).getSecond().isEmpty());
    }

    @Test
    public void testFoldersWithSameNameUnderDifferentParentsArePreserved() throws IOException {
        List<Pair<String, Map<String, Object>>> folders = readFolders("""
            {
              "folders": {
                "Parent 1": {},
                "Parent 2": {},
                "Child": { "parent": "Parent 1" },
                "Child": { "parent": "Parent 2" }
              }
            }
            """);

        Assertions.assertEquals(4, folders.size());
        Assertions.assertEquals("Child", folders.get(2).getFirst());
        Assertions.assertEquals("Parent 1", folders.get(2).getSecond().get(RegistryConstants.ATTR_PARENT));
        Assertions.assertEquals("Child", folders.get(3).getFirst());
        Assertions.assertEquals("Parent 2", folders.get(3).getSecond().get(RegistryConstants.ATTR_PARENT));
    }

    @Test
    public void testUniqueFolderConfigurationIsReadNormally() throws IOException {
        List<Pair<String, Map<String, Object>>> folders = readFolders("""
            {
              "folders": {
                "Parent": { "description": "Parent folder" },
                "Child": { "parent": "Parent", "description": "Child folder" }
              }
            }
            """);

        Assertions.assertEquals(2, folders.size());
        Assertions.assertEquals("Parent folder", folders.get(0).getSecond().get(RegistryConstants.ATTR_DESCRIPTION));
        Assertions.assertEquals("Parent", folders.get(1).getSecond().get(RegistryConstants.ATTR_PARENT));
        Assertions.assertEquals("Child folder", folders.get(1).getSecond().get(RegistryConstants.ATTR_DESCRIPTION));
    }

    @Test
    public void testMissingOrNullFoldersAreReadAsEmpty() throws IOException {
        Assertions.assertTrue(readFolders("{ \"connections\": {} }").isEmpty());
        Assertions.assertTrue(readFolders("{ \"folders\": null }").isEmpty());
    }

    private static List<Pair<String, Map<String, Object>>> readFolders(@NotNull String data) throws IOException {
        return TestSerializer.readFolders(data);
    }

    private static final class TestSerializer extends DataSourceSerializerModern<DataSourceDescriptor> {
        private TestSerializer(@NotNull DataSourceRegistry<DataSourceDescriptor> registry) {
            super(registry);
        }

        @NotNull
        @SuppressWarnings("unchecked")
        private static List<Pair<String, Map<String, Object>>> readFolders(@NotNull String data) throws IOException {
            Object folders = readConfigurationMap(data).get("folders");
            return folders == null ? List.of() : (List<Pair<String, Map<String, Object>>>) folders;
        }
    }
}
