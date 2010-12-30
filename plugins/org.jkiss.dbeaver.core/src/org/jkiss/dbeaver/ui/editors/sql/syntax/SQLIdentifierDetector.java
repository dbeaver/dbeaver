/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
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

    public SQLIdentifierDetector(String catalogSeparator)
    {
        this.catalogSeparator = catalogSeparator;
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

    public boolean isWordPart(char c) {
        return super.isWordPart(c) || catalogSeparator.indexOf(c) != -1;
    }

    public boolean isPlainWordPart(char c) {
        return super.isWordPart(c);
    }

}