/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql;

public class SQLPreferenceConstants
{

    public static final String INSERT_SINGLE_PROPOSALS_AUTO            = "insert.single.proposals.auto";
    public static final String ENABLE_AUTO_ACTIVATION                  = "enable.auto.activation";
    public static final String AUTO_ACTIVATION_DELAY                   = "auto.activation.delay";
    public static final String PROPOSAL_INSERT_CASE                    = "proposal.insert.case";

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

    public static final int PROPOSAL_CASE_DEFAULT                      = 0;
    public static final int PROPOSAL_CASE_UPPER                        = 1;
    public static final int PROPOSAL_CASE_LOWER                        = 2;
}
