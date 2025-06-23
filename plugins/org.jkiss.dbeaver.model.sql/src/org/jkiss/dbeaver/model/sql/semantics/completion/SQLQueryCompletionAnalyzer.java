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
package org.jkiss.dbeaver.model.sql.semantics.completion;

import org.eclipse.jface.text.BadLocationException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableParametrized;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionAnalyzer;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionContext;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDummyDataSourceContext;
import org.jkiss.dbeaver.model.stm.LSMInspections;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;


public class SQLQueryCompletionAnalyzer implements DBRRunnableParametrized<DBRProgressMonitor> {

    private static final Log log = Log.getLog(SQLCompletionAnalyzer.class);

    @NotNull
    private final Function<DBRProgressMonitor, SQLQueryCompletionContext> completionContextSupplier;
    @NotNull
    protected final SQLCompletionRequest request;
    @NotNull
    private final Supplier<Integer> currentCompletionOffsetSupplier;
    @NotNull
    private final AtomicReference<Pair<Integer, List<SQLQueryCompletionProposal>>> result = new AtomicReference<>(Pair.of(null, Collections.emptyList()));
    private SQLQueryCompletionProposalContext proposalContext;

    public SQLQueryCompletionAnalyzer(
        @NotNull Function<DBRProgressMonitor, SQLQueryCompletionContext> completionContextSupplier,
        @NotNull SQLCompletionRequest request,
        @NotNull Supplier<Integer> currentCompletionOffsetSupplier
    ) {
        this.completionContextSupplier = completionContextSupplier;
        this.request = request;
        this.currentCompletionOffsetSupplier = currentCompletionOffsetSupplier;
    }

    @Override
    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        SQLQueryCompletionContext completionContext = this.completionContextSupplier.apply(monitor);
//        while (completionContext.getRequestOffset() != this.completionRequestPostion.getOffset()) {
//            // Context preparation was initiated after the parsing when the user stopped typing,
//            // but then he started typing again, before the context preparation was finished.
//            // No need to proceed with actual proposals preparation, we can just repeat the completion context preparation.
//            completionContext = this.editor.obtainCompletionContext(this.completionRequestPostion);
//        }

        Pair<Integer, List<SQLQueryCompletionProposal>> result;
        SQLCompletionContext requestContext = this.request.getContext();
        if (completionContext != null && requestContext.getDataSource() != null && requestContext.getExecutionContext() != null) {
            // TODO don't we want to be able to accomplish subqueries and such even without the connection?
            this.proposalContext = this.createProposalContext(completionContext);
            List<SQLQueryCompletionProposal> proposals = this.prepareContextfulCompletion(monitor, completionContext);
            result = Pair.of(completionContext.getRequestOffset(), proposals);
        } else {
            int completionRequestPosition = completionContext != null
                ? completionContext.getRequestOffset()
                : this.currentCompletionOffsetSupplier.get();
            result = Pair.of(completionRequestPosition, Collections.emptyList());
        }

        this.result.set(result);
    }

    @NotNull
    protected SQLQueryCompletionProposalContext createProposalContext(@NotNull SQLQueryCompletionContext completionContext) {
        return new SQLQueryCompletionProposalContext(this.request, completionContext.getRequestOffset());
    }

    private String getTextFragmentAt(int offset, int length) {
        if (offset >= 0 && offset + length <= this.request.getDocument().getLength()) {
            try {
                return this.request.getDocument().get(offset, length);
            } catch (BadLocationException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    private List<SQLQueryCompletionProposal> prepareContextfulCompletion(DBRProgressMonitor monitor, SQLQueryCompletionContext completionContext) {
        Collection<SQLQueryCompletionSet> completionSets = completionContext.prepareProposal(monitor, this.request);
        SQLQueryCompletionTextProvider textProvider = new SQLQueryCompletionTextProvider(this.request, completionContext, monitor);

        List<SQLQueryCompletionProposal> proposals = new LinkedList<>();

        // FIXME forcibly exclude duplicated completions for now;
        //  correct fix requires better completion scenarios distinguishing to not prepare unnecessary items at all
        Set<String> texts = new HashSet<>();
        for (SQLQueryCompletionSet completionSet : completionSets) {
            for (SQLQueryCompletionItem item : completionSet.getItems()) {
                DBSObject object = SQLQueryDummyDataSourceContext.isDummyObject(item.getObject()) ? null : item.getObject();
                String text = item.apply(textProvider);
                if (texts.add(text)) {
                    String decoration = item.apply(SQLQueryCompletionExtraTextProvider.INSTANCE);
                    String description = item.apply(SQLQueryCompletionDescriptionProvider.INSTANCE);
                    String replacementString = this.prepareReplacementString(item, text, completionContext);
                    proposals.add(this.createProposal(
                        item.getKind(),
                        object,
                        this.prepareProposalImage(item),
                        text,
                        decoration,
                        description,
                        replacementString,
                        completionSet.getReplacementPosition(),
                        completionSet.getReplacementLength(),
                        item.getFilterInfo(),
                        item.getScore()
                    ));
                }
            }
        }

        return proposals;
    }

    protected SQLQueryCompletionProposal createProposal(
        @NotNull SQLQueryCompletionItemKind itemKind,
        @Nullable DBSObject object,
        @Nullable DBPImage image,
        @Nullable String displayString,
        @Nullable String decorationString,
        @NotNull String description,
        @NotNull String replacementString,
        int replacementOffset,
        int replacementLength,
        @Nullable SQLQueryWordEntry filterString,
        int proposalScore
    ) {
        return new SQLQueryCompletionProposal(
            this.proposalContext,
            itemKind,
            object,
            image,
            displayString,
            decorationString,
            description,
            replacementString,
            replacementOffset,
            replacementLength,
            filterString,
            proposalScore
        );
    }

    @NotNull
    private String prepareReplacementString(@NotNull SQLQueryCompletionItem item, @NotNull String text, @NotNull SQLQueryCompletionContext completionContext) {
        LSMInspections.SyntaxInspectionResult inspectionResult = completionContext.getInspectionResult();
        boolean whitespaceNeeded = item.getKind() == SQLQueryCompletionItemKind.RESERVED ||
            (!text.endsWith(" ") && this.proposalContext.isInsertSpaceAfterProposal() && (
                (inspectionResult.expectingTableReference() && item.getKind().isTableName) ||
                ((inspectionResult.expectingColumnReference() || inspectionResult.expectingColumnName()) && item.getKind().isColumnName)
            ));
        return whitespaceNeeded ? text + " " : text;
    }

    @NotNull
    public List<? extends SQLQueryCompletionProposal> getResult() {
        return this.result.get().getSecond();
    }

    public Integer getActualContextOffset() {
        return this.result.get().getFirst();
    }

    @Nullable
    protected DBPImage prepareProposalImage(@NotNull SQLQueryCompletionItem item) {
        return null;
    }
}