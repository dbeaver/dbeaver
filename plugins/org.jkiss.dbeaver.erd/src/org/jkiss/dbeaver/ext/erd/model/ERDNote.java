/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * Simple text note
 */
public class ERDNote extends ERDObject<String> implements DBPNamedObject {

    public ERDNote(String text)
    {
        super(text);
    }

    public String getName()
    {
        return getObject();
    }
}
