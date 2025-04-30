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
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Map;
import java.util.Set;

/**
 * Provides information about resolved sources with ability to separately provide tables and aliases used in the query
 */
public interface SQLQuerySourcesInfoCollection {

    /**
     * Returns all resolved query sources
     */
    @NotNull
    Map<SQLQueryRowsSourceModel, SourceResolutionResult> getResolutionResults();

    /**
     * Returns a set of tables used in the query
     */
    @NotNull
    Set<DBSObject> getReferencedTables();

    /**
     * Returns a set of aliases used in the query
     */
    @NotNull
    Set<String> getAliasesInUse();
}
