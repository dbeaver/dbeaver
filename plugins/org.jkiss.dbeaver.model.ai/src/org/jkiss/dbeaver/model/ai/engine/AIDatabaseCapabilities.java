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
package org.jkiss.dbeaver.model.ai.engine;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

public record AIDatabaseCapabilities(@Nullable Boolean catalogs, @Nullable Boolean schemas) {
    @NotNull
    public static AIDatabaseCapabilities read(@NotNull AIFunctionContext context) throws DBException {
        AIDatabaseContext database = context.getContext();
        if (database == null) {
            return new AIDatabaseCapabilities(null, null);
        }
        var executionContext = database.getExecutionContext();
        if (!(executionContext.getDataSource() instanceof DBSObjectContainer container)) {
            return new AIDatabaseCapabilities(false, false);
        }
        Class<? extends DBSObject> childType = container.getPrimaryChildType(context.getMonitor());
        boolean catalogs = DBSCatalog.class.isAssignableFrom(childType);
        Boolean schemas = DBSSchema.class.isAssignableFrom(childType);
        if (catalogs) {
            DBCExecutionContextDefaults<?, ?> defaults = executionContext.getContextDefaults();
            DBSCatalog catalog = defaults == null ? null : defaults.getDefaultCatalog();
            // catalog/schema terms alone are unreliable, e.g. SQLite JDBC advertises a schema term
            schemas = catalog == null ? null
                : DBSSchema.class.isAssignableFrom(catalog.getPrimaryChildType(context.getMonitor()));
        }
        return new AIDatabaseCapabilities(catalogs, schemas);
    }
}
