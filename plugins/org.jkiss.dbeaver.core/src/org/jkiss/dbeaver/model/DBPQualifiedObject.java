/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

/**
 * Named object extension
 */
public interface DBPQualifiedObject
{

    /**
     * Entity full qualified name.
     * Should include all parent objects' names and thus uniquely identify this entity within database.
     * @return full qualified name, never returns null.
     */
    String getFullQualifiedName();

}
