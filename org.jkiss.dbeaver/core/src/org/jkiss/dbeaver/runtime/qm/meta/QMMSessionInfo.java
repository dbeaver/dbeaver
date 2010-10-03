/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Data source information
 */
public class QMMSessionInfo {

    private String containerId;
    private long openTime;
    private long closeTime;

    private Set<QMMStatementInfo> activeStatements = new TreeSet<QMMStatementInfo>();
    private List<QMMStatementInfo> statements;
    private List<QMMTransactionInfo> transactions;

}
