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
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.util.List;

/**
 * Hides all the aggregated row sources leaving just the result tuple columns coming from parent context
 */
public class SQLQueryPureResultTupleContext extends SQLQuerySyntaxContext {

    public SQLQueryPureResultTupleContext(@NotNull SQLQueryDataContext parent) {
        super(parent);
    }

    @Override
    public boolean hasUnresolvedSource() {
        return false;
    }

    @Nullable
    @Override
    public SourceResolutionResult resolveSource(@NotNull DBRProgressMonitor monitor, @NotNull List<String> tableName) {
        return null;
    }

    @Nullable
    @Override
    public SQLQueryRowsSourceModel findRealSource(@NotNull DBSEntity table) {
        return null;
    }
    
    @Override
    protected void collectKnownSourcesImpl(@NotNull KnownSourcesInfo result) {
        // no further sources visible
    }
    
    @Override
    public KnownSourcesInfo getKnownSources() {
        return this.parent.getKnownSources();
    }
}

