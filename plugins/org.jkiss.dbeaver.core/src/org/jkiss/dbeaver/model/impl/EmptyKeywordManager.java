/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl;

import org.jkiss.dbeaver.model.DBPKeywordManager;
import org.jkiss.dbeaver.model.DBPKeywordType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Empty keyword manager
 */
public class EmptyKeywordManager implements DBPKeywordManager {
    public static final DBPKeywordManager INSTANCE = new EmptyKeywordManager();

    public Set<String> getReservedWords()
    {
        return Collections.emptySet();
    }

    public Set<String> getFunctions()
    {
        return Collections.emptySet();
    }

    public Set<String> getTypes()
    {
        return Collections.emptySet();
    }

    public DBPKeywordType getKeywordType(String word)
    {
        return null;
    }

    public List<String> getMatchedKeywords(String word)
    {
        return Collections.emptyList();
    }

    public boolean isKeywordStart(String word)
    {
        return false;
    }

    public boolean isTableQueryWord(String word)
    {
        return false;
    }

    public boolean isColumnQueryWord(String word)
    {
        return false;
    }

    public String[] getSingleLineComments()
    {
        return new String[0];
    }

}
