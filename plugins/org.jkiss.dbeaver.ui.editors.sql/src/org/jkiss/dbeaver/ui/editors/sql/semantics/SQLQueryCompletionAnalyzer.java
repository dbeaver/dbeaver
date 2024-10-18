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

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.viewers.StyledString;
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
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class SQLQueryCompletionAnalyzer implements DBRRunnableParametrized<DBRProgressMonitor> {

    private static final Log log = Log.getLog(SQLCompletionAnalyzer.class);

    private final StyledString.Styler extraTextStyler = StyledString.createColorRegistryStyler(JFacePreferences.DECORATIONS_COLOR, null);

    @NotNull
    private final SQLEditorBase editor;
    @NotNull
    private final SQLCompletionRequest request;
    @NotNull
    private final Position completionRequestPostion;
    @NotNull
    private volatile List<ICompletionProposal> proposals = Collections.emptyList();

    private final SQLQueryCompletionProposalContext proposalContext;

    public SQLQueryCompletionAnalyzer(@NotNull SQLEditorBase editor, @NotNull SQLCompletionRequest request, Position completionRequestPostion) {
        this.editor = editor;
        this.request = request;
        this.completionRequestPostion = completionRequestPostion;
        this.proposalContext = new SQLQueryCompletionProposalContext(request);
    }

    @Override
    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        SQLQueryCompletionContext completionContext = this.editor.obtainCompletionContext(this.completionRequestPostion);

        if (completionContext != null && this.request.getContext().getDataSource() != null) {
            String fragment = getTextFragmentAt(this.completionRequestPostion.offset - 2, 2);
            if (fragment != null && Pattern.matches("^[\\s\\,]\\*$", fragment)) {
                this.prepareColumnsTupleSubstitution(monitor, completionContext);
            } else {
                this.prepareContextfulCompletion(monitor, completionContext);

                if (this.request.getContext().isSortAlphabetically()) {
                    this.proposals.sort(Comparator.comparing(ICompletionProposal::getDisplayString, String::compareToIgnoreCase));
                }
            }
        }
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

    private void prepareColumnsTupleSubstitution(DBRProgressMonitor monitor, SQLQueryCompletionContext completionContext) {
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
            this.completionRequestPostion.getOffset() - 1,
            1,
            null
        );

        this.proposals = List.of(proposal);
    }

    private void prepareContextfulCompletion(DBRProgressMonitor monitor, SQLQueryCompletionContext completionContext) {
        SQLQueryCompletionSet completionSet = completionContext.prepareProposal(monitor, this.completionRequestPostion.getOffset(), this.request);
        SQLQueryCompletionTextProvider textProvider = new SQLQueryCompletionTextProvider(this.request, completionContext, monitor);

        this.proposals = new ArrayList<>(completionSet.getItems().size());
        for (SQLQueryCompletionItem item : completionSet.getItems()) {
            DBSObject object = SQLQueryDummyDataSourceContext.isDummyObject(item.getObject()) ? null : item.getObject();
            String text = item.apply(textProvider);
            String decoration = item.apply(SQLQueryCompletionExtraTextProvider.INSTANCE);
            String description = item.apply(SQLQueryCompletionDescriptionProvider.INSTANCE);
            String replacementString = this.prepareReplacementString(item, text);
            this.proposals.add(new SQLQueryCompletionProposal(
                this.proposalContext,
                item.getKind(),
                object,
                this.preparePoposalImage(item),
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

    @NotNull
    private String prepareReplacementString(@NotNull SQLQueryCompletionItem item, @NotNull String text) {
        boolean whitespaceNeeded = item.getKind() == SQLQueryCompletionItemKind.RESERVED
            || (!text.endsWith(" ") && this.proposalContext.isInsertSpaceAfterProposal());
        return whitespaceNeeded ? text + " " : text;
    }

    @NotNull
    public List<? extends ICompletionProposal> getProposals() {
        return this.proposals;
    }

    @NotNull
    private DBPImage preparePoposalImage(@NotNull SQLQueryCompletionItem item) {
        DBPImage image = switch (item.getKind()) {
            case UNKNOWN ->  DBValueFormatting.getObjectImage(item.getObject());
            case RESERVED -> UIIcon.SQL_TEXT;
            case SUBQUERY_ALIAS -> DBIcon.TREE_TABLE_ALIAS;
            case DERIVED_COLUMN_NAME -> DBIcon.TREE_FOREIGN_KEY_COLUMN;
            case NEW_TABLE_NAME -> DBIcon.TREE_TABLE;
            case USED_TABLE_NAME -> UIIcon.EDIT_TABLE;
            case TABLE_COLUMN_NAME -> DBIcon.TREE_COLUMN;
            default -> throw new IllegalStateException("Unexpected completion item kind " + item.getKind());
        };
        return image;
    }
}