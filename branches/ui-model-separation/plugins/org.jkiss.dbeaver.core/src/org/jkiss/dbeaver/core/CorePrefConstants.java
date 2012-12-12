/*
 * Copyright (C) 2010-2012 Serge Rieder serge@jkiss.org
 * Copyright (C) 2012 Eugene Fradkin eugene.fradkin@gmail.com
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
package org.jkiss.dbeaver.core;

/**
 * CorePrefConstants
 */
public final class CorePrefConstants
{
    public static final String NATIVE_LIB_PATH = "native.lib.path"; //$NON-NLS-1$

    public static final String DEFAULT_AUTO_COMMIT = "default.autocommit"; //$NON-NLS-1$
    public static final String KEEP_STATEMENT_OPEN = "keep.statement.open"; //$NON-NLS-1$
    public static final String QUERY_ROLLBACK_ON_ERROR = "query.rollback-on-error"; //$NON-NLS-1$

    public static final String SCRIPT_COMMIT_TYPE = "script.commit.type"; //$NON-NLS-1$
    public static final String SCRIPT_COMMIT_LINES = "script.commit.lines"; //$NON-NLS-1$
    public static final String SCRIPT_ERROR_HANDLING = "script.error.handling"; //$NON-NLS-1$
    public static final String SCRIPT_FETCH_RESULT_SETS = "script.fetch.resultset"; //$NON-NLS-1$
    public static final String SCRIPT_AUTO_FOLDERS = "script.auto.folders"; //$NON-NLS-1$

	public static final String STATEMENT_TIMEOUT = "statement.timeout"; //$NON-NLS-1$
	public static final String MEMORY_CONTENT_MAX_SIZE = "content.memory.maxsize"; //$NON-NLS-1$
	public static final String CONTENT_HEX_ENCODING = "content.hex.encoding"; //$NON-NLS-1$
	public static final String READ_EXPENSIVE_PROPERTIES = "database.props.expensive"; //$NON-NLS-1$
	public static final String META_CASE_SENSITIVE = "database.meta.casesensitive"; //$NON-NLS-1$

	// Resources
	public static final String RESOURCE_HANDLER_ROOT_PREFIX = "resource.root."; //$NON-NLS-1$

	// Network
	public static final String NET_TUNNEL_PORT_MIN = "net.tunnel.port.min"; //$NON-NLS-1$
	public static final String NET_TUNNEL_PORT_MAX = "net.tunnel.port.max"; //$NON-NLS-1$

	// ResultSet
	public static final String RESULT_SET_MAX_ROWS = "resultset.maxrows"; //$NON-NLS-1$

	public static final String RESULT_SET_BINARY_SHOW_STRINGS = "resultset.binary.showStrings"; //$NON-NLS-1$

	public static final String RESULT_SET_BINARY_STRING_MAX_LEN = "resultset.binary.stringMaxLength"; //$NON-NLS-1$

}
