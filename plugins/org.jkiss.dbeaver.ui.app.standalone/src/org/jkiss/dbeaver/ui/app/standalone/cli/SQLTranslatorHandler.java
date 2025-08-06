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
package org.jkiss.dbeaver.ui.app.standalone.cli;

import org.apache.commons.cli.CommandLine;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLModelPreferences;
import org.jkiss.dbeaver.model.sql.translate.SQLQueryTranslator;
import org.jkiss.dbeaver.ui.app.standalone.ICommandLineParameterHandler;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.FileReader;
import java.io.IOException;

public class SQLTranslatorHandler implements ICommandLineParameterHandler {
    private static final Log log = Log.getLog(SQLTranslatorHandler.class);

    @Override
    public void handleParameter(CommandLine commandLine, String name, String value) {
        String[] args = value.split(",");
        if (args.length != 2) {
            throw new IllegalStateException("Input parameter format: dialect,<input-file-path>");
        }
        String dialect = args[0];
        String inputFile = args[1];
        if (CommonUtils.isEmpty(inputFile)) {
            throw new IllegalStateException("Input file not specified");
        }
        DBPPreferenceStore preferenceStore = new SimplePreferenceStore() {
            @Override
            public void save() throws IOException {

            }
        };
        preferenceStore.setValue(SQLModelPreferences.SQL_FORMAT_FORMATTER, "default");

        String script;
        try (FileReader fr = new FileReader(inputFile)) {
            script = IOUtils.readToString(fr);
        } catch (IOException e) {
            throw new RuntimeException("Error opening input file " + inputFile, e);
        }

        SQLDialect srcDialect = new BasicSQLDialect() {

        };
        SQLDialect targetDialect = new BasicSQLDialect() {

        };

        String result = null;
        try {
            result = SQLQueryTranslator.translateScript(srcDialect, targetDialect, preferenceStore, script);
        } catch (Exception e) {
            throw new RuntimeException("Error translating file " + inputFile, e);
        }

        System.out.println(result);
    }
}