/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql;

import org.jkiss.dbeaver.ui.DBeaverConstants;


public class SQLPreferenceConstants
{

    public static final String PAGE_GENERAL                            = DBeaverConstants.PLUGIN_ID + ".GeneralPreferencePage";
    public static final String PAGE_LOGGING                            = DBeaverConstants.PLUGIN_ID + ".LoggingPreferencePage";
    public static final String PAGE_TEMPLATE                           = DBeaverConstants.PLUGIN_ID + ".TemplatesPreferencePage";
    public static final String PAGE_PERSPECTIVE                        = DBeaverConstants.PLUGIN_ID + ".PerspectivePage";
    public static final String PAGE_SQLFILE                            = DBeaverConstants.PLUGIN_ID + ".sqlfile";
    public static final String PAGE_CODEASSIST                         = DBeaverConstants.PLUGIN_ID + ".codeassist";
    public static final String PAGE_EXPORT                             = DBeaverConstants.PLUGIN_ID + ".ExportFormatPreferencePage";
    public static final String PAGE_SQLEDITOR                          = DBeaverConstants.PLUGIN_ID + ".SQLEditor";
    public static final String PAGE_SQLDEBUG                           = DBeaverConstants.PLUGIN_ID + ".SQLDebug";
    public static final String PAGE_RESULT                             = DBeaverConstants.PLUGIN_ID + ".sqlresultsview";
    public static final String PAGE_CONNECTIONOPTIONS                  = DBeaverConstants.PLUGIN_ID + ".connectionleveloptions";
    public static final String PAGE_MISC                               = DBeaverConstants.PLUGIN_ID + ".miscpage";
    public static final String PAGE_PLAN                               = DBeaverConstants.PLUGIN_ID + ".queryplan";

    /**
	 * A named preference that defines whether hint to make hover sticky should
	 * be shown.
	 */
    public static final String EDITOR_SHOW_TEXT_HOVER_AFFORDANCE = "PreferenceConstants.EDITOR_SHOW_TEXT_HOVER_AFFORDANCE"; //$NON-NLS-1$

    /**
     * A named preference that defines what the target database type is in portability check.
     */
    public static final String EDITOR_PORTABILITY_CHECK_TARGET   = "PreferenceConstants.EDITOR_PORTABILITY_CHECK_TARGET";

    public static final String FAIL_TO_CONNECT_DATABASE                = "sqlfile.fail.to.connect.database";

    public static final String SHOW_SYSTEM_TABLES                  = "show.system.tables";

    public static final String SHOW_SYSTEM_PROCEDURES              = "show.system.procedures";

    public static final String SHOW_OWNER_OF_TABLE                 = "show.owner.of.table";

    public static final String SHOW_SYSTEM_VIEWS                   = "show.system.views";

    public static final String INSERT_SINGLE_PROPOSALS_AUTO            = "insert.single.proposals.auto";

    public static final String ENABLE_AUTO_ACTIVATION                  = "enable.auto.activation";

    public static final String AUTO_ACTIVATION_DELAY                   = "auto.activation.delay";

    public static final String AUTO_ACTIVATION_TRIGGER                 = "auto.activation.trigger";

    // Syntax Validation
    public static final String SYNTAX_VALIDATION                       = "syntax.validation";

    public static final String SYNTAX_VALIDATION_MAX_LINE              = "syntax.validation.max.line";

    public static final String SYNTAX_VALIDATION_MAX_LINE_NUMBER       = "syntax.validation.max.line.number";

    public static final String SHOW_DAILOG_FOR_SYNTAX_VALIDATION       = "show.dailog.for.syntax.validation";

    public static final String SHOW_SYNTAX_ERROR_DETAIL                = "show.syntax.error.detail";

    // Typing constants
    public static final String SQLEDITOR_CLOSE_SINGLE_QUOTES           = "SQLEditor.closeSingleQuotes";

    public static final String SQLEDITOR_CLOSE_DOUBLE_QUOTES           = "SQLEditor.closeDoubleQuotes";

    public static final String SQLEDITOR_CLOSE_BRACKETS                = "SQLEditor.closeBrackets";

    public static final String SQLEDITOR_CLOSE_COMMENTS                = "SQLEditor.closeComments";

    public static final String SQLEDITOR_CLOSE_BEGIN_END               = "SQLEditor.closeBeginEndStatement";

    public static final String EXTERNAL_TOOL_CONFIGURED                = "external.tool.configured";


}
