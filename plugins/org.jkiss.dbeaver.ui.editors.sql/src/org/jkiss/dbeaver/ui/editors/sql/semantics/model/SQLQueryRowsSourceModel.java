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
package org.jkiss.dbeaver.ui.editors.sql.semantics.model;


import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;


public abstract class SQLQueryRowsSourceModel extends SQLQueryNodeModel {
    private SQLQueryDataContext dataContext;

    public SQLQueryRowsSourceModel(Interval region) {
        super(region);
        this.dataContext = null;
    }

    @NotNull
    public SQLQueryDataContext getDataContext() {
        if (this.dataContext == null) {
            throw new UnsupportedOperationException("Data context was not resolved for the rows source yet");
        } else {
            return this.dataContext;
        }
    }

    @NotNull
    public SQLQueryDataContext propagateContext(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        return this.dataContext = this.propagateContextImpl(context, statistics);
    }

    protected abstract SQLQueryDataContext propagateContextImpl(
        @NotNull SQLQueryDataContext context,
        @NotNull SQLQueryRecognitionContext statistics
    );
}


