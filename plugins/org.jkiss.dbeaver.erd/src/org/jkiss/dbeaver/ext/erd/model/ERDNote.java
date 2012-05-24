/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.model;

/**
 * Simple text note
 */
public class ERDNote extends ERDObject<String> {

    public ERDNote(String text)
    {
        super(text);
    }

    @Override
    public String getName()
    {
        return getObject();
    }
}
