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

package org.jkiss.dbeaver;

/**
 * Preferences constants
 */
public final class DBeaverPreferences
{
    public static final String AGENT_ENABLED = "agent.enabled"; //$NON-NLS-1$
    public static final String AGENT_LONG_OPERATION_NOTIFY = "agent.long.operation.notify"; //$NON-NLS-1$
    public static final String AGENT_LONG_OPERATION_TIMEOUT = "agent.long.operation.timeout"; //$NON-NLS-1$

    public static final String PLATFORM_LANGUAGE = "platform.language"; //$NON-NLS-1$

    public static final String NAVIGATOR_EXPAND_ON_CONNECT = "navigator.expand.on.connect"; //$NON-NLS-1$
    public static final String NAVIGATOR_SORT_ALPHABETICALLY = "navigator.sort.case.insensitive"; //$NON-NLS-1$
    public static final String NAVIGATOR_SORT_FOLDERS_FIRST = "navigator.sort.forlers.first"; //$NON-NLS-1$
    public static final String NAVIGATOR_SYNC_EDITOR_DATASOURCE = "navigator.sync.editor.datasource"; //$NON-NLS-1$
    public static final String NAVIGATOR_REFRESH_EDITORS_ON_OPEN = "navigator.refresh.editor.open"; //$NON-NLS-1$
    public static final String NAVIGATOR_GROUP_BY_DRIVER = "navigator.group.by.driver"; //$NON-NLS-1$
    public static final String NAVIGATOR_EDITOR_FULL_NAME = "navigator.editor.full-name"; //$NON-NLS-1$
    public static final String NAVIGATOR_OBJECT_DOUBLE_CLICK = "navigator.object.doubleClick"; //$NON-NLS-1$
    public static final String NAVIGATOR_CONNECTION_DOUBLE_CLICK = "navigator.connection.doubleClick"; //$NON-NLS-1$
    public static final String NAVIGATOR_SHOW_SQL_PREVIEW = "navigator.editor.show.preview"; //$NON-NLS-1$
    public static final String NAVIGATOR_SHOW_OBJECT_TIPS = "navigator.show.objects.tips"; //$NON-NLS-1$

    public static final String ENTITY_EDITOR_DETACH_INFO = "entity.editor.info.detach"; //$NON-NLS-1$
    public static final String ENTITY_EDITOR_INFO_SASH_STATE = "entity.editor.info.sash.state"; //$NON-NLS-1$

    public static final String KEEP_STATEMENT_OPEN = "keep.statement.open"; //$NON-NLS-1$

    public static final String SCRIPT_COMMIT_TYPE = "script.commit.type"; //$NON-NLS-1$
    public static final String SCRIPT_COMMIT_LINES = "script.commit.lines"; //$NON-NLS-1$
    public static final String SCRIPT_ERROR_HANDLING = "script.error.handling"; //$NON-NLS-1$
    public static final String SCRIPT_FETCH_RESULT_SETS = "script.fetch.resultset"; //$NON-NLS-1$

    public static final String SCRIPT_DELETE_EMPTY = "script.delete.empty"; //$NON-NLS-1$
    public static final String SCRIPT_AUTO_FOLDERS = "script.auto.folders"; //$NON-NLS-1$
    public static final String SCRIPT_CREATE_CONNECTION_FOLDERS = "script.auto.connection.folders"; //$NON-NLS-1$
    public static final String SCRIPT_TITLE_PATTERN = "script.title.pattern"; //$NON-NLS-1$

    public static final String STATEMENT_INVALIDATE_BEFORE_EXECUTE = "statement.invalidate.before.execute"; //$NON-NLS-1$
    public static final String STATEMENT_TIMEOUT = "statement.timeout"; //$NON-NLS-1$
    public static final String MEMORY_CONTENT_MAX_SIZE = "content.memory.maxsize"; //$NON-NLS-1$
    public static final String READ_EXPENSIVE_PROPERTIES = "database.props.expensive"; //$NON-NLS-1$
    public static final String EDITOR_SEPARATE_CONNECTION = "database.editor.separate.connection"; //$NON-NLS-1$
    public static final String EDITOR_CONNECT_ON_ACTIVATE = "database.editor.connect.on.activate"; //$NON-NLS-1$
    public static final String EDITOR_CONNECT_ON_EXECUTE = "database.editor.connect.on.execute"; //$NON-NLS-1$

    public static final String TEXT_EDIT_UNDO_LEVEL = "text.edit.undo.level"; //$NON-NLS-1$

    public static final String CONFIRM_EXIT = "exit"; //$NON-NLS-1$
    public static final String CONFIRM_ORDER_RESULTSET = "order_resultset"; //$NON-NLS-1$
    public static final String CONFIRM_RS_FETCH_ALL = "fetch_all_rows"; //$NON-NLS-1$
    public static final String CONFIRM_RS_EDIT_CLOSE = "close_resultset_edit"; //$NON-NLS-1$
    public static final String CONFIRM_TXN_DISCONNECT = "disconnect_txn"; //$NON-NLS-1$
    public static final String CONFIRM_ENTITY_EDIT_CLOSE = "close_entity_edit"; //$NON-NLS-1$
    public static final String CONFIRM_ENTITY_DELETE = "entity_delete"; //$NON-NLS-1$
    public static final String CONFIRM_LOCAL_FOLDER_DELETE = "local_folder_delete"; //$NON-NLS-1$
    public static final String CONFIRM_ENTITY_REJECT = "entity_reject"; //$NON-NLS-1$
    public static final String CONFIRM_ENTITY_REVERT = "entity_revert"; //$NON-NLS-1$
    //public static final String CONFIRM_ENTITY_RENAME = "entity_rename"; //$NON-NLS-1$
    public static final String CONFIRM_EDITOR_CLOSE = "close_editor_edit"; //$NON-NLS-1$
    public static final String CONFIRM_DRIVER_DOWNLOAD = "driver_download"; //$NON-NLS-1$
    public static final String CONFIRM_MANUAL_DOWNLOAD = "driver_download_manual"; //$NON-NLS-1$
    public static final String CONFIRM_VERSION_CHECK = "version_check"; //$NON-NLS-1$
    public static final String CONFIRM_KEEP_STATEMENT_OPEN = "keep_statement_open"; //$NON-NLS-1$
    public static final String CONFIRM_DANGER_SQL = "dangerous_sql"; //$NON-NLS-1$

    private static final String PROPERTY_USE_ALL_COLUMNS_QUIET = "virtual-key-quiet";

    // Hex editor font identifiers
    public static final String HEX_FONT_NAME = "hex.font.name"; //$NON-NLS-1$
    public static final String HEX_FONT_SIZE = "hex.font.size"; //$NON-NLS-1$
    public static final String HEX_FONT_STYLE = "hex.font.style"; //$NON-NLS-1$

    // General UI
    public static final String UI_AUTO_UPDATE_CHECK = "ui.auto.update.check"; //$NON-NLS-1$
    public static final String UI_UPDATE_CHECK_TIME = "ui.auto.update.check.time"; //$NON-NLS-1$
    public static final String UI_KEEP_DATABASE_EDITORS = "ui.editors.reopen-after-restart"; //$NON-NLS-1$

    public static final String UI_DRIVERS_VERSION_UPDATE = "ui.drivers.version.update"; //$NON-NLS-1$
    public static final String UI_DRIVERS_HOME = "ui.drivers.home"; //$NON-NLS-1$

    public static final String UI_PROXY_HOST = "ui.proxy.host"; //$NON-NLS-1$
    public static final String UI_PROXY_PORT = "ui.proxy.port"; //$NON-NLS-1$
    public static final String UI_PROXY_USER = "ui.proxy.user"; //$NON-NLS-1$
    public static final String UI_PROXY_PASSWORD = "ui.proxy.password"; //$NON-NLS-1$

    public static final String UI_DRIVERS_SOURCES = "ui.drivers.sources"; //$NON-NLS-1$
    public static final String UI_MAVEN_REPOSITORIES = "ui.maven.repositories"; //$NON-NLS-1$

    // Resources
    public static final String RESOURCE_HANDLER_ROOT_PREFIX = "resource.root."; //$NON-NLS-1$

    // ResultSet
    public static final String RS_EDIT_MAX_TEXT_SIZE = "resultset.edit.maxtextsize"; //$NON-NLS-1$
    public static final String RS_EDIT_LONG_AS_LOB = "resultset.edit.longaslob"; //$NON-NLS-1$
    public static final String RS_EDIT_USE_ALL_COLUMNS = "resultset.edit.key.use_all_columns";
    public static final String RS_EDIT_AUTO_UPDATE_VALUE = "resultset.edit.value.autoupdate"; //$NON-NLS-1$
    public static final String RS_COMMIT_ON_EDIT_APPLY = "resultset.commit.oneditapply"; //$NON-NLS-1$
    public static final String RS_COMMIT_ON_CONTENT_APPLY = "resultset.commit.oncontentapply"; //$NON-NLS-1$
    public static final String RS_EDIT_NEW_ROWS_AFTER = "resultset.edit.new.row.after";
    public static final String RS_EDIT_REFRESH_AFTER_UPDATE = "resultset.edit.refreshAfterUpdate"; //$NON-NLS-1$

    public static final String RESULT_SET_AUTO_FETCH_NEXT_SEGMENT = "resultset.autofetch.next.segment"; //$NON-NLS-1$
    public static final String RESULT_SET_READ_METADATA = "resultset.read.metadata"; //$NON-NLS-1$
    public static final String RESULT_SET_READ_REFERENCES = "resultset.read.references"; //$NON-NLS-1$
    public static final String RESULT_SET_MAX_ROWS = "resultset.maxrows"; //$NON-NLS-1$
    public static final String RESULT_SET_CANCEL_TIMEOUT = "resultset.cancel.timeout"; //$NON-NLS-1$
    public static final String RESULT_SET_BINARY_EDITOR_TYPE = "resultset.binary.editor"; //$NON-NLS-1$
    public static final String RESULT_SET_ORDER_SERVER_SIDE = "resultset.order.serverSide"; //$NON-NLS-1$
    public static final String RESULT_SET_SHOW_ODD_ROWS = "resultset.show.oddRows"; //$NON-NLS-1$
    public static final String RESULT_SET_SHOW_CELL_ICONS = "resultset.show.cellIcons"; //$NON-NLS-1$
    public static final String RESULT_SET_SHOW_DESCRIPTION = "resultset.show.columnDescription"; //$NON-NLS-1$
    public static final String RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES = "resultset.calc.columnWidthByValues"; //$NON-NLS-1$
    public static final String RESULT_SET_SHOW_CONNECTION_NAME = "resultset.show.connectionName"; //$NON-NLS-1$
    public static final String RESULT_SET_COLORIZE_DATA_TYPES = "resultset.show.colorizeDataTypes"; //$NON-NLS-1$
    public static final String RESULT_SET_RIGHT_JUSTIFY_NUMBERS = "resultset.show.rightJustifyNumbers"; //$NON-NLS-1$
    public static final String RESULT_SET_AUTO_SWITCH_MODE = "resultset.behavior.autoSwitchMode"; //$NON-NLS-1$
    public static final String RESULT_SET_DOUBLE_CLICK = "resultset.behavior.doubleClick"; //$NON-NLS-1$
    public static final String RESULT_SET_ROW_BATCH_SIZE = "resultset.show.row.batch.size"; //$NON-NLS-1$
    public static final String RESULT_SET_PRESENTATION = "resultset.presentation.active"; //$NON-NLS-1$
    public static final String RESULT_SET_STRING_USE_CONTENT_EDITOR = "resultset.string.use.content.editor"; //$NON-NLS-1$

    public static final String RESULT_TEXT_MAX_COLUMN_SIZE = "resultset.text.max.column.size"; //$NON-NLS-1$
    public static final String RESULT_TEXT_VALUE_FORMAT = "resultset.text.value.format"; //$NON-NLS-1$
    public static final String RESULT_TEXT_SHOW_NULLS = "resultset.text.show.nulls"; //$NON-NLS-1$
    public static final String RESULT_TEXT_DELIMITER_LEADING = "resultset.text.delimiter.leading"; //$NON-NLS-1$
    public static final String RESULT_TEXT_DELIMITER_TRAILING = "resultset.text.delimiter.trailing"; //$NON-NLS-1$

    //public static final String DEFAULT_RESOURCE_ENCODING = "resource.encoding.default";

    public static final String LOGS_DEBUG_ENABLED = "logs.debug.enabled";
    public static final String LOGS_DEBUG_LOCATION = "logs.debug.location";
    
    public static final String HEX_DEF_WIDTH ="default.hex.width";

}
