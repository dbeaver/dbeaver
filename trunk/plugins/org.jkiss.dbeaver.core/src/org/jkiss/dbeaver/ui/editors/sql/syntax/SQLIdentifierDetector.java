/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Determines whether a given character is valid as part of an SQL identifier.
 */
public class SQLIdentifierDetector extends SQLWordDetector
{
    private char structSeparator;
    private String quoteSymbol;

    public SQLIdentifierDetector(char structSeparator, String quoteSymbol)
    {
        this.structSeparator = structSeparator;
        this.quoteSymbol = quoteSymbol;
    }

    public boolean containsSeparator(String identifier)
    {
        return identifier.indexOf(structSeparator) != -1;
    }

    public List<String> splitIdentifier(String identifier)
    {
        if (!containsSeparator(identifier)) {
            return Collections.singletonList(identifier);
        }
        List<String> tokens = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(identifier, String.valueOf(structSeparator));
        while (st.hasMoreTokens()) {
            tokens.add(st.nextToken());
        }
        return tokens;
    }

    @Override
    public boolean isWordPart(char c) {
        return super.isWordPart(c) ||
            (quoteSymbol != null && quoteSymbol.indexOf(c) != -1) ||
            structSeparator == c;
    }

    public boolean isPlainWordPart(char c) {
        return super.isWordPart(c);
    }

    public boolean isQuoted(String token)
    {
        return quoteSymbol != null && !quoteSymbol.isEmpty() && token.startsWith(quoteSymbol);
    }

    public String removeQuotes(String name)
    {
        if (quoteSymbol == null || quoteSymbol.isEmpty()) {
            return name;
        }
        if (name.startsWith(quoteSymbol)) {
            name = name.substring(quoteSymbol.length());
        }
        if (name.endsWith(quoteSymbol)) {
            name = name.substring(name.length() - quoteSymbol.length());
        }
        return name;
    }


}