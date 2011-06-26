/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

/**
 * Table partition
 */
public class OracleTablePartition extends OraclePartitionBase<OracleTable> {
    protected OracleTablePartition(OracleTable oracleTable, String name, boolean persisted)
    {
        super(oracleTable, name, persisted);
    }
}
