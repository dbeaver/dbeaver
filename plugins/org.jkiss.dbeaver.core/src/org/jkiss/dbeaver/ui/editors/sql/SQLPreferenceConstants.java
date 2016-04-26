/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql;

public class SQLPreferenceConstants
{

    public static final String INSERT_SINGLE_PROPOSALS_AUTO            = "insert.single.proposals.auto";
    public static final String ENABLE_AUTO_ACTIVATION                  = "enable.auto.activation";
    public static final String AUTO_ACTIVATION_DELAY                   = "auto.activation.delay";
    public static final String PROPOSAL_INSERT_CASE                    = "proposal.insert.case";
    public static final String HIDE_DUPLICATE_PROPOSALS                = "hide.duplicate.proposals";
    public static final String PROPOSAL_SHORT_NAME                     = "proposals.short.name";

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

    // Matching brackets
    public final static String MATCHING_BRACKETS                        = "SQLEditor.matchingBrackets";
    public final static String MATCHING_BRACKETS_COLOR                  = "SQLEditor.matchingBracketsColor";

    public final static String RESET_CURSOR_ON_EXECUTE                  = "SQLEditor.resetCursorOnExecute";

    public static final int PROPOSAL_CASE_DEFAULT                       = 0;
    public static final int PROPOSAL_CASE_UPPER                         = 1;
    public static final int PROPOSAL_CASE_LOWER                         = 2;

    public final static String SQL_FORMAT_KEYWORD_CASE_AUTO =           "SQLEditor.format.keywordCaseAuto";
    public final static String SQL_FORMAT_EXTRACT_FROM_SOURCE =         "SQLEditor.format.extractFromSource";

    public final static String BEEP_ON_QUERY_END                        = "SQLEditor.beepOnQueryEnd";
}
