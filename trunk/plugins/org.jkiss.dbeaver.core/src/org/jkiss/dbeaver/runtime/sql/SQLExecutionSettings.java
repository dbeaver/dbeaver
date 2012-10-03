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
