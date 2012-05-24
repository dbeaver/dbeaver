/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
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

    @Override
    public Set<String> getReservedWords()
    {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getFunctions()
    {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getTypes()
    {
        return Collections.emptySet();
    }

    @Override
    public DBPKeywordType getKeywordType(String word)
    {
        return null;
    }

    @Override
    public List<String> getMatchedKeywords(String word)
    {
        return Collections.emptyList();
    }

    @Override
    public boolean isKeywordStart(String word)
    {
        return false;
    }

    @Override
    public boolean isTableQueryWord(String word)
    {
        return false;
    }

    @Override
    public boolean isColumnQueryWord(String word)
    {
        return false;
    }

    @Override
    public String[] getSingleLineComments()
    {
        return new String[0];
    }

}
