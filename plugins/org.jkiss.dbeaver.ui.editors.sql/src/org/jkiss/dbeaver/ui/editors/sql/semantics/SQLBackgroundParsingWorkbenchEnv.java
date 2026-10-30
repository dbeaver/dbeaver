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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.ui.IEditorInput;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.RunnableWithResult;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.parser.SQLParserContext;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryVariablesSubset;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;

import java.util.List;
import java.util.function.Function;

public final class SQLBackgroundParsingWorkbenchEnv implements SQLBackgroundParsingEnvironment {
    public static final SQLBackgroundParsingWorkbenchEnv INSTANCE = new SQLBackgroundParsingWorkbenchEnv();

    private SQLBackgroundParsingWorkbenchEnv() {
    }

    @NotNull
    @Override
    public Job createJob(@NotNull Function<DBRProgressMonitor, IStatus> work) {
        return new WorkbenchParsingJob(work);
    }

    @Nullable
    @Override
    public Interval getVisibleFragment(@NotNull TextViewer viewer) {
        return UIUtils.syncExec(new RunnableWithResult<>() {
            @Nullable
            @Override
            public Interval runWithResult() {
                IDocument doc = viewer.getDocument();
                if (doc == null) {
                    return null;
                }
                int stepsToKeep = 2;
                int startOffset = viewer.getTopIndexStartOffset();
                try {
                    int firstVisibleLine = doc.getLineOfOffset(startOffset);
                    int visibleLinesCount = viewer.getTextWidget().getSize().y / viewer.getTextWidget().getLineHeight();
                    int rangeStart = doc.getLineOffset(Math.max(0, firstVisibleLine - visibleLinesCount * stepsToKeep));
                    int rangeEnd = doc.getLineOffset(Math.min(
                        doc.getNumberOfLines(), firstVisibleLine + visibleLinesCount * (stepsToKeep + 1)
                    ));
                    return new Interval(rangeStart, rangeEnd);
                } catch (BadLocationException e) {
                    return new Interval(startOffset, viewer.getBottomIndexEndOffset());
                }
            }
        });
    }

    @Override
    public void invalidatePresentation(@NotNull TextViewer viewer, int offset, int length) {
        UIUtils.asyncExec(() -> viewer.invalidateTextPresentation(offset, length));
    }

    @Override
    public boolean isParserApplied(@Nullable IEditorInput input) {
        return SQLEditorUtils.isSQLSyntaxParserApplied(input);
    }

    @Nullable
    @Override
    public DBPDataSourceContainer getInputDataSource(@Nullable IEditorInput input) {
        return EditorUtils.getInputDataSource(input);
    }

    @NotNull
    @Override
    public List<SQLScriptElement> extractScriptQueries(
        @NotNull SQLParserContext context, int offset, int length,
        boolean scriptMode, boolean keepDelimiters, boolean parseParameters, boolean includeBatchDelimiters
    ) {
        return SQLScriptParser.extractScriptQueries(
            context, offset, length, scriptMode, keepDelimiters, parseParameters, includeBatchDelimiters
        );
    }

    @Nullable
    @Override
    public SQLQueryModel recognizeQuery(
        @NotNull SQLQueryRecognitionContext context, @NotNull String text, @NotNull SQLQueryVariablesSubset varsSubset
    ) {
        return SQLQueryModelRecognizer.recognizeQuery(context, text, varsSubset);
    }

    private static final class WorkbenchParsingJob extends AbstractJob implements SQLBackgroundParsingEnvironment.Job {
        @NotNull
        private final Function<DBRProgressMonitor, IStatus> work;

        private WorkbenchParsingJob(@NotNull Function<DBRProgressMonitor, IStatus> work) {
            super("Background parsing job");
            this.work = work;
        }

        @NotNull
        @Override
        protected IStatus run(@NotNull DBRProgressMonitor monitor) {
            this.setSkipErrorOnCanceling(true);
            return this.work.apply(monitor);
        }
    }
}
