/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * Object with unique name.
 * Generally all objects have unique name (in context of their parent objects) but sometimes the name isn't unique.
 * For example stored procedures can be overridden, as a result multiple procedures have the same name.
 * Such objects may implements this interface to provide really unique name.
 * Unique name used in some operations like object tree refresh.
 */
public interface DBSObjectUnique extends DBSObject
{

    /**
     * Object's unique name
     *
     * @return object unique name
     */
    String getUniqueName();

}
