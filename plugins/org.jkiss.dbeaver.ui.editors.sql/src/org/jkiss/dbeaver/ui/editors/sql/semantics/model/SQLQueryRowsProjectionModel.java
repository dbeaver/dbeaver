/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbol;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;

import java.util.List;

public class SQLQueryRowsProjectionModel extends SQLQueryRowsSourceModel {
    private final SQLQueryRowsSourceModel fromSource; // from tableExpression
    private final SQLQuerySelectionResultModel result; // selectList
    
    public SQLQueryRowsProjectionModel(@NotNull Interval range, @NotNull SQLQueryRowsSourceModel fromSource, @NotNull SQLQuerySelectionResultModel result) {
    	super(range);
        this.result = result;
        this.fromSource = fromSource;
    }
    
    public SQLQueryRowsSourceModel getFromSource() {
		return fromSource;
	}

	public SQLQuerySelectionResultModel getResult() {
		return result;
	}

	@NotNull
    @Override
    protected SQLQueryDataContext propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        context = fromSource.propagateContext(context, statistics);
        List<SQLQuerySymbol> resultColumns = this.result.expandColumns(context, statistics); 
        return context.overrideResultTuple(resultColumns).hideSources();
    }
    
    @Override
    protected <R, T> R applyImpl(SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
    	return visitor.visitRowsProjection(this, arg);
    }
}