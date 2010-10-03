/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

/**
 * QM Savepoint info
 */
public class QMMSavePointInfo {

    private QMMTransactionInfo transaction;
    private long startTime;
    private String name;
    private boolean finished;
    private boolean commited;
    private QMMSavePointInfo previous;

}