/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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

package org.jkiss.dbeaver;

/**
 * Preferences constants
 */
public final class ModelPreferences
{
    public static final String QUERY_ROLLBACK_ON_ERROR = "query.rollback-on-error"; //$NON-NLS-1$

    public static final String SCRIPT_STATEMENT_DELIMITER = "script.sql.delimiter"; //$NON-NLS-1$
    public static final String SCRIPT_IGNORE_NATIVE_DELIMITER = "script.sql.ignoreNativeDelimiter"; //$NON-NLS-1$

    public static final String MEMORY_CONTENT_MAX_SIZE = "content.memory.maxsize"; //$NON-NLS-1$
    public static final String CONTENT_HEX_ENCODING = "content.hex.encoding"; //$NON-NLS-1$
    public static final String META_SEPARATE_CONNECTION = "database.meta.separate.connection"; //$NON-NLS-1$
    public static final String META_CASE_SENSITIVE = "database.meta.casesensitive"; //$NON-NLS-1$

    // Network
    public static final String NET_TUNNEL_PORT_MIN = "net.tunnel.port.min"; //$NON-NLS-1$
    public static final String NET_TUNNEL_PORT_MAX = "net.tunnel.port.max"; //$NON-NLS-1$

    // ResultSet
    public static final String RESULT_SET_MAX_ROWS_USE_SQL = "resultset.maxrows.sql"; //$NON-NLS-1$
    public static final String RESULT_SET_BINARY_PRESENTATION = "resultset.binary.representation"; //$NON-NLS-1$
    public static final String RESULT_SET_BINARY_STRING_MAX_LEN = "resultset.binary.stringMaxLength"; //$NON-NLS-1$

    public static final String SQL_PARAMETERS_ENABLED = "sql.parameter.enabled"; //$NON-NLS-1$
    public static final String SQL_ANONYMOUS_PARAMETERS_ENABLED = "sql.parameter.anonymous.enabled"; //$NON-NLS-1$
    public static final String SQL_ANONYMOUS_PARAMETERS_MARK = "sql.parameter.mark"; //$NON-NLS-1$
}
