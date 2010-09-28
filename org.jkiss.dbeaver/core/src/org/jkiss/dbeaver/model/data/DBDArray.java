/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * Array
 *
 * @author Serge Rider
 */
public interface DBDArray extends DBDValue {

    DBSTypedObject getElementType();

    Object[] getValue()
        throws DBCException;

}