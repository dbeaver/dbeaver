/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.model;

import org.eclipse.core.runtime.FileLocator;
import org.jkiss.dbeaver.DBeaverConstants;
import org.jkiss.dbeaver.core.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * DBIcon
 */
public class DBIcon implements DBPImage
{
    static final Log log = Log.getLog(DBIcon.class);

    public static final DBIcon DBEAVER_LOGO = new DBIcon("dbeaver_logo", "dbeaver.png"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon GEN_DATABASE = new DBIcon("gen_database", "database.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon GEN_DATABASE_TYPE = new DBIcon("gen_database_type", "database_type.png"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon TREE = new DBIcon("tree", "tree/tree.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_ADMIN = new DBIcon("admin", "tree/admin.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_DATABASE = new DBIcon("database", "tree/database.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_DATABASE_CATEGORY = new DBIcon("database_category", "tree/database_category.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_SCHEMA = new DBIcon("schema", "tree/schema.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_TABLE = new DBIcon("table", "tree/table.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_TABLE_ALIAS = new DBIcon("table_alias", "tree/table_alias.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_TABLE_SORT = new DBIcon("table_sort", "tree/table_sort.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_VIEW = new DBIcon("view", "tree/view.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FUNCTION = new DBIcon("function", "tree/function.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_PROCEDURE = new DBIcon("procedure", "tree/procedure.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_COLUMNS = new DBIcon("columns", "tree/columns.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_COLUMN = new DBIcon("column", "tree/column.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_INDEX = new DBIcon("index", "tree/index.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_CONSTRAINT = new DBIcon("constraint", "tree/constraint.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_UNIQUE_KEY = new DBIcon("unique-key", "tree/unique_constraint.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOREIGN_KEY = new DBIcon("foreign-key", "tree/foreign_key.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOREIGN_KEY_COLUMN = new DBIcon("foreign-key-column", "tree/foreign_key_column.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_REFERENCE = new DBIcon("reference", "tree/reference.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_TRIGGER = new DBIcon("trigger", "tree/trigger.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_USER = new DBIcon("user", "tree/user.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_PAGE = new DBIcon("page", "tree/page.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOLDER = new DBIcon("folder", "tree/folder.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOLDER_LINK = new DBIcon("folder_linked", "tree/folder_link.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOLDER_DATABASE = new DBIcon("folder_database", "tree/folder_database.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOLDER_SCHEMA = new DBIcon("folder_schema", "tree/folder_schema.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOLDER_TABLE = new DBIcon("folder_table", "tree/folder_table.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOLDER_VIEW = new DBIcon("folder_view", "tree/folder_view.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOLDER_USER = new DBIcon("folder_user", "tree/folder_user.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOLDER_ADMIN = new DBIcon("folder_admin", "tree/folder_admin.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_SESSIONS = new DBIcon("sessions", "tree/sessions.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_PACKAGE = new DBIcon("package", "tree/package.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_DATA_TYPE = new DBIcon("data_type", "tree/data_type.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_SEQUENCE = new DBIcon("sequence", "tree/sequence.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_SYNONYM = new DBIcon("synonym", "tree/synonym.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_TABLESPACE = new DBIcon("tablespace", "tree/tablespace.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_PARTITION = new DBIcon("partition", "tree/partition.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_ATTRIBUTE = new DBIcon("attribute", "tree/attribute.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_ARGUMENT = new DBIcon("argument", "tree/argument.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_JAVA_CLASS = new DBIcon("javaClass", "tree/java_class.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_JAVA_INTERFACE = new DBIcon("javaInterface", "tree/java_interface.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_LINK = new DBIcon("link", "tree/link.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FILE = new DBIcon("file", "tree/file.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_CLASS = new DBIcon("class", "tree/class.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_ASSOCIATION = new DBIcon("association", "tree/association.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_SERVER = new DBIcon("server", "tree/server.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_SERVERS = new DBIcon("servers", "tree/servers.png"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon TREE_INFO = new DBIcon("info", "tree/info.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOLDER_INFO = new DBIcon("folder_info", "tree/folder_info.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_RECYCLE_BIN = new DBIcon("recycle_bin", "tree/recycle_bin.png"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon TREE_EXPAND = new DBIcon("expand", "misc/expand.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_COLLAPSE = new DBIcon("collapse", "misc/collapse.png"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon PROJECT = new DBIcon("project", "project.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon PROJECTS = new DBIcon("projects", "projects.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon CONNECTIONS = new DBIcon("connections", "connections.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon DATABASES = new DBIcon("databases", "databases.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon SCRIPTS = new DBIcon("scripts", "scripts.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon BOOKMARK_FOLDER = new DBIcon("bookmark_folder", "bookmark_folder.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon BOOKMARK = new DBIcon("bookmark", "bookmark.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon PICTURE = new DBIcon("picture", "picture.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon PICTURE_SAVE = new DBIcon("picture_save", "picture_save.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon PROPERTIES = new DBIcon("properties", "properties.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon CONFIGURATION = new DBIcon("configuration", "configuration.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon LINK = new DBIcon("link", "misc/link.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon CURSOR = new DBIcon("cursor", "misc/cursor.png"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon LOADING1 = new DBIcon("loading1", "tree/load/loading1.gif"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon LOADING2 = new DBIcon("loading2", "tree/load/loading2.gif"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon LOADING3 = new DBIcon("loading3", "tree/load/loading3.gif"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon LOADING4 = new DBIcon("loading4", "tree/load/loading4.gif"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon PROGRESS0 = new DBIcon("progress0", "misc/progress_0.gif"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon PROGRESS1 = new DBIcon("progress1", "misc/progress_1.gif"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon PROGRESS2 = new DBIcon("progress2", "misc/progress_2.gif"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon PROGRESS3 = new DBIcon("progress3", "misc/progress_3.gif"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon PROGRESS4 = new DBIcon("progress4", "misc/progress_4.gif"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon PROGRESS5 = new DBIcon("progress5", "misc/progress_5.gif"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon PROGRESS6 = new DBIcon("progress6", "misc/progress_6.gif"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon PROGRESS7 = new DBIcon("progress7", "misc/progress_7.gif"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon PROGRESS8 = new DBIcon("progress8", "misc/progress_8.gif"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon PROGRESS9 = new DBIcon("progress9", "misc/progress_9.gif"); //$NON-NLS-1$ //$NON-NLS-2$

//    public static final DBIcon RS_FIRST = new DBIcon("rs_first", "sql/resultset_first.png"); //$NON-NLS-1$ //$NON-NLS-2$
//    public static final DBIcon RS_LAST = new DBIcon("rs_last", "sql/resultset_last.png"); //$NON-NLS-1$ //$NON-NLS-2$
//    public static final DBIcon RS_NEXT = new DBIcon("rs_next", "sql/resultset_next.png"); //$NON-NLS-1$ //$NON-NLS-2$
//    public static final DBIcon RS_PREV = new DBIcon("rs_prev", "sql/resultset_previous.png"); //$NON-NLS-1$ //$NON-NLS-2$
//    public static final DBIcon RS_FETCH_PAGE = new DBIcon("resultset_fetch_page.png", "sql/resultset_fetch_page.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon RS_REFRESH = new DBIcon("rs_refresh", "sql/resultset_refresh.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon RS_GRID = new DBIcon("rs_mode_grid", "sql/grid.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon RS_DETAILS = new DBIcon("rs_details", "sql/details.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon RS_FORWARD = new DBIcon("rs_forward", "sql/forward.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon RS_BACK = new DBIcon("rs_back", "sql/back.png"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon TXN_COMMIT_AUTO = new DBIcon("txn_commit_auto", "sql/txn_auto.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TXN_COMMIT_MANUAL = new DBIcon("txn_commit_manual", "sql/txn_manual.png"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon RULER_POSITION = new DBIcon("ruler_position", "misc/ruler_position.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon FILTER_VALUE = new DBIcon("filter_value", "misc/filter_value.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon FILTER_INPUT = new DBIcon("filter_input", "misc/filter_input.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon FILTER_CLIPBOARD = new DBIcon("filter_clipboard", "misc/filter_clipboard.png"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon FIND = new DBIcon("find", "misc/find.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon FIND_TEXT = new DBIcon("find_text", "misc/find_text.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon SEARCH = new DBIcon("search", "misc/search.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon CHECK = new DBIcon("check", "misc/check.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon CHECK2 = new DBIcon("check2", "misc/check2.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon CHECK_ON = new DBIcon("checked", "misc/checked.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon CHECK_OFF = new DBIcon("unchecked", "misc/unchecked.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ZOOM = new DBIcon("zoom", "misc/zoom.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ZOOM_IN = new DBIcon("zoom_in", "misc/zoom_in.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ZOOM_OUT = new DBIcon("zoom_out", "misc/zoom_out.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ROTATE = new DBIcon("rotate", "misc/rotate.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ROTATE_LEFT = new DBIcon("rotate_left", "misc/rotate_left.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ROTATE_RIGHT = new DBIcon("rotate_right", "misc/rotate_right.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon FIT_WINDOW = new DBIcon("fit_window", "misc/fit-window.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ORIGINAL_SIZE = new DBIcon("original_size", "misc/original-size.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ASTERISK = new DBIcon("asterisk", "misc/asterisk.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon BULLET_BLACK = new DBIcon("bullet_black", "misc/bullet_black.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon BULLET_GREEN = new DBIcon("bullet_green", "misc/bullet_green.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon BULLET_RED = new DBIcon("bullet_red", "misc/bullet_red.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon BULLET_STAR = new DBIcon("bullet_star", "misc/bullet_star.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ARROW_UP = new DBIcon("arrow_up", "misc/arrow_up.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ARROW_DOWN = new DBIcon("arrow_down", "misc/arrow_down.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ARROW_LEFT = new DBIcon("arrow_left", "misc/arrow_left.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ARROW_LEFT_ALL = new DBIcon("arrow_left_all", "misc/arrow_left_all.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ARROW_RIGHT = new DBIcon("arrow_right", "misc/arrow_right.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ARROW_RIGHT_ALL = new DBIcon("arrow_right_all", "misc/arrow_right_all.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ARROW_RESET = new DBIcon("arrow_reset", "misc/arrow_reset.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon SORT_INCREASE = new DBIcon("sort_increase", "misc/sort_increase.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon SORT_DECREASE = new DBIcon("sort_decrease", "misc/sort_decrease.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon SORT_UNKNOWN = new DBIcon("sort_unknown", "misc/sort_unknown.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon FRAME_QUERY = new DBIcon("frame_query", "misc/frame_query.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon FILTER = new DBIcon("filter", "misc/filter.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon FILTER_APPLY = new DBIcon("filter_apply", "misc/filter_apply.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon FILTER_RESET = new DBIcon("filter_reset", "misc/filter_reset.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon EVENT = new DBIcon("event", "misc/event.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon HOME = new DBIcon("home", "misc/home.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon COMPILE = new DBIcon("compile", "misc/compile.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon COMPILE_LOG = new DBIcon("compile_log", "misc/compile_log.png"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon SAVE = new DBIcon("save", "file/save.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon SAVE_AS = new DBIcon("save_as", "file/save_as.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon LOAD = new DBIcon("load", "file/load.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon RESET = new DBIcon("reset", "file/reset.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon COMPARE = new DBIcon("compare", "file/compare.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ACCEPT = new DBIcon("accept", "sql/accept.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon REJECT = new DBIcon("reject", "sql/cancel.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon REVERT = new DBIcon("revert", "sql/revert.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon IMPORT = new DBIcon("import", "file/import.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon EXPORT = new DBIcon("export", "file/export.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon REFRESH = new DBIcon("refresh", "/refresh.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon CONFIRM = new DBIcon("confirm", "misc/confirm.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon CLOSE = new DBIcon("close", "misc/close.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon JAR = new DBIcon("jar", "misc/jar.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon SOURCES = new DBIcon("sources", "misc/sources.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon CANCEL = new DBIcon("cancel", "misc/cancel.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon PRINT = new DBIcon("print", "misc/print.png"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon ROW_ADD = new DBIcon("row_add", "sql/row_add.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ROW_COPY = new DBIcon("row_copy", "sql/row_copy.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ROW_EDIT = new DBIcon("row_edit", "sql/row_edit.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ROW_DELETE = new DBIcon("row_delete", "sql/row_delete.png"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon EDIT_DATABASE = new DBIcon("edit_database", "misc/edit_database.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon EDIT_TABLE = new DBIcon("edit_table", "misc/edit_table.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon EDIT_COLUMN = new DBIcon("edit_column", "misc/edit_column.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon CONFIG_TABLE = new DBIcon("edit_table", "misc/config_table.png"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon TYPE_BOOLEAN = new DBIcon("boolean", "sql/types/boolean.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_NUMBER = new DBIcon("number", "sql/types/number.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_STRING = new DBIcon("string", "sql/types/string.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_DATETIME = new DBIcon("datetime", "sql/types/datetime.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_BINARY = new DBIcon("binary", "sql/types/binary.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_TEXT = new DBIcon("text", "sql/types/text.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_XML = new DBIcon("xml", "sql/types/xml.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_LOB = new DBIcon("lob", "sql/types/lob.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_ARRAY = new DBIcon("array", "sql/types/array.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_STRUCT = new DBIcon("struct", "sql/types/struct.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_DOCUMENT = new DBIcon("document", "sql/types/document.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_OBJECT = new DBIcon("object", "sql/types/object.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_IMAGE = new DBIcon("image", "sql/types/image.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_REFERENCE = new DBIcon("reference", "sql/types/reference.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_ROWID = new DBIcon("rowid", "sql/types/rowid.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_ANY = new DBIcon("any", "sql/types/any.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_UUID = new DBIcon("uuid", "sql/types/uuid.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_UNKNOWN = new DBIcon("unknown", "sql/types/unknown.png"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon SQL_CONNECT = new DBIcon("sql_connect", "sql/connect.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon SQL_DISCONNECT = new DBIcon("sql_disconnect", "sql/disconnect.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon SQL_SCRIPT = new DBIcon("sql_script", "sql/sql_script.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon SQL_EXECUTE = new DBIcon("sql_exec", "sql/sql_exec.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon SQL_SCRIPT_EXECUTE = new DBIcon("sql_script_exec", "sql/sql_script_exec.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon SQL_ANALYSE = new DBIcon("sql_analyse", "sql/sql_analyse.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon SQL_EXPLAIN_PLAN = new DBIcon("sql_explain", "sql/sql_plan.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon SQL_VALIDATE = new DBIcon("sql_validate", "sql/sql_validate.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon SQL_PREVIEW = new DBIcon("sql_preview", "sql/sql_preview.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon SQL_TEXT = new DBIcon("sql_text", "sql/sql_text.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon SAVE_TO_DATABASE = new DBIcon("save_to_db", "sql/save_to_database.png"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon OVER_SUCCESS = new DBIcon("over_success", "over/success_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon OVER_FAILED = new DBIcon("over_failed", "over/failed_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon OVER_ERROR = new DBIcon("over_error", "over/error_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon OVER_UNKNOWN = new DBIcon("over_condition", "over/conditional_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon OVER_LAMP = new DBIcon("over_lamp", "over/lamp_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon OVER_KEY = new DBIcon("over_key", "over/key_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon OVER_LOCK = new DBIcon("over_lock", "over/lock_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon OVER_EXTERNAL = new DBIcon("over_external", "over/external_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon OVER_REFERENCE = new DBIcon("over_reference", "over/reference_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$

    private static Map<String, DBPImage> iconMap = new HashMap<String, DBPImage>();


    static  {
        for (Field field : DBIcon.class.getDeclaredFields()) {
            if ((field.getModifiers() & Modifier.STATIC) == 0 || field.getType() != DBIcon.class) {
                continue;
            }
            try {
                DBIcon icon = (DBIcon) field.get(null);
                URL fileURL = FileLocator.toFileURL(new URL(icon.getLocation()));
                try {
                    String filePath = fileURL.toString().replace(" ", "%20");
                    File file = new File(new URI(filePath));
                    if (!file.exists()) {
                        log.warn("Bad image '" + icon.getToken() + "' location: " + icon.getLocation());
                        continue;
                    }
                    DBIcon.iconMap.put(icon.getToken(), icon);
                } catch (URISyntaxException e) {
                    throw new IOException("Bad local file path: " + fileURL, e);
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    private final String token;
    private final String path;

    public DBIcon(String path)
    {
        this.token = null;
        this.path = path;
    }

    private DBIcon(String token, String path)
    {
        this.token = token;
        this.path = ICON_LOCATION_PREFIX + path;
    }

    public static DBPImage getImageById(String token)
    {
        return iconMap.get(token);
    }


    /**
     * Token is icon id. It can be used to refer on icons in plugin extensions
     * @return unique token
     */
    public String getToken()
    {
        return token;
    }

    @Override
    public String getLocation() {
        return path;
    }

    static final String ICON_LOCATION_PREFIX = "platform:/plugin/" + DBeaverConstants.PLUGIN_ID + "/icons/";
}
