/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.core.DBeaverIcons;

/**
 * DBIcon
 */
public enum DBIcon
{
    DBEAVER_LOGO("dbeaver_logo", "icons/dbeaver.png"), //$NON-NLS-1$ //$NON-NLS-2$

    GEN_DATABASE("gen_database", "icons/database.png"), //$NON-NLS-1$ //$NON-NLS-2$
    GEN_DATABASE_TYPE("gen_database_type", "icons/database_type.png"), //$NON-NLS-1$ //$NON-NLS-2$

    TREE("tree", "icons/tree/tree.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_ADMIN("admin", "icons/tree/admin.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_DATABASE("database", "icons/tree/database.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_DATABASE_CATEGORY("database_category", "icons/tree/database_category.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_SCHEMA("schema", "icons/tree/schema.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_TABLES("tables", "icons/tree/tables.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_TABLE("table", "icons/tree/table.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_TABLE_ALIAS("table_alias", "icons/tree/table_alias.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_TABLE_SORT("table_sort", "icons/tree/table_sort.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_VIEW("view", "icons/tree/view.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FUNCTION("function", "icons/tree/function.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_PROCEDURE("procedure", "icons/tree/procedure.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_COLUMNS("columns", "icons/tree/columns.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_COLUMN("column", "icons/tree/column.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_INDEX("index", "icons/tree/index.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_CONSTRAINT("constraint", "icons/tree/constraint.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_UNIQUE_KEY("unique-key", "icons/tree/unique_constraint.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOREIGN_KEY("foreign-key", "icons/tree/foreign_key.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOREIGN_KEY_COLUMN("foreign-key-column", "icons/tree/foreign_key_column.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_REFERENCE("reference", "icons/tree/reference.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_TRIGGER("trigger", "icons/tree/trigger.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_USER("user", "icons/tree/user.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_PAGE("page", "icons/tree/page.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER("folder", "icons/tree/folder.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_LINK("folder_linked", "icons/tree/folder_link.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_DATABASE("folder_database", "icons/tree/folder_database.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_SCHEMA("folder_schema", "icons/tree/folder_schema.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_TABLE("folder_table", "icons/tree/folder_table.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_VIEW("folder_view", "icons/tree/folder_view.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_USER("folder_user", "icons/tree/folder_user.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_ADMIN("folder_admin", "icons/tree/folder_admin.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_SESSIONS("sessions", "icons/tree/sessions.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_PACKAGE("package", "icons/tree/package.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_DATA_TYPE("data_type", "icons/tree/data_type.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_SEQUENCE("sequence", "icons/tree/sequence.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_SYNONYM("synonym", "icons/tree/synonym.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_TABLESPACE("tablespace", "icons/tree/tablespace.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_PARTITION("partition", "icons/tree/partition.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_ATTRIBUTE("attribute", "icons/tree/attribute.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_ARGUMENT("argument", "icons/tree/argument.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_JAVA_CLASS("javaClass", "icons/tree/java_class.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_JAVA_INTERFACE("javaInterface", "icons/tree/java_interface.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_LINK("link", "icons/tree/link.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FILE("file", "icons/tree/file.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_CLASS("class", "icons/tree/class.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_ASSOCIATION("association", "icons/tree/association.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_SERVER("server", "icons/tree/server.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_SERVERS("servers", "icons/tree/servers.png"), //$NON-NLS-1$ //$NON-NLS-2$

    TREE_INFO("info", "/icons/tree/info.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_INFO("folder_info", "/icons/tree/folder_info.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_RECYCLE_BIN("recycle_bin", "/icons/tree/recycle_bin.png"), //$NON-NLS-1$ //$NON-NLS-2$

    TREE_EXPAND("expand", "/icons/misc/expand.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_COLLAPSE("collapse", "/icons/misc/collapse.png"), //$NON-NLS-1$ //$NON-NLS-2$

    PROJECT("project", "icons/project.png"), //$NON-NLS-1$ //$NON-NLS-2$
    PROJECTS("projects", "icons/projects.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CONNECTIONS("connections", "icons/connections.png"), //$NON-NLS-1$ //$NON-NLS-2$
    DATABASES("databases", "icons/databases.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SCRIPTS("scripts", "icons/scripts.png"), //$NON-NLS-1$ //$NON-NLS-2$
    BOOKMARK_FOLDER("bookmark_folder", "icons/bookmark_folder.png"), //$NON-NLS-1$ //$NON-NLS-2$
    BOOKMARK("bookmark", "icons/bookmark.png"), //$NON-NLS-1$ //$NON-NLS-2$
    PICTURE("picture", "icons/picture.png"), //$NON-NLS-1$ //$NON-NLS-2$
    PICTURE_SAVE("picture_save", "icons/picture_save.png"), //$NON-NLS-1$ //$NON-NLS-2$
    PROPERTIES("properties", "icons/properties.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CONFIGURATION("configuration", "icons/configuration.png"), //$NON-NLS-1$ //$NON-NLS-2$
    LINK("link", "icons/misc/link.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CURSOR("cursor", "icons/misc/cursor.png"), //$NON-NLS-1$ //$NON-NLS-2$

    LOADING1("loading1", "icons/tree/load/loading1.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    LOADING2("loading2", "icons/tree/load/loading2.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    LOADING3("loading3", "icons/tree/load/loading3.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    LOADING4("loading4", "icons/tree/load/loading4.gif"), //$NON-NLS-1$ //$NON-NLS-2$

    PROGRESS0("progress0", "icons/misc/progress_0.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    PROGRESS1("progress1", "icons/misc/progress_1.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    PROGRESS2("progress2", "icons/misc/progress_2.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    PROGRESS3("progress3", "icons/misc/progress_3.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    PROGRESS4("progress4", "icons/misc/progress_4.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    PROGRESS5("progress5", "icons/misc/progress_5.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    PROGRESS6("progress6", "icons/misc/progress_6.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    PROGRESS7("progress7", "icons/misc/progress_7.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    PROGRESS8("progress8", "icons/misc/progress_8.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    PROGRESS9("progress9", "icons/misc/progress_9.gif"), //$NON-NLS-1$ //$NON-NLS-2$

    RS_FIRST("rs_first", "/icons/sql/resultset_first.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_LAST("rs_last", "/icons/sql/resultset_last.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_NEXT("rs_next", "/icons/sql/resultset_next.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_PREV("rs_prev", "/icons/sql/resultset_previous.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_REFRESH("rs_refresh", "/icons/sql/resultset_refresh.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_GRID("rs_mode_grid", "icons/sql/grid.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_DETAILS("rs_details", "icons/sql/details.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_FORWARD("rs_forward", "icons/sql/forward.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_BACK("rs_back", "icons/sql/back.png"), //$NON-NLS-1$ //$NON-NLS-2$

    TXN_COMMIT_AUTO("txn_commit_auto", "icons/sql/txn_auto.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TXN_COMMIT_MANUAL("txn_commit_manual", "icons/sql/txn_manual.png"), //$NON-NLS-1$ //$NON-NLS-2$

    RULER_POSITION("ruler_position", "/icons/misc/ruler_position.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FILTER_VALUE("filter_value", "/icons/misc/filter_value.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FILTER_INPUT("filter_input", "/icons/misc/filter_input.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FILTER_CLIPBOARD("filter_clipboard", "/icons/misc/filter_clipboard.png"), //$NON-NLS-1$ //$NON-NLS-2$

    FIND("find", "/icons/misc/find.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FIND_TEXT("find_text", "/icons/misc/find_text.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SEARCH("search", "/icons/misc/search.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CHECK("check", "/icons/misc/check.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CHECK2("check2", "/icons/misc/check2.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CHECK_ON("checked", "/icons/misc/checked.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CHECK_OFF("unchecked", "/icons/misc/unchecked.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ZOOM("zoom", "/icons/misc/zoom.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ZOOM_IN("zoom_in", "/icons/misc/zoom_in.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ZOOM_OUT("zoom_out", "/icons/misc/zoom_out.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROTATE("rotate", "/icons/misc/rotate.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROTATE_LEFT("rotate_left", "/icons/misc/rotate_left.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROTATE_RIGHT("rotate_right", "/icons/misc/rotate_right.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FIT_WINDOW("fit_window", "/icons/misc/fit-window.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ORIGINAL_SIZE("original_size", "/icons/misc/original-size.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ASTERISK("asterisk", "/icons/misc/asterisk.png"), //$NON-NLS-1$ //$NON-NLS-2$
    BULLET_BLACK("bullet_black", "/icons/misc/bullet_black.png"), //$NON-NLS-1$ //$NON-NLS-2$
    BULLET_GREEN("bullet_green", "/icons/misc/bullet_green.png"), //$NON-NLS-1$ //$NON-NLS-2$
    BULLET_RED("bullet_red", "/icons/misc/bullet_red.png"), //$NON-NLS-1$ //$NON-NLS-2$
    BULLET_STAR("bullet_star", "/icons/misc/bullet_star.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_UP("arrow_up", "/icons/misc/arrow_up.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_DOWN("arrow_down", "/icons/misc/arrow_down.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_LEFT("arrow_left", "/icons/misc/arrow_left.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_LEFT_ALL("arrow_left_all", "/icons/misc/arrow_left_all.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_RIGHT("arrow_right", "/icons/misc/arrow_right.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_RIGHT_ALL("arrow_right_all", "/icons/misc/arrow_right_all.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_RESET("arrow_reset", "/icons/misc/arrow_reset.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SORT_INCREASE("sort_increase", "/icons/misc/sort_increase.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SORT_DECREASE("sort_decrease", "/icons/misc/sort_decrease.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SORT_UNKNOWN("sort_unknown", "/icons/misc/sort_unknown.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FRAME_QUERY("frame_query", "/icons/misc/frame_query.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FILTER("filter", "/icons/misc/filter.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FILTER_APPLY("filter_apply", "/icons/misc/filter_apply.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FILTER_RESET("filter_reset", "/icons/misc/filter_reset.png"), //$NON-NLS-1$ //$NON-NLS-2$
    EVENT("event", "/icons/misc/event.png"), //$NON-NLS-1$ //$NON-NLS-2$
    HOME("home", "/icons/misc/home.png"), //$NON-NLS-1$ //$NON-NLS-2$
    COMPILE("compile", "/icons/misc/compile.png"), //$NON-NLS-1$ //$NON-NLS-2$
    COMPILE_LOG("compile_log", "/icons/misc/compile_log.png"), //$NON-NLS-1$ //$NON-NLS-2$

    SAVE("save", "/icons/file/save.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SAVE_ALL("save_all", "/icons/file/save_all.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SAVE_AS("save_as", "/icons/file/save_as.png"), //$NON-NLS-1$ //$NON-NLS-2$
    LOAD("load", "/icons/file/load.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RESET("reset", "/icons/file/reset.png"), //$NON-NLS-1$ //$NON-NLS-2$
    COMPARE("compare", "/icons/file/compare.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ACCEPT("accept", "/icons/sql/accept.png"), //$NON-NLS-1$ //$NON-NLS-2$
    REJECT("reject", "/icons/sql/cancel.png"), //$NON-NLS-1$ //$NON-NLS-2$
    REVERT("revert", "/icons/sql/revert.png"), //$NON-NLS-1$ //$NON-NLS-2$
    IMPORT("import", "/icons/file/import.png"), //$NON-NLS-1$ //$NON-NLS-2$
    EXPORT("export", "/icons/file/export.png"), //$NON-NLS-1$ //$NON-NLS-2$
    REFRESH("refresh", "/icons/refresh.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CONFIRM("confirm", "/icons/misc/confirm.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CLOSE("close", "/icons/misc/close.png"), //$NON-NLS-1$ //$NON-NLS-2$
    JAR("jar", "/icons/misc/jar.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SOURCES("sources", "/icons/misc/sources.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CANCEL("cancel", "/icons/misc/cancel.png"), //$NON-NLS-1$ //$NON-NLS-2$

    ROW_ADD("row_add", "/icons/sql/row_add.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROW_COPY("row_copy", "/icons/sql/row_copy.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROW_EDIT("row_edit", "/icons/sql/row_edit.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROW_DELETE("row_delete", "/icons/sql/row_delete.png"), //$NON-NLS-1$ //$NON-NLS-2$

    EDIT_DATABASE("edit_database", "/icons/misc/edit_database.png"), //$NON-NLS-1$ //$NON-NLS-2$
    EDIT_TABLE("edit_table", "/icons/misc/edit_table.png"), //$NON-NLS-1$ //$NON-NLS-2$
    EDIT_COLUMN("edit_column", "/icons/misc/edit_column.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CONFIG_TABLE("edit_table", "/icons/misc/config_table.png"), //$NON-NLS-1$ //$NON-NLS-2$
    LOCKED("locked", "/icons/misc/locked.png"), //$NON-NLS-1$ //$NON-NLS-2$

    TYPE_BOOLEAN("boolean", "/icons/sql/types/boolean.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_NUMBER("number", "/icons/sql/types/number.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_STRING("string", "/icons/sql/types/string.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_DATETIME("datetime", "/icons/sql/types/datetime.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_BINARY("binary", "/icons/sql/types/binary.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_TEXT("text", "/icons/sql/types/text.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_XML("xml", "/icons/sql/types/xml.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_LOB("lob", "/icons/sql/types/lob.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_ARRAY("array", "/icons/sql/types/array.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_STRUCT("struct", "/icons/sql/types/struct.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_DOCUMENT("document", "/icons/sql/types/document.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_OBJECT("object", "/icons/sql/types/object.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_IMAGE("image", "/icons/sql/types/image.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_REFERENCE("reference", "/icons/sql/types/reference.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_ROWID("rowid", "/icons/sql/types/rowid.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_ANY("any", "/icons/sql/types/any.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_UUID("uuid", "/icons/sql/types/uuid.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_UNKNOWN("unknown", "/icons/sql/types/unknown.png"), //$NON-NLS-1$ //$NON-NLS-2$

    SQL_CONNECT("sql_connect", "/icons/sql/connect.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_DISCONNECT("sql_disconnect", "/icons/sql/disconnect.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_SCRIPT("sql_script", "/icons/sql/sql_script.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_EXECUTE("sql_exec", "/icons/sql/sql_exec.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_SCRIPT_EXECUTE("sql_script_exec", "/icons/sql/sql_script_exec.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_ANALYSE("sql_analyse", "/icons/sql/sql_analyse.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_EXPLAIN_PLAN("sql_explain", "/icons/sql/sql_plan.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_VALIDATE("sql_validate", "/icons/sql/sql_validate.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_PREVIEW("sql_preview", "/icons/sql/sql_preview.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_TEXT("sql_text", "/icons/sql/sql_text.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SAVE_TO_DATABASE("save_to_db", "/icons/sql/save_to_database.png"), //$NON-NLS-1$ //$NON-NLS-2$

    OVER_SUCCESS("over_success", "/icons/over/success_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_FAILED("over_failed", "/icons/over/failed_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_ERROR("over_error", "/icons/over/error_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_UNKNOWN("over_condition", "/icons/over/conditional_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_LAMP("over_lamp", "/icons/over/lamp_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_KEY("over_key", "/icons/over/key_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_LOCK("over_lock", "/icons/over/lock_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_EXTERNAL("over_external", "/icons/over/external_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_REFERENCE("over_reference", "/icons/over/reference_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$

    private final String token;
    private final String path;

    DBIcon(String token, String path)
    {
        this.token = token;
        this.path = path;
    }

    /**
     * Token is icon id. It can be used to refer on icons in plugin extensions
     * @return unique token
     */
    public String getToken()
    {
        return token;
    }

    public String getPath()
    {
        return path;
    }

    public Image getImage()
    {
        return DBeaverIcons.getImage(this);
    }

    public ImageDescriptor getImageDescriptor()
    {
        return DBeaverIcons.getImageDescriptor(this);
    }

}
