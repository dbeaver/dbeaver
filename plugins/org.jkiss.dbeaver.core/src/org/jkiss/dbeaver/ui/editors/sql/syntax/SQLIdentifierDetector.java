/*
 * Copyright (C) 2010-2012 Serge Rieder
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
    private String catalogSeparator;
    private String quoteSymbol;

    public SQLIdentifierDetector(String catalogSeparator, String quoteSymbol)
    {
        this.catalogSeparator = catalogSeparator;
        this.quoteSymbol = quoteSymbol;
    }

    public String getCatalogSeparator()
    {
        return catalogSeparator;
    }

    public String getQuoteSymbol()
    {
        return quoteSymbol;
    }

    public boolean containsSeparator(String identifier)
    {
        if (catalogSeparator.length() == 1) {
            return identifier.indexOf(catalogSeparator.charAt(0)) != -1;
        } else {
            for (int i = 0; i < catalogSeparator.length(); i++) {
                if (identifier.indexOf(catalogSeparator.charAt(i)) != -1) {
                    return true;
                }
            }
            return false;
        }
    }

    public List<String> splitIdentifier(String identifier)
    {
        if (!containsSeparator(identifier)) {
            return Collections.singletonList(identifier);
        }
        List<String> tokens = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(identifier, catalogSeparator);
        while (st.hasMoreTokens()) {
            tokens.add(st.nextToken());
        }
        return tokens;
    }

    @Override
    public boolean isWordPart(char c) {
        return super.isWordPart(c) ||
            (quoteSymbol != null && quoteSymbol.indexOf(c) != -1) ||
            (catalogSeparator != null && catalogSeparator.indexOf(c) != -1);
    }

    public boolean isPlainWordPart(char c) {
        return super.isWordPart(c);
    }

    public boolean isQuoted(String token)
    {
        return token.startsWith(quoteSymbol);
    }

    public String removeQuotes(String name)
    {
        if (name.startsWith(quoteSymbol)) {
            name = name.substring(quoteSymbol.length());
        }
        if (name.endsWith(quoteSymbol)) {
            name = name.substring(name.length() - quoteSymbol.length());
        }
        return name;
    }


}