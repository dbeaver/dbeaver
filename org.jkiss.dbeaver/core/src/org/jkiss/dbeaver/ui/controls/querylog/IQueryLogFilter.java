/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.querylog;

import org.jkiss.dbeaver.runtime.qm.QMMetaEvent;

/**
 * Query log filter
 */
public interface IQueryLogFilter {

    boolean accept(QMMetaEvent event);

}
