/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql;

import org.jkiss.dbeaver.ui.DBeaverConstants;

/**
 * SQL editor constants
 */
public class SQLConstants {

    public final static String SQL_CONTENT_TYPE = "org.jkiss.dbeaver.core.content-type-sql";

    public final static String SQL_COMMENT = "sql_comment";

    public static final String SHORT_MESSAGE = "short_message"; //$NON-NLS-1$

    /**
     * Marker type contant for SQL portability targets.
     */
    public static final String PORTABILITY_MARKER_TYPE = DBeaverConstants.PLUGIN_ID + ".portabilitytask";           //$NON-NLS-1$
    /**
     * Marker type contant for SQL syntax errors.
     */
    public static final String SYNTAX_MARKER_TYPE      = DBeaverConstants.PLUGIN_ID + ".syntaxproblem";             //$NON-NLS-1$

}
