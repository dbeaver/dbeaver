/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.querylog;

/**
 * Query log filter
 */
public interface IQueryLogFilter {

    boolean accept(Object event);

}
