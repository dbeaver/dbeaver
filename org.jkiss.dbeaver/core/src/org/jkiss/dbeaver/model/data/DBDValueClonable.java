/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

/**
 * DBDContentClonable
 */
public interface DBDValueClonable extends DBDValue {

    /**
     * Makes exact copy of content object
     * @return copy
     */
    DBDValueClonable cloneValue();

}