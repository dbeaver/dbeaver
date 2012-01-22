/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSEntityAssociation
 */
public interface DBSEntityConstraint extends DBSObject {

    DBSEntityConstraintType getConstraintType();

    DBSEntity getParentObject();

}