/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSEntityAttribute
 */
public interface DBSEntityAttribute extends DBSEntityElement, DBSColumnBase {

    boolean isRequired();

    boolean isSequence();

    String getDefaultValue();

}
