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
public class QMMSessionInfo extends QMMObject {

    private String containerId;
    private long openTime;
    private long closeTime;

    private QMMStatementInfo statement;
    private QMMTransactionInfo transaction;

}
