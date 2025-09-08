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
package org.jkiss.dbeaver.model.sql.commands;

import com.google.gson.Gson;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;

import java.io.StringReader;
import java.util.Map;

/**
 * Control command handler
 */
public class SQLCommandExport implements SQLControlCommandHandler {

    @NotNull
    @Override
    public SQLControlResult handleCommand(@NotNull DBRProgressMonitor monitor, @NotNull SQLControlCommand command, @NotNull SQLScriptContext scriptContext) throws DBException {
        final Map<String, Object> params;

        try {
            params = JSONUtils.parseMap(new Gson(), new StringReader(command.getParameter()));
        } catch (Exception e) {
            throw new DBException("Invalid syntax. Use '@export {\"type\": <type>, \"producer\": {...}}, \"consumer\": {...}}, \"processor\": {...}}'", e);
        }

        scriptContext.setPragma(SQLPragmaHandler.PRAGMA_EXPORT, params);

        return SQLControlResult.success();
    }
}
