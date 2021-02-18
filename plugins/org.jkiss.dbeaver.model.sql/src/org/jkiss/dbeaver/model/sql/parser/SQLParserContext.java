/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.sql.parser;

import org.eclipse.jface.text.IDocument;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.text.parser.TPRuleBasedScanner;
import org.jkiss.dbeaver.runtime.DBWorkbench;

/**
 * Parser context
 */
public class SQLParserContext {

    @NotNull
    private final DBPContextProvider contextProvider;
    @NotNull
    private final SQLSyntaxManager syntaxManager;
    @NotNull
    private final SQLRuleManager ruleManager;
    @NotNull
    private final IDocument document;
    private TPRuleBasedScanner scanner;

    public SQLParserContext(@NotNull DBPContextProvider contextProvider, @NotNull SQLSyntaxManager syntaxManager, @NotNull SQLRuleManager ruleManager, @NotNull IDocument document) {
        this.contextProvider = contextProvider;
        this.syntaxManager = syntaxManager;
        this.ruleManager = ruleManager;
        this.document = document;
    }

    @Nullable
    public DBPDataSource getDataSource() {
        DBCExecutionContext executionContext = contextProvider.getExecutionContext();
        return executionContext == null ? null : executionContext.getDataSource();
    }

    @NotNull
    public SQLSyntaxManager getSyntaxManager() {
        return syntaxManager;
    }

    @NotNull
    public SQLRuleManager getRuleManager() {
        return ruleManager;
    }

    @NotNull
    public IDocument getDocument() {
        return document;
    }

    public SQLDialect getDialect() {
        return SQLUtils.getDialectFromDataSource(getDataSource());
    }

    public TPRuleBasedScanner getScanner() {
        if (scanner == null) {
            scanner = new TPRuleBasedScanner();
            scanner.setRules(ruleManager.getAllRules());
        }
        return scanner;
    }

    public DBPPreferenceStore getPreferenceStore() {
        DBPDataSource dataSource = getDataSource();
        return dataSource == null ?
            DBWorkbench.getPlatform().getPreferenceStore() :
            dataSource.getContainer().getPreferenceStore();
    }

    void startScriptEvaluation() {
        getScanner().startEval();
    }

    void endScriptEvaluation() {
        getScanner().endEval();
    }

}
