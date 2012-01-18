/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * Named object extension
 */
public interface DBSEntityQualified<ATTR extends DBSEntityAttribute> extends DBSEntity<ATTR> {

    /**
     * Entity full qualified name.
     * Should include all parent objects' names and thus uniquely identify this entity within database.
     * @return full qualified name, never returns null.
     */
    String getFullQualifiedName();

}
