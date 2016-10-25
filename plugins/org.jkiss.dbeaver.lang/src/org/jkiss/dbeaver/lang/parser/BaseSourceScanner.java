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
package org.jkiss.dbeaver.lang.parser;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.rules.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.lang.SCMSourceScanner;
import org.jkiss.dbeaver.lang.SCMSourceText;
import org.jkiss.dbeaver.lang.SCMToken;
import org.jkiss.utils.ArrayUtils;

import java.util.Collection;

/**
 * BaseSourceScanner.
 */
public class BaseSourceScanner extends RuleBasedScanner implements SCMSourceScanner, SCMSourceText {

    public BaseSourceScanner(Document document, Collection<IRule> rules) {

        setRules(ArrayUtils.toArray(IRule.class, rules));

        setRange(document, 0, document.getLength());
    }

    @NotNull
    @Override
    public SCMSourceText getSource() {
        return this;
    }

    @Override
    public int getLength() {
        return fDocument.getLength();
    }

    @Override
    public char getChar(int offset) throws IndexOutOfBoundsException {
        try {
            return fDocument.getChar(offset);
        } catch (BadLocationException e) {
            throw new IndexOutOfBoundsException(e.getMessage());
        }
    }

    @NotNull
    @Override
    public String getSegment(int beginOffset, int endOffset) {
        try {
            return fDocument.get(beginOffset, endOffset - beginOffset);
        } catch (BadLocationException e) {
            throw new IndexOutOfBoundsException(e.getMessage());
        }
    }
}
