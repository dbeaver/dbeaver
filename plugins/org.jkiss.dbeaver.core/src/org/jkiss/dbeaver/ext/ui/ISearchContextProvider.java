/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

/**
 * ISearchContextProvider
 */
public interface ISearchContextProvider
{
    public static enum SearchType {
        NONE,
        NEXT,
        PREVIOUS
    }

    boolean isSearchPossible();

    boolean isSearchEnabled();

    void performSearch(SearchType searchType);

}