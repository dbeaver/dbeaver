/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.indent;

public interface StopCondition
{
    /**
     * Instructs the scanner to return the current position.
     * 
     * @param ch the char at the current position
     * @param position the current position
     * @param forward the iteration direction
     * @return <code>true</code> if the stop condition is met.
     */
    boolean stop(char ch, int position, boolean forward);
}
