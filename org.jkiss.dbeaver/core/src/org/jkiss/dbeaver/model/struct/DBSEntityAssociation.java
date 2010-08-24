/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSEntityAssociation
 */
public interface DBSEntityAssociation extends DBSObject {

    DBSEntity getAssociatedEntity();
    // target entity
    // identifying
    // multiplicity
    // bidirectional
}