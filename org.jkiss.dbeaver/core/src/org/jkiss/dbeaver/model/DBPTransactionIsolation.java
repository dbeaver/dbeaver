package org.jkiss.dbeaver.model;

/**
 * DBPTransactionIsolation
 */
public interface DBPTransactionIsolation
{
    boolean isEnabled();

    String getName();
}
