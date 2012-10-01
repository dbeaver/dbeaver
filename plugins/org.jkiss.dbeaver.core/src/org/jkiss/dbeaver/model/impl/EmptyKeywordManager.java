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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.dbeaver.model.DBPKeywordManager;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.utils.Pair;

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
    public boolean isEntityQueryWord(String word)
    {
        return false;
    }

    @Override
    public boolean isAttributeQueryWord(String word)
    {
        return false;
    }

    @Override
    public Pair<String, String> getMultiLineComments()
    {
        return null;
    }

    @Override
    public String[] getSingleLineComments()
    {
        return new String[0];
    }

}
