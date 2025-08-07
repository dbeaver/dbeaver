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
package org.jkiss.dbeaver.model.sql.semantics.context;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SQLQueryConnectionContext {
    private static final Log log = Log.getLog(SQLQueryConnectionContext.class);

    @NotNull
    public final SQLDialect dialect;

    protected SQLQueryConnectionContext(
        @NotNull SQLDialect dialect
    ) {
        this.dialect = dialect;
    }

    @NotNull
    public abstract List<SQLQueryResultPseudoColumn> obtainRowsetPseudoColumns(@Nullable SQLQueryRowsSourceModel rowsSource);

    /**
     * Resolve target object for alias
     */
    @Nullable
    public static DBSObject expandAliases(@NotNull DBRProgressMonitor monitor, @Nullable DBSObject obj) {
        // TODO treat alias as a virtual table instead of blind expansion!
        while (obj instanceof DBSAlias aliasObject) {
            try {
                obj = aliasObject.getTargetObject(monitor);
            } catch (DBException e) {
                obj = null;
                log.debug("Can't resolve target object for alias '" + aliasObject.getName() + "'", e);
            }
        }
        return obj;
    }

    /**
     * Find real table referenced by its name in the database
     */
    @NotNull
    public List<DBSEntity> findRealTables(@NotNull DBRProgressMonitor monitor, @NotNull List<String> tableName) {
        // TODO consider differentiating direct references vs expanded aliases:
        //  each alias expansion should be treated as a virtual table
        List<? extends DBSObject> objs = this.findRealObjectsImpl(monitor, tableName);
        if (objs.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<DBSEntity> results = new ArrayList<>(objs.size());
            for (int i  = 0; i < objs.size(); i++) {
                DBSObject obj = expandAliases(monitor, objs.get(i));
                if (obj instanceof DBSTable || obj instanceof DBSView) {
                    results.add((DBSEntity) obj);
                }
            }
            return results;
        }
    }

    /**
     * Find real object of given type referenced by its name in the database
     */
    @NotNull
    public final List<? extends DBSObject> findRealObjects(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObjectType objectType,
        @NotNull List<String> objectName
    ) {
        List<? extends DBSObject> objs = this.findRealObjectsImpl(monitor, objectName);
        if (objs.isEmpty()) {
            return Collections.emptyList();
        } else if (objs.size() == 1) {
            return objectType.getTypeClass().isInstance(objs.getFirst()) ? objs : Collections.emptyList();
        } else {
            return objs.stream().filter(o ->  objectType.getTypeClass().isInstance(o)).toList();
        }
    }

    public abstract boolean isDummy();

    @NotNull
    protected abstract List<? extends DBSObject> findRealObjectsImpl(@NotNull DBRProgressMonitor monitor, @NotNull List<String> objectName);

    @Nullable
    public abstract SQLQueryResultPseudoColumn resolveGlobalPseudoColumn(@NotNull String name);
}
