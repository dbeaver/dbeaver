/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * Entity constraint
 */
public interface DBSEntityConstraint extends DBSObject {

    @Override
    DBSEntity getParentObject();

    DBSEntityConstraintType getConstraintType();

}