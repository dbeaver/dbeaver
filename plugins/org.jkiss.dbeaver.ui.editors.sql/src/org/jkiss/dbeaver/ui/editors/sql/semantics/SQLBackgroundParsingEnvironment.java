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
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.antlr.v4.runtime.misc.Interval;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.ui.IEditorInput;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.parser.SQLParserContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryVariablesSubset;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;

import java.util.List;
import java.util.function.Function;

/**
 * Editor, workbench, and parsing operations performed on behalf of a parsing session.
 *
 * <p>FIXME: This covers only the minimal API surface needed for synchronization tests;
 * further decomposition of the {@link SQLBackgroundParsingJob} may be desirable in the future.</p>
 */
public interface SQLBackgroundParsingEnvironment {
    @NotNull
    Job createJob(@NotNull Function<DBRProgressMonitor, IStatus> work);

    @Nullable
    Interval getVisibleFragment(@NotNull TextViewer viewer);

    void invalidatePresentation(@NotNull TextViewer viewer, int offset, int length);

    boolean isParserApplied(@Nullable IEditorInput input);

    @Nullable
    DBPDataSourceContainer getInputDataSource(@Nullable IEditorInput input);

    @NotNull
    List<SQLScriptElement> extractScriptQueries(
        @NotNull SQLParserContext context, int offset, int length,
        boolean scriptMode, boolean keepDelimiters, boolean parseParameters, boolean includeBatchDelimiters
    );

    @Nullable
    SQLQueryModel recognizeQuery(
        @NotNull SQLQueryRecognitionContext context, @NotNull String text, @NotNull SQLQueryVariablesSubset varsSubset
    );

    /** Job controls exposed to the parser without tying its lifecycle to an Eclipse Job. */
    interface Job {
        int getState();

        boolean cancel();

        void schedule(long delayMillis);
    }
}
