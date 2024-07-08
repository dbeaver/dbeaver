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
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.viewers.StyledString;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableParametrized;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionAnalyzer;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionContext;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionItem;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionItemKind;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionSet;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDummyDataSourceContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLCompletionProposal;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class SQLQueryCompletionAnalyzer implements DBRRunnableParametrized<DBRProgressMonitor> {

    private static final Log log = Log.getLog(SQLCompletionAnalyzer.class);

    private final StyledString.Styler extraTextStyler = StyledString.createColorRegistryStyler(JFacePreferences.DECORATIONS_COLOR, null);

    @NotNull
    private final SQLEditorBase editor;
    @NotNull
    private final SQLCompletionRequest request;
    @NotNull
    private volatile List<SQLCompletionProposal> proposals = Collections.emptyList();

    public SQLQueryCompletionAnalyzer(@NotNull SQLEditorBase editor, @NotNull SQLCompletionRequest request) {
        this.editor = editor;
        this.request = request;
    }

    @Override
    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        int position = this.request.getDocumentOffset();
        SQLQueryCompletionContext completionContext = this.editor.obtainCompletionContext(position);
        if (completionContext != null && this.request.getContext().getDataSource() != null) {
            SQLQueryCompletionSet completionSet = completionContext.prepareProposal(monitor, position, this.request);
            SQLQueryCompletionTextProvider formatter = new SQLQueryCompletionTextProvider(this.request, completionContext, monitor);
            SQLQueryCompletionExtraTextProvider extraFormatter = new SQLQueryCompletionExtraTextProvider();
            SQLQueryCompletionDescriptionProvider descriptionProvider = new SQLQueryCompletionDescriptionProvider();

            this.proposals = new ArrayList<>(completionSet.getItems().size()); 
            for (SQLQueryCompletionItem item : completionSet.getItems()) {
                DBPNamedObject object = SQLQueryDummyDataSourceContext.isDummyObject(item.getObject()) ? null : item.getObject();
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
                DBPKeywordType kwType = item.getKind() == SQLQueryCompletionItemKind.RESERVED ? DBPKeywordType.KEYWORD : DBPKeywordType.OTHER;
                String text = item.apply(formatter);
                String description = item.apply(descriptionProvider); // TODO maybe move to the getAdditionalInfo(..) ?
                this.proposals.add(new SQLCompletionProposal(this.request, text, text, text.length(), image, kwType, description, object, Collections.emptyMap()) {
                    @Override
                    public Object getAdditionalInfo(DBRProgressMonitor monitor) {
                        if (object instanceof DBSObject o) {
                            // preload object info, like at SQLCompletionAnalyzer.makeProposalsFromObject(..)
                            // but maybe instead put it to SuggestionInformationControl.createTreeControl(..),
                            //                where the DBNDatabaseNode is required but missing if not cached
                            DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(monitor, o, true);
                        }
                        return super.getAdditionalInfo(monitor);
                    }

                    @Override
                    public StyledString getStyledDisplayString() {
                        StyledString result = super.getStyledDisplayString();
                        String extra = item.apply(extraFormatter);
                        if (extra != null) {
                            int oldLength = result.length();
                            result.append(" " + extra);
                            result.setStyle(oldLength, result.length() - oldLength, extraTextStyler);
                        }
                        return result;
                    }
                });
//                Image eimage = DBeaverIcons.getImage(image);
//                this.proposals.add(new CompletionProposal(
//                        item.getText(),
//                        completionSet.getReplacementPosition(),
//                        completionSet.getReplacementLength(),
//                        item.getText().length(),
//                        eimage,
//                        item.getText(),
//                        new ContextInformation(
//                                eimage,
//                                item.getExtraText() == null ? "" : item.getExtraText(),
//                                item.getDescription() == null ? "" : item.getDescription()
//                        ),
//                        item.getDescription()
//                ));
            }

            if (this.request.getContext().isSortAlphabetically()) {
                this.proposals.sort(Comparator.comparing(SQLCompletionProposal::getReplacementString, String::compareToIgnoreCase));
            }
        }
    }

    @NotNull
    public List<? extends ICompletionProposal> getProposals() {
        return this.proposals;
    }
}