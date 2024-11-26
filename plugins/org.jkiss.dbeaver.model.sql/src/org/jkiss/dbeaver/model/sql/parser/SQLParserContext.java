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

package org.jkiss.dbeaver.model.sql.parser;

import org.eclipse.jface.text.IDocument;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.text.parser.TPRuleBasedScanner;
import org.jkiss.dbeaver.runtime.DBWorkbench;

/**
 * Parser context
 */
public class SQLParserContext {

    private static final Log log = Log.getLog(SQLParserContext.class);

    @Nullable
    private final DBPDataSourceContainer dataSourceContainer;

    @Nullable
    private final DBPDataSource dataSource;

    @NotNull
    private final SQLSyntaxManager syntaxManager;
    @NotNull
    private final SQLRuleManager ruleManager;
    @NotNull
    private final IDocument document;
    private TPRuleBasedScanner scanner;

    @Nullable
    private DBPPreferenceStore preferenceStore;

    @Nullable
    private SQLDialect cachedSqlDialect = null;

    public SQLParserContext(@Nullable DBPDataSource dataSource, @NotNull SQLSyntaxManager syntaxManager, @NotNull SQLRuleManager ruleManager, @NotNull IDocument document) {
        this.dataSource = dataSource;
        if (dataSource != null) {
            this.dataSourceContainer = dataSource.getContainer();
        } else {
            this.dataSourceContainer = null;
        }
        this.syntaxManager = syntaxManager;
        this.ruleManager = ruleManager;
        this.document = document;
    }

    public SQLParserContext(@Nullable DBPDataSourceContainer dataSourceContainer, @NotNull SQLSyntaxManager syntaxManager, @NotNull SQLRuleManager ruleManager, @NotNull IDocument document) {
        this.dataSourceContainer = dataSourceContainer;
        if (dataSourceContainer != null) {
            this.dataSource = dataSourceContainer.getDataSource();
        } else {
            this.dataSource = null;
        }
        this.syntaxManager = syntaxManager;
        this.ruleManager = ruleManager;
        this.document = document;
    }


    @Nullable
    public DBPDataSource getDataSource() {
        return dataSource;
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
        if (dataSource != null) {
            return dataSource.getSQLDialect();
        }
        if (dataSourceContainer != null) {
            try {
                DBPDataSource dataSource = dataSourceContainer.getDataSource();
                if (dataSource != null) {
                    return dataSource.getSQLDialect();
                }
                if (cachedSqlDialect == null) {
                    cachedSqlDialect = dataSourceContainer.getScriptDialect().createInstance();
                }
                return cachedSqlDialect;
            } catch (Exception e) {
                log.warn(String.format("Can't get dialect from dataSourceContainerId: %s", dataSourceContainer.getId()));
            }
        }
        return BasicSQLDialect.INSTANCE;
    }

    public TPRuleBasedScanner getScanner() {
        if (scanner == null) {
            scanner = new TPRuleBasedScanner();
            scanner.setRules(ruleManager.getAllRules());
        }
        return scanner;
    }

    public DBPPreferenceStore getPreferenceStore() {
        if (preferenceStore != null) {
            return preferenceStore;
        }
        DBPDataSource dataSource = getDataSource();
        return dataSource == null ?
            DBWorkbench.getPlatform().getPreferenceStore() :
            dataSource.getContainer().getPreferenceStore();
    }

    public void setPreferenceStore(@Nullable DBPPreferenceStore preferenceStore) {
        this.preferenceStore = preferenceStore;
    }

    void startScriptEvaluation() {
        getScanner().startEval();
    }

    void endScriptEvaluation() {
        getScanner().endEval();
    }

}
