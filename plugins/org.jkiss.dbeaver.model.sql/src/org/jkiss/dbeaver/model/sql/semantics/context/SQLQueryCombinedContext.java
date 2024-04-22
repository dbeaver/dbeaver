/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.stm.STMUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.util.List;

/**
 * Represents combination of two contexts made as a result of some subsets merging operation
 */
public class SQLQueryCombinedContext extends SQLQueryResultTupleContext {
    private final SQLQueryDataContext otherParent;

    public SQLQueryCombinedContext(@NotNull SQLQueryDataContext left, @NotNull SQLQueryDataContext right) {
        super(left, STMUtils.combineLists(left.getColumnsList(), right.getColumnsList()));
        this.otherParent = right;
    }

    @Nullable
    @Override
    public SQLQueryRowsSourceModel findRealSource(@NotNull DBSEntity table) {
        return anyOfTwo(parent.findRealSource(table), otherParent.findRealSource(table)); // TODO consider ambiguity
    }

    @Nullable
    @Override
    public DBSEntity findRealTable(@NotNull DBRProgressMonitor monitor, @NotNull List<String> tableName) {
        return anyOfTwo(parent.findRealTable(monitor, tableName), otherParent.findRealTable(monitor, tableName)); // TODO consider ambiguity
    }

    @Nullable
    @Override
    public SourceResolutionResult resolveSource(@NotNull DBRProgressMonitor monitor, @NotNull List<String> tableName) {
        return anyOfTwo(parent.resolveSource(monitor, tableName), otherParent.resolveSource(monitor, tableName)); // TODO consider ambiguity
    }

    @Nullable
    private static <T> T anyOfTwo(@Nullable T a, @Nullable T b) {
        return a != null ? a : b;
    }
    
    @Override
    protected void collectKnownSourcesImpl(@NotNull KnownSourcesInfo result) {
        this.parent.collectKnownSourcesImpl(result);
        this.otherParent.collectKnownSourcesImpl(result);
    }
}
