/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

/**
 * Embedded workbench part
 */
public interface IEmbeddedPart
{
    /**
     * Activates editor part.
     * Called when part becomes active (visible).
     */
    void activatePart();

    /**
     * Deactivates editor part.
     * Called when part becomes inactive (invisible).
     */
    void deactivatePart();
    
    /**
     * Refreshes editor part content.
     */
    //void refreshPart();

}
