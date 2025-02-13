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

package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;

import java.util.Map;

/**
 * SQL control command
 */
public class SQLControlCommand implements SQLScriptElement {

    private final DBPDataSource dataSource;
    private final String text;
    private final String command;
    private final String commandId;
    private final String parameter;
    private final int offset;
    private final int length;
    private Object data;
    private boolean emptyCommand;
    private final Map<String, String> parameters;

    public SQLControlCommand(
        @Nullable DBPDataSource dataSource,
        @NotNull String text,
        @Nullable String commandId,
        int offset,
        int length,
        @NotNull Map<String, String> parameters
    ) {
        this.dataSource = dataSource;
        this.text = text;
        this.command = commandId;
        this.commandId = commandId;
        this.parameter = null;
        this.offset = offset;
        this.length = length;
        this.parameters = parameters;
    }

    public SQLControlCommand(DBPDataSource dataSource, SQLSyntaxManager syntaxManager, String text, String commandId, int offset, int length, boolean emptyCommand) {
        this.dataSource = dataSource;

        this.text = text;
        final String multilineCommandPrefix = syntaxManager.getControlCommandPrefix().repeat(2);
        if (text.startsWith(multilineCommandPrefix)) {
            text = text.substring(multilineCommandPrefix.length(), text.length() - multilineCommandPrefix.length());
        } else if (text.startsWith(syntaxManager.getControlCommandPrefix())) {
            text = text.substring(syntaxManager.getControlCommandPrefix().length());
        }
        int divPos = -1;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                divPos = i;
                break;
            }
        }

        this.command = divPos == -1 ? text : text.substring(0, divPos);
        this.parameter = divPos == -1 ? null : text.substring(divPos + 1).trim();
        this.offset = offset;
        this.length = length;
        this.emptyCommand = emptyCommand;

        this.commandId = commandId == null ? command : commandId;
        this.parameters = null;
    }

    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSource == null ? null : dataSource.getContainer();
    }

    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    public String getOriginalText() {
        return command;
    }

    @NotNull
    @Override
    public String getText() {
        return text;
    }

    public String getCommand() {
        return command;
    }

    public String getCommandId() {
        return commandId;
    }

    public String getParameter() {
        return parameter;
    }

    public Map<String, String> getParameters() {
        return this.parameters;
    }

    public boolean isEmptyCommand() {
        return emptyCommand;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public void reset() {

    }

    @Override
    public String toString() {
        return text;
    }

}
