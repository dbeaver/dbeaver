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
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableParametrized;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionAnalyzer;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionContext;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionItem;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionItemKind;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionSet;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDummyDataSourceContext;
import org.jkiss.dbeaver.model.stm.LSMInspections;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.utils.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class SQLQueryCompletionAnalyzer implements DBRRunnableParametrized<DBRProgressMonitor> {

    private static final Log log = Log.getLog(SQLCompletionAnalyzer.class);

    @NotNull
    private final SQLEditorBase editor;
    @NotNull
    private final SQLCompletionRequest request;
    @NotNull
    private final Position completionRequestPosition;
    @NotNull
    private final AtomicReference<Pair<Integer, List<SQLQueryCompletionProposal>>> result = new AtomicReference<>(Pair.of(null, Collections.emptyList()));

    private final SQLQueryCompletionProposalContext proposalContext;

    public SQLQueryCompletionAnalyzer(
        @NotNull SQLEditorBase editor,
        @NotNull SQLCompletionRequest request,
        @NotNull Position completionRequestPosition
    ) {
        this.editor = editor;
        this.request = request;
        this.completionRequestPosition = completionRequestPosition;
        this.proposalContext = new SQLQueryCompletionProposalContext(request);
    }

    @Override
    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        SQLQueryCompletionContext completionContext = this.editor.obtainCompletionContext(monitor, this.completionRequestPosition);
//        while (completionContext.getRequestOffset() != this.completionRequestPostion.getOffset()) {
//            // Context preparation was initiated after the parsing when the user stopped typing,
//            // but then he started typing again, before the context preparation was finished.
//            // No need to proceed with actual proposals preparation, we can just repeat the completion context preparation.
//            completionContext = this.editor.obtainCompletionContext(this.completionRequestPostion);
//        }

        Pair<Integer, List<SQLQueryCompletionProposal>> result;
        if (completionContext != null && this.request.getContext().getDataSource() != null) {
            // TODO don't we want to be able to accomplish subqueries and such even without the connection?
            result = this.prepareProposals(monitor, completionContext);
        } else {
            result = Pair.of(this.completionRequestPosition.getOffset(), Collections.emptyList());
        }

        this.result.set(result);
    }

    @NotNull
    private Pair<Integer, List<SQLQueryCompletionProposal>> prepareProposals(DBRProgressMonitor monitor, SQLQueryCompletionContext completionContext) {
        Pair<Integer, List<SQLQueryCompletionProposal>> result;
        String fragment = getTextFragmentAt(completionContext.getRequestOffset() - 2, 2);
        List<SQLQueryCompletionProposal> proposals;
        if (fragment != null && Pattern.matches("^[\\s\\,]\\*$", fragment)) {
            proposals = this.prepareColumnsTupleSubstitution(monitor, completionContext);
        } else {
            proposals = this.prepareContextfulCompletion(monitor, completionContext);

            if (this.request.getContext().isSortAlphabetically()) {
                proposals.sort(Comparator.comparing(ICompletionProposal::getDisplayString, String::compareToIgnoreCase));
            }
        }
        result = Pair.of(completionContext.getRequestOffset(), proposals);
        return result;
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

    private List<SQLQueryCompletionProposal> prepareColumnsTupleSubstitution(DBRProgressMonitor monitor, SQLQueryCompletionContext completionContext) {
        SQLQueryCompletionTextProvider formatter = new SQLQueryCompletionTextProvider(this.request, completionContext, monitor);
        String columnListString = completionContext.prepareCurrentTupleColumns().stream()
            .map(c -> c.apply(formatter))
            .collect(Collectors.joining(", "));
        request.setWordPart(SQLConstants.ASTERISK);

        var proposal = new SQLQueryCompletionProposal(
            this.proposalContext,
            SQLQueryCompletionItemKind.UNKNOWN,
            null,
            null,
            columnListString,
            null,
            "Complete columns tuple",
            columnListString,
            completionContext.getRequestOffset() - 1,
            1,
            null
        );

        return List.of(proposal);
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
                    proposals.add(new SQLQueryCompletionProposal(
                        this.proposalContext,
                        item.getKind(),
                        object,
                        this.prepareProposalImage(item),
                        text,
                        decoration,
                        description,
                        replacementString,
                        completionSet.getReplacementPosition(),
                        completionSet.getReplacementLength(),
                        item.getFilterInfo()
                    ));
                }
            }
        }

        return proposals;
    }

    @NotNull
    private String prepareReplacementString(@NotNull SQLQueryCompletionItem item, @NotNull String text, @NotNull SQLQueryCompletionContext completionContext) {
        LSMInspections.SyntaxInspectionResult inspectionResult = completionContext.getInspectionResult();
        boolean whitespaceNeeded = item.getKind() == SQLQueryCompletionItemKind.RESERVED ||
            (!text.endsWith(" ") && this.proposalContext.isInsertSpaceAfterProposal() && (
                (inspectionResult.expectingTableReference() && item.getKind().isTableName) ||
                (inspectionResult.expectingColumnReference() && item.getKind().isColumnName)
            ));
        return whitespaceNeeded ? text + " " : text;
    }

    @NotNull
    public List<? extends ICompletionProposal> getResult() {
        return this.result.get().getSecond();
    }

    public Integer getActualContextOffset() {
        return this.result.get().getFirst();
    }

    @NotNull
    private DBPImage prepareProposalImage(@NotNull SQLQueryCompletionItem item) {
        DBPImage image = switch (item.getKind()) {
            case UNKNOWN ->  DBValueFormatting.getObjectImage(item.getObject());
            case RESERVED -> UIIcon.SQL_TEXT;
            case SUBQUERY_ALIAS -> DBIcon.TREE_TABLE_ALIAS;
            case DERIVED_COLUMN_NAME -> DBIcon.TREE_FOREIGN_KEY_COLUMN;
            case NEW_TABLE_NAME -> DBIcon.TREE_TABLE;
            case USED_TABLE_NAME -> UIIcon.EDIT_TABLE;
            case TABLE_COLUMN_NAME -> DBIcon.TREE_COLUMN;
            case JOIN_CONDITION -> DBIcon.TREE_CONSTRAINT;
            default -> throw new IllegalStateException("Unexpected completion item kind " + item.getKind());
        };
        return image;
    }
}