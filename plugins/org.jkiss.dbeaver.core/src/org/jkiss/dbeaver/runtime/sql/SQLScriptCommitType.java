/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.sql;

/**
 * SQLScriptCommitType
*/
public enum SQLScriptCommitType
{
    AT_END,
    AUTOCOMMIT,
    NLINES,
    NO_COMMIT;

    public static SQLScriptCommitType fromOrdinal(int ordinal)
    {
        for (SQLScriptCommitType ct : values()) {
            if (ct.ordinal() == ordinal) {
                return ct;
            }
        }
        return null;
    }
}
