/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import java.util.List;
import java.util.Set;

/**
 * Keyword manager.
 * <p/>
 * Contains information about some concrete datasource underlying database syntax.
 * Support runtime change of datasource (reloads syntax information)
 */
public interface DBPKeywordManager {

    Set<String> getReservedWords();

    Set<String> getFunctions();

    Set<String> getTypes();

    DBPKeywordType getKeywordType(String word);

    List<String> getMatchedKeywords(String word);

    boolean isKeywordStart(String word);

    boolean isTableQueryWord(String word);

    boolean isColumnQueryWord(String word);

    String[] getSingleLineComments();
}
