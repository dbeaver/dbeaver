/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.layout;

/**
 * This is the most common form of an layouted object.
 */
public interface LayouterObject {
    
    /**
     * Translate the contents of the LayouterObject by same offset
     * @param dx x delta
     * @param dy y delta
     */
    void translate(int dx, int dy);
    
    /**
     * Get the contents that is wrapped by the LayouterObject. This is
     * typically a Fig but may by some other LayouterObject or a
     * Collection.
     */
    Object getContent();
}
