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
package org.jkiss.dbeaver.ui.ai.chat;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.AIChatAnnotation;
import org.jkiss.dbeaver.model.ai.AIChatConversation;
import org.jkiss.dbeaver.model.ai.AIContextSettings;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQueryListener;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.SqlJobResult;
import org.jkiss.dbeaver.model.sql.parser.SQLParserContext;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.registry.SQLPresentationDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class AIChatControllerBase implements AIChatController {
    private static final Log log = Log.getLog(AIChatControllerBase.class);

    @Override
    public void executeInEditor(@NotNull String text) {
        this.findOrOpenEditor(editor -> UIUtils.asyncExec(() -> {
            try {
                UIUtils.runWithMonitor(monitor -> {
                    DBPDataSourceContainer container = editor.getDataSourceContainer();
                    if (container == null) {
                        throw new DBException("No database connection is associated with the editor");
                    }

                    boolean dsConnected = DBUtils.initDataSource(
                        monitor, container, e ->
                            executeInEditor(monitor, editor, container, text)
                    );
                    if (!dsConnected && !CommonUtils.isEmpty(container.getConnectionError())) {
                        throw new DBException("Error connecting to '" + container.getName() + "':\n" + container.getConnectionError());
                    }
                    return dsConnected;
                });
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Error executing query in SQL editor", null, e);
            }
        }));
    }

    private void executeInEditor(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SQLEditor editor,
        @NotNull DBPDataSourceContainer container,
        @NotNull String text
    ) {
        Document document = new Document(text);
        SQLRuleManager ruleManager = editor.getRuleManager();
        SQLSyntaxManager syntaxManager = editor.getSyntaxManager();
        SQLParserContext parserContext = new SQLParserContext(container, syntaxManager, ruleManager, document);
        List<SQLScriptElement> scriptElements = SQLScriptParser.extractScriptQueries(
            parserContext, 0, text.length(), true, false, true
        );

        DBPDataSource dataSource = this.getActiveDataSource();
        if (dataSource == null) {
            return;
        }
        if (!AIUtils.confirmExecutionIfNeeded(dataSource, scriptElements, false)) {
            return;
        }
        try {
            AIUtils.disableAutoCommitIfNeeded(
                monitor,
                scriptElements,
                AbstractDataSourceHandler.getExecutionContextFromPart(editor)
            );
        } catch (DBException e) {
            log.error("Error when trying to disable auto-commit:", e);
            return;
        }


        boolean oldShowScriptRulerOnExecution = editor.getShowScriptRulerOnExecution();
        editor.setShowScriptRulerOnExecution(false);
        SQLQueryListener queryListener = new SQLQueryListener() {
            @Override
            public void onEndSqlJob(@NotNull DBCSession session, @NotNull SqlJobResult result) {
                UIUtils.asyncExec(() ->
                    editor.setShowScriptRulerOnExecution(oldShowScriptRulerOnExecution));
            }
        };
        UIUtils.syncExec(() -> editor.processQueries(
            scriptElements,
            true,
            false,
            false,
            true,
            queryListener,
            null
        ));
    }

    @Override
    public void openInEditor(@NotNull String text, @Nullable AIChatConversation conversation) {
        this.findOrOpenEditor(editor -> {
            ISelection selection = editor.getSelectionProvider().getSelection();
            IDocument document = editor.getDocument();
            if (document != null && selection instanceof TextSelection textSelection) {
                try {
                    int offset = textSelection.getOffset();
                    int length = textSelection.getLength();
                    SQLScriptElement query = editor.extractQueryAtPos(offset);
                    if (query != null) {
                        offset = query.getOffset();
                        length = query.getLength();
                    }
                    document.replace(offset, length, text);
                    editor.getSelectionProvider().setSelection(new TextSelection(offset + text.length(), 0));
                    editor.showExtraPresentation((SQLPresentationDescriptor) null);

                    if (conversation != null) {
                        IAnnotationModel annotationModel = Objects.requireNonNull(editor.getAnnotationModel());
                        annotationModel.addAnnotation(
                            new AIChatAnnotation(conversation.getId()),
                            new Position(offset, text.length())
                        );
                    }
                } catch (BadLocationException ex) {
                    DBWorkbench.getPlatformUI().showError(
                        "Insert SQL",
                        "Error inserting SQL completion in text editor",
                        ex
                    );
                }
            }
        });
    }

    private void findOrOpenEditor(@NotNull Consumer<SQLEditor> consumer) {
        AIContextSettings settings = getContextSettings();
        if (settings == null) {
            DBWorkbench.getPlatformUI().showError("Can't open editor", "Please set the active connection");
            return;
        }
        IWorkbenchWindow window = UIUtils.getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        if (page == null) {
            return;
        }
        DBPDataSourceContainer container = settings.getDataSourceContainer();
        if (container == null) {
            return;
        }
        IEditorPart activeEditor = page.getActiveEditor();
        if (activateEditor(consumer, activeEditor, container, page)) {
            return;
        }
        for (IEditorReference reference : page.getEditorReferences()) {
            IEditorPart editor = reference.getEditor(false);
            if (activateEditor(consumer, editor, container, page)) {
                return;
            }
        }

        SQLEditorUtils.openNewSqlConsoleAndTryConnect(container, editorOrNull -> {
            if (editorOrNull != null) {
                consumer.accept(editorOrNull);
            }
        });
    }

    private static boolean activateEditor(
        @NotNull Consumer<SQLEditor> consumer,
        @Nullable IEditorPart editor,
        @Nullable DBPDataSourceContainer container,
        @NotNull IWorkbenchPage page
    ) {
        if (editor instanceof SQLEditor sqlEditor && sqlEditor.getDataSourceContainer() == container) {
            page.activate(editor);
            consumer.accept(sqlEditor);
            return true;
        }
        return false;
    }

    @Nullable
    private DBPDataSource getActiveDataSource() {
        AIContextSettings settings = getContextSettings();
        if (settings == null || settings.getDataSourceContainer() == null) {
            return null;
        }
        return settings.getDataSourceContainer().getDataSource();
    }

}
