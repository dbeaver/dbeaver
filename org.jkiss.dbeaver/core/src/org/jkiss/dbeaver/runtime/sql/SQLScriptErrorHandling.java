package org.jkiss.dbeaver.runtime.sql;

/**
 * SQLScriptErrorHandling
*/
public enum SQLScriptErrorHandling
{
    STOP_ROLLBACK,
    STOP_COMMIT,
    IGNORE;

    public static SQLScriptErrorHandling fromOrdinal(int ordinal)
    {
        for (SQLScriptErrorHandling eh : values()) {
            if (eh.ordinal() == ordinal) {
                return eh;
            }
        }
        return null;
    }
}
