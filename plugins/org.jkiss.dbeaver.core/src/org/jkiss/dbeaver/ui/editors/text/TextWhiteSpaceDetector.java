/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.text;

import org.eclipse.jface.text.rules.IWhitespaceDetector;

/**
 * A class that determines if a character is an SQL whitespace character
 */
public class TextWhiteSpaceDetector implements IWhitespaceDetector
{

    public boolean isWhitespace(char c)
    {
        return Character.isWhitespace(c);
    }

}