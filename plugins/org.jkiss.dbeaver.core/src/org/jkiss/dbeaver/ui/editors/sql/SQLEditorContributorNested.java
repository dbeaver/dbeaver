/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql;

/**
 * Nested SQL editor contributor.
 * It is really nested if more than one contributor exists at the same time.
 * Otherwise it should act as normal contributor
 */
public class SQLEditorContributorNested extends SQLEditorContributor {

    private static int nestedCount = 0;

    private int nestedId;

    public SQLEditorContributorNested()
    {
        nestedId = nestedCount;
        nestedCount++;
    }

    @Override
    public void dispose()
    {
        super.dispose();
        nestedCount--;
    }

    @Override
    protected boolean isNestedEditor()
    {
        return false;//nestedId > 0;
    }

}
