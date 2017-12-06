/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.core;

/**
 * Core module commands (some).
 */
public interface CoreCommands
{
    String CMD_COPY_SPECIAL = "org.jkiss.dbeaver.core.edit.copy.special"; //$NON-NLS-1$
    String CMD_PASTE_SPECIAL = "org.jkiss.dbeaver.core.edit.paste.special"; //$NON-NLS-1$

    String CMD_OBJECT_OPEN = "org.jkiss.dbeaver.core.object.open"; //$NON-NLS-1$
    String CMD_OBJECT_CREATE = "org.jkiss.dbeaver.core.object.create"; //$NON-NLS-1$
    String CMD_OBJECT_DELETE = "org.jkiss.dbeaver.core.object.delete"; //$NON-NLS-1$
    String CMD_OBJECT_MOVE_UP = "org.jkiss.dbeaver.core.object.move.up"; //$NON-NLS-1$
    String CMD_OBJECT_MOVE_DOWN = "org.jkiss.dbeaver.core.object.move.down"; //$NON-NLS-1$

    String GROUP_TOOLS = "tools";

    String CMD_CONNECT = "org.jkiss.dbeaver.core.connect";
    String CMD_DISCONNECT = "org.jkiss.dbeaver.core.disconnect";
    String CMD_DISCONNECT_ALL = "org.jkiss.dbeaver.core.disconnectAll";
    String CMD_INVALIDATE = "org.jkiss.dbeaver.core.invalidate";
    String CMD_COMMIT = "org.jkiss.dbeaver.core.commit";
    String CMD_ROLLBACK = "org.jkiss.dbeaver.core.rollback";

    String CMD_EXECUTE_STATEMENT = "org.jkiss.dbeaver.ui.editors.sql.run.statement"; //$NON-NLS-1$
    String CMD_EXECUTE_STATEMENT_NEW = "org.jkiss.dbeaver.ui.editors.sql.run.statementNew"; //$NON-NLS-1$
    String CMD_EXECUTE_SCRIPT = "org.jkiss.dbeaver.ui.editors.sql.run.script"; //$NON-NLS-1$
    String CMD_EXECUTE_SCRIPT_NEW = "org.jkiss.dbeaver.ui.editors.sql.run.scriptNew"; //$NON-NLS-1$
    String CMD_EXECUTE_ROW_COUNT = "org.jkiss.dbeaver.ui.editors.sql.run.count"; //$NON-NLS-1$
    String CMD_EXECUTE_EXPRESSION = "org.jkiss.dbeaver.ui.editors.sql.run.expression"; //$NON-NLS-1$
    String CMD_EXECUTE_ALL_ROWS = "org.jkiss.dbeaver.ui.editors.sql.run.all.rows"; //$NON-NLS-1$

    String CMD_EXPLAIN_PLAN = "org.jkiss.dbeaver.ui.editors.sql.run.explain"; //$NON-NLS-1$
    String CMD_OPEN_FILE = "org.jkiss.dbeaver.ui.editors.sql.open.file"; //$NON-NLS-1$
    String CMD_SAVE_FILE = "org.jkiss.dbeaver.ui.editors.sql.save.file"; //$NON-NLS-1$
    String CMD_TOGGLE_AUTOCOMMIT = "org.jkiss.dbeaver.core.txn.autocommit"; //$NON-NLS-1$

    String CMD_CONTENT_FORMAT = "org.jkiss.dbeaver.ui.editors.text.content.format"; //$NON-NLS-1$

    String CMD_SQL_EDITOR_OPEN = "org.jkiss.dbeaver.core.sql.editor.open";
    String CMD_SQL_EDITOR_NEW = "org.jkiss.dbeaver.core.sql.editor.create";
    String CMD_SQL_EDITOR_RECENT = "org.jkiss.dbeaver.core.sql.editor.recent";

    String CMD_SQL_QUERY_NEXT = "org.jkiss.dbeaver.ui.editors.sql.query.next";
    String CMD_SQL_QUERY_PREV = "org.jkiss.dbeaver.ui.editors.sql.query.prev";
    String CMD_SQL_SWITCH_PANEL = "org.jkiss.dbeaver.ui.editors.sql.switch.panel";
    String CMD_SQL_SHOW_OUTPUT = "org.jkiss.dbeaver.ui.editors.sql.show.output";
    String CMD_SQL_SHOW_LOG = "org.jkiss.dbeaver.ui.editors.sql.show.log";
    String CMD_SQL_EDITOR_MAXIMIZE_PANEL = "org.jkiss.dbeaver.ui.editors.sql.maximize.result.panel";
    String CMD_SQL_EDITOR_CLOSE_TAB = "org.jkiss.dbeaver.ui.editors.sql.close.tab";

    String CMD_SQL_ASSIST_TEMPLATES = "org.jkiss.dbeaver.ui.editors.sql.assist.templates"; //$NON-NLS-1$

    String CMD_LINK_EDITOR = "org.jkiss.dbeaver.core.navigator.linkeditor";
    String CMD_SYNC_CONNECTION = "org.jkiss.dbeaver.ui.editors.sql.sync.connection";

    String CMD_SQL_RENAME = "org.jkiss.dbeaver.ui.editors.sql.rename";
    String CMD_OBJECT_SET_ACTIVE = "org.jkiss.dbeaver.core.navigator.set.active";
}
