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

package org.jkiss.dbeaver.ui.controls.resultset;

/**
 * Result set preference constants
 */
public final class ResultSetPreferences {


    // ResultSet
    public static final String RS_EDIT_MAX_TEXT_SIZE = "resultset.edit.maxtextsize"; //$NON-NLS-1$
    public static final String RS_EDIT_LONG_AS_LOB = "resultset.edit.longaslob"; //$NON-NLS-1$
    public static final String RS_EDIT_USE_ALL_COLUMNS = "resultset.edit.key.use_all_columns";
    public static final String RS_EDIT_AUTO_UPDATE_VALUE = "resultset.edit.value.autoupdate"; //$NON-NLS-1$
    public static final String RS_COMMIT_ON_EDIT_APPLY = "resultset.commit.oneditapply"; //$NON-NLS-1$
    public static final String RS_COMMIT_ON_CONTENT_APPLY = "resultset.commit.oncontentapply"; //$NON-NLS-1$
    public static final String RS_EDIT_NEW_ROWS_AFTER = "resultset.edit.new.row.after";
    public static final String RS_EDIT_REFRESH_AFTER_UPDATE = "resultset.edit.refreshAfterUpdate"; //$NON-NLS-1$
    public static final String RS_GROUPING_DEFAULT_SORTING = "resultset.grouping.defaultSorting"; //$NON-NLS-1$
    public static final String RS_GROUPING_SHOW_DUPLICATES_ONLY = "resultset.grouping.showDuplicatesOnly"; //$NON-NLS-1$

    public static final String RESULT_SET_AUTO_FETCH_NEXT_SEGMENT = "resultset.autofetch.next.segment"; //$NON-NLS-1$
    public static final String RESULT_SET_REREAD_ON_SCROLLING = "resultset.reread.on.scroll"; //$NON-NLS-1$
    public static final String RESULT_SET_READ_METADATA = "resultset.read.metadata"; //$NON-NLS-1$
    public static final String RESULT_SET_READ_REFERENCES = "resultset.read.references"; //$NON-NLS-1$
    public static final String RESULT_SET_MAX_ROWS = "resultset.maxrows"; //$NON-NLS-1$
    public static final String RESULT_SET_CANCEL_TIMEOUT = "resultset.cancel.timeout"; //$NON-NLS-1$
    public static final String RESULT_SET_BINARY_EDITOR_TYPE = "resultset.binary.editor"; //$NON-NLS-1$
    public static final String RESULT_SET_ORDER_SERVER_SIDE = "resultset.order.serverSide"; //$NON-NLS-1$
    public static final String RESULT_SET_SHOW_ODD_ROWS = "resultset.show.oddRows"; //$NON-NLS-1$
    public static final String RESULT_SET_SHOW_CELL_ICONS = "resultset.show.cellIcons"; //$NON-NLS-1$
    public static final String RESULT_SET_SHOW_ATTR_ICONS = "resultset.show.attIcons"; //$NON-NLS-1$
    public static final String RESULT_SET_SHOW_ATTR_FILTERS = "resultset.show.attFilters"; //$NON-NLS-1$
    public static final String RESULT_SET_SHOW_ATTR_ORDERING = "resultset.show.attOrdering"; //$NON-NLS-1$
    public static final String RESULT_SET_SHOW_DESCRIPTION = "resultset.show.columnDescription"; //$NON-NLS-1$
    public static final String RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES = "resultset.calc.columnWidthByValues"; //$NON-NLS-1$
    public static final String RESULT_SET_SHOW_CONNECTION_NAME = "resultset.show.connectionName"; //$NON-NLS-1$
    public static final String RESULT_SET_COLORIZE_DATA_TYPES = "resultset.show.colorizeDataTypes"; //$NON-NLS-1$
    public static final String RESULT_SET_RIGHT_JUSTIFY_NUMBERS = "resultset.show.rightJustifyNumbers"; //$NON-NLS-1$
    public static final String RESULT_SET_RIGHT_JUSTIFY_DATETIME = "resultset.show.rightJustifyDateTime"; //$NON-NLS-1$
    public static final String RESULT_SET_AUTO_SWITCH_MODE = "resultset.behavior.autoSwitchMode"; //$NON-NLS-1$
    public static final String RESULT_SET_DOUBLE_CLICK = "resultset.behavior.doubleClick"; //$NON-NLS-1$
    public static final String RESULT_SET_ROW_BATCH_SIZE = "resultset.show.row.batch.size"; //$NON-NLS-1$
    public static final String RESULT_SET_PRESENTATION = "resultset.presentation.active"; //$NON-NLS-1$
    public static final String RESULT_SET_STRING_USE_CONTENT_EDITOR = "resultset.string.use.content.editor"; //$NON-NLS-1$
    public static final String RESULT_SET_USE_NAVIGATOR_FILTERS = "resultset.filter.use.navigator"; //$NON-NLS-1$
    public static final String RESULT_TEXT_TAB_SIZE = "resultset.text.tab.size"; //$NON-NLS-1$
    public static final String RESULT_TEXT_MAX_COLUMN_SIZE = "resultset.text.max.column.size"; //$NON-NLS-1$
    public static final String RESULT_TEXT_VALUE_FORMAT = "resultset.text.value.format"; //$NON-NLS-1$
    public static final String RESULT_TEXT_SHOW_NULLS = "resultset.text.show.nulls"; //$NON-NLS-1$
    public static final String RESULT_TEXT_DELIMITER_LEADING = "resultset.text.delimiter.leading"; //$NON-NLS-1$
    public static final String RESULT_TEXT_DELIMITER_TRAILING = "resultset.text.delimiter.trailing"; //$NON-NLS-1$

    // Confirmations
    public static final String CONFIRM_ORDER_RESULTSET = "order_resultset"; //$NON-NLS-1$
    public static final String CONFIRM_RS_FETCH_ALL = "fetch_all_rows"; //$NON-NLS-1$
    public static final String CONFIRM_RS_EDIT_CLOSE = "close_resultset_edit"; //$NON-NLS-1$
    public static final String CONFIRM_RS_PANEL_RESET = "reset_panels_content"; //$NON-NLS-1$
    public static final String CONFIRM_KEEP_STATEMENT_OPEN = "keep_statement_open"; //$NON-NLS-1$

    public static final String KEEP_STATEMENT_OPEN = "keep.statement.open"; //$NON-NLS-1$
}
