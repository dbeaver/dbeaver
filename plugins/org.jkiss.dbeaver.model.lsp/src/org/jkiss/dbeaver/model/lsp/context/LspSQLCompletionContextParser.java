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
package org.jkiss.dbeaver.model.lsp.context;

import org.eclipse.jface.text.Document;
import org.eclipse.lsp4j.CompletionItem;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLModelPreferences;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.completion.CompletionProposalBase;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionAnalyzer;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionContext;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.parser.SQLParserContext;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.model.sql.semantics.SQLDocumentSyntaxContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLScriptItemAtOffset;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionAnalyzer;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionContext;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class LspSQLCompletionContextParser {

    private static final String DEFAULT_ENGINE_COMPLETION = "DEFAULT";

    // FIXME: Mostly copy-pasted from WebSQLCompletionContextScriptParser.obtainCompletionContext
    @NotNull
    public static SQLQueryCompletionContext obtainCompletionContext(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String query,
        int position,
        SQLCompletionRequest request
    ) {
        Document document = new Document(query);
        SQLDocumentSyntaxContext syntaxContext = new SQLDocumentSyntaxContext();
        SQLParserContext parserContext = new SQLParserContext(
            request.getContext().getDataSource(),
            request.getContext().getSyntaxManager(),
            request.getContext().getRuleManager(),
            document
        );
        var scriptItems = SQLScriptParser.parseScript(
            parserContext.getDataSource(),
            parserContext.getDialect(),
            parserContext.getPreferenceStore(),
            document.get()
        );
        for (var item : scriptItems) {
            var model = SQLQueryModelRecognizer.recognizeQuery(
                new SQLQueryRecognitionContext(
                    monitor,
                    request.getContext().getExecutionContext(),
                    true,
                    DBWorkbench.getPlatform().getPreferenceStore().getBoolean(SQLModelPreferences.VALIDATE_FUNCTIONS),
                    request.getContext().getSyntaxManager(),
                    request.getContext().getDataSource().getSQLDialect()
                ),
                item.getOriginalText()
            );
            syntaxContext.registerScriptItemContext(
                item.getOriginalText(),
                model,
                item.getOffset(),
                item.getLength(),
                true
            );
        }

        SQLScriptItemAtOffset scriptItem = syntaxContext.findScriptItem(position);
        if (scriptItem != null) {
            scriptItem.item.setHasContextBoundaryAtLength(false);
            return SQLQueryCompletionContext.prepareCompletionContext(
                scriptItem,
                position,
                request.getContext().getExecutionContext(),
                request.getContext().getDataSource().getSQLDialect()
            );
        } else {
            return SQLQueryCompletionContext.prepareOffquery(0, position);
        }
    }

    // FIXME: Mostly copy-pasted from WebServiceSQL.getCompletionProposals
    @NotNull
    public static List<CompletionItem> createCompletionsList(
        @NotNull ContextAwareDocument document,
        int offset,
        @NotNull SQLCompletionContext completionContext
    ) throws InterruptedException, InvocationTargetException, DBException {
        Document doc = new Document(document.getText());
        SQLParserContext parserContext = new SQLParserContext(
            document.getDataSource(),
            completionContext.getSyntaxManager(),
            completionContext.getRuleManager(),
            doc
        );
        SQLScriptElement activeQuery = SQLScriptParser.extractActiveQuery(parserContext, offset, 0);

        SQLCompletionRequest request = new SQLCompletionRequest(
            completionContext,
            doc,
            offset,
            activeQuery,
            false
        );

        List<CompletionProposalBase> proposals = new ArrayList<>();
        boolean useDefaultCompletionEngine = DEFAULT_ENGINE_COMPLETION.equalsIgnoreCase(DBWorkbench.getPlatform().getPreferenceStore()
            .getString(SQLModelPreferences.AUTOCOMPLETION_MODE));
        if (!useDefaultCompletionEngine) {
            SQLQueryCompletionAnalyzer analyzer = new SQLQueryCompletionAnalyzer(
                m -> LspSQLCompletionContextParser.obtainCompletionContext(
                    new VoidProgressMonitor(),
                    document.getText(),
                    offset,
                    request
                ),
                request,
                request::getDocumentOffset
            );
            analyzer.run(new VoidProgressMonitor());
            proposals.addAll(analyzer.getResult());
        } else {
            SQLCompletionAnalyzer analyzer = new SQLCompletionAnalyzer(request);
            analyzer.setCheckNavigatorNodes(false);
            analyzer.runAnalyzer(new VoidProgressMonitor());
            proposals.addAll(analyzer.getProposals());
        }
        int maxResults = 200;
        if (proposals.size() > maxResults) {
            proposals = proposals.subList(0, maxResults);
        }

        return proposals.stream()
            .map(p -> new CompletionItem(p.getReplacementString()))
            .toList();
    }
}
