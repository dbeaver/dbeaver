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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.impl.DataSourceContextProvider;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorParametersProvider;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.StringWriter;

public class SQLEditorHandlerExportData extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
        if (editor != null) {
            editor.exportDataFromQuery(new ExportDataSQLScriptContext(editor));
        }
        return null;
    }

    private static class ExportDataSQLScriptContext extends SQLScriptContext {
        public ExportDataSQLScriptContext(SQLEditor editor) {
            super(null, new DataSourceContextProvider(editor.getDataSource()), null, new StringWriter(), new SQLEditorParametersProvider(editor.getSite()));
        }

        @Override
        public boolean hasVariable(String name) {
            return super.hasVariable(name) || super.getParameterDefaultValue(name) != null;
        }

        @Override
        public Object getVariable(String name) {
            if (super.hasVariable(name)) {
                return super.getVariable(name);
            }
            return super.getParameterDefaultValue(name);
        }
    }
}