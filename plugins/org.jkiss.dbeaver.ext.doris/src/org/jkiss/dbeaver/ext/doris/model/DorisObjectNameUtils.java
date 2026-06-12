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
package org.jkiss.dbeaver.ext.doris.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;

final class DorisObjectNameUtils {

    private DorisObjectNameUtils() {
    }

    @Nullable
    static GenericCatalog getCatalog(@NotNull GenericStructContainer container) {
        if (container instanceof GenericSchema schema) {
            return schema.getCatalog();
        }
        return container.getCatalog();
    }

    @Nullable
    static GenericSchema getSchema(@NotNull GenericStructContainer container) {
        if (container instanceof GenericSchema schema) {
            return schema;
        }
        return container.getSchema();
    }

    @NotNull
    static String getFullyQualifiedName(
        @NotNull DorisDataSource dataSource,
        @Nullable GenericCatalog catalog,
        @Nullable GenericSchema schema,
        @NotNull DBPNamedObject object
    ) {
        if (catalog != null && schema != null) {
            return DBUtils.getFullQualifiedName(dataSource, catalog, schema, object);
        } else if (schema != null) {
            return DBUtils.getFullQualifiedName(dataSource, schema, object);
        }
        return DBUtils.getQuotedIdentifier(dataSource, object.getName());
    }
}
