/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.sql.translate;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL translator
 */
public final class SQLQueryTranslator {

    public static String translateScript(
        @NotNull SQLDialect sourceDialect,
        @NotNull SQLDialect targetDialect,
        @NotNull DBPPreferenceStore preferenceStore,
        @NotNull String script) throws DBException
    {
        SQLTranslateContext context = new SQLTranslateContext(sourceDialect, targetDialect, preferenceStore);

        List<SQLScriptElement> sqlScriptElements = SQLScriptParser.parseScript(sourceDialect, preferenceStore, script);
        List<SQLScriptElement> result = new ArrayList<>();
        for (SQLScriptElement element : sqlScriptElements) {
            result.addAll(
                context.translateCommand(element));
        }
        String scriptDelimiter = targetDialect.getScriptDelimiters()[0];

        StringBuilder sql = new StringBuilder();
        for (SQLScriptElement element : result) {
            sql.append(element.getText());
            sql.append(scriptDelimiter).append("\n");
        }
        return sql.toString();
    }


}
