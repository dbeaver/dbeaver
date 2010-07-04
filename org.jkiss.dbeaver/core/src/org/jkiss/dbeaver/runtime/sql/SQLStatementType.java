/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.sql;

/**
 * Statement type
 */
public enum SQLStatementType {
    UNLKNOWN,
    SELECT,
    INSERT,
    DELETE,
    UPDATE,
    DDL
}
