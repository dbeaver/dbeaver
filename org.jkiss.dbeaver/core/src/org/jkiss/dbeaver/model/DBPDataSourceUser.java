package org.jkiss.dbeaver.model;

/**
 * DBPDataSourceUser
 */
public interface DBPDataSourceUser
{
    /**
     * Checks this users needs active connection.
     *
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    boolean needsConnection();

}