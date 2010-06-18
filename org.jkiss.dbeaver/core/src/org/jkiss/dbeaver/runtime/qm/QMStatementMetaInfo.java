/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm;

import java.util.Map;

/**
 * DBCStatement meta info
 */
public class QMStatementMetaInfo {
    
    private QMTransactionMetaInfo transaction;
    private String queryString;
    private long openTime;
    private long closeTime;
    private long fetchRowCount;
    private long updateRowCount;
    private Throwable executionError;
    private Map<Object, Object> parameters;

}
