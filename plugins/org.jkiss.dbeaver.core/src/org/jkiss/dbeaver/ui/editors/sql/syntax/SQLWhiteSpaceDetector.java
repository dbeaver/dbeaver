/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.rules.IWhitespaceDetector;

/**
 * A class that determines if a character is an SQL whitespace character
 */
public class SQLWhiteSpaceDetector implements IWhitespaceDetector
{

    public boolean isWhitespace(char c)
    {
        return Character.isWhitespace(c);
    }

}