/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import org.jkiss.dbeaver.model.edit.DBECommandContext;

/**
 * Editable property source
 */
public interface IPropertySourceEditable {

    boolean isEditable(Object object);

    DBECommandContext getCommandContext();

//    void addPropertySourceListener(IPropertySourceListener listener);

//    void removePropertySourceListener(IPropertySourceListener listener);

}
