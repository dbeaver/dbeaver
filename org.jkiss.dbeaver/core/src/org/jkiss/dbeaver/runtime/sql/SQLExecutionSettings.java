/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.sql;

/**
 * SQLExecutionSettings
 */
public class SQLExecutionSettings
{

    private SQLScriptCommitType commitType = SQLScriptCommitType.AT_END;
    private int commitLines = 1000;
    private SQLScriptErrorHandling errorHandling = SQLScriptErrorHandling.STOP_ROLLBACK;
    private boolean fetchResultSets = false;

    public SQLScriptCommitType getCommitType()
    {
        return commitType;
    }

    public void setCommitType(SQLScriptCommitType commitType)
    {
        this.commitType = commitType;
    }

    public int getCommitLines()
    {
        return commitLines;
    }

    public void setCommitLines(int commitLines)
    {
        this.commitLines = commitLines;
    }

    public SQLScriptErrorHandling getErrorHandling()
    {
        return errorHandling;
    }

    public void setErrorHandling(SQLScriptErrorHandling errorHandling)
    {
        this.errorHandling = errorHandling;
    }

    public boolean isFetchResultSets()
    {
        return fetchResultSets;
    }

    public void setFetchResultSets(boolean fetchResultSets)
    {
        this.fetchResultSets = fetchResultSets;
    }

/*
    public static SQLExecutionSettings loadFromStore(IPreferenceStore store)
    {
        SQLExecutionSettings settings = new SQLExecutionSettings();
        try {
            settings.setCommitType(CommitType.valueOf(store.getString(PrefConstants.SCRIPT_COMMIT_TYPE)));
            settings.setCommitLines(store.getInt(PrefConstants.SCRIPT_COMMIT_LINES));
            settings.setErrorHandling(ErrorHandling.valueOf(store.getString(PrefConstants.SCRIPT_ERROR_HANDLING)));
            settings.setFetchResultSets(store.getBoolean(PrefConstants.SCRIPT_FETCH_RESULT_SETS));
        } catch (IllegalArgumentException e) {
            // Do nothing
        }
        return settings;
    }

    public void saveToStore(IPreferenceStore store)
    {
    }

    public void clearStore(IPreferenceStore store)
    {
    }

*/
}
