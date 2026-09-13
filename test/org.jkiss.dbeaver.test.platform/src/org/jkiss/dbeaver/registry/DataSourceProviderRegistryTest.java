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

import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;

public class DataSourceProviderRegistryTest {
    @Test
    public void testConnectionTypesCollectionIsStableDuringRegistryUpdate() {
        DataSourceProviderRegistry registry = DataSourceProviderRegistry.getInstance();
        Collection<DBPConnectionType> connectionTypesSnapshot = registry.getConnectionTypes();
        DBPConnectionType customType = new DBPConnectionType(
            "snapshot-test",
            "Snapshot test",
            "1,2,3",
            "4,5,6",
            null,
            true,
            false,
            false,
            false,
            false,
            false,
            0,
            false,
            0
        );

        try {
            registry.addConnectionType(customType);

            Assertions.assertSame(customType, registry.getConnectionType(customType.getId(), null));
            Assertions.assertFalse(connectionTypesSnapshot.contains(customType));
        } finally {
            registry.removeConnectionType(customType);
        }
    }
}
