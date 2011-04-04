/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

/**
 * ISearchTextRunner
 */
public interface ISearchTextRunner
{
    public static final int SEARCH_CASE_SENSITIVE   = 1;
    public static final int SEARCH_FORWARD          = 2;
    public static final int SEARCH_BACKWARD         = 4;

    void performSearch(String searchString, int options);

}