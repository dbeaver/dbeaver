/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.layout;

import java.util.List;

/**
 * This interface is for container in a layouted diagram. They are
 * intended to hold other object like nodes or even other containers.
 * An example are nested packages in classdiagrams.
 */
public interface LayouterContainer {

    /**
     * Get all the objects from this container.
     *
     * @return All the objects from this container.
     */
    public List getObjects();

    /**
     * Resize this container, so it fits the layouted objects within itself.
     *
     * @param newSize represents The new size of this container.
     */
    public void resize();
}
