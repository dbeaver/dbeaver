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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class DataSourceSerializerModernTest {
    @Test
    public void testFullFolderPath() {
        Assertions.assertEquals(
            "Parent/Child",
            TestSerializer.resolveFolderPath("Parent/Child", Map.of())
        );
    }

    @Test
    public void testLegacyFolderPath() {
        Assertions.assertEquals(
            "Parent/Child",
            TestSerializer.resolveFolderPath("Child", Map.of(RegistryConstants.ATTR_PARENT, "Parent"))
        );
    }

    private static final class TestSerializer extends DataSourceSerializerModern<DataSourceDescriptor> {
        private TestSerializer(@NotNull DataSourceRegistry<DataSourceDescriptor> registry) {
            super(registry);
        }

        @NotNull
        private static String resolveFolderPath(@NotNull String name, @NotNull Map<String, Object> configuration) {
            return getFolderPath(name, configuration);
        }
    }
}
