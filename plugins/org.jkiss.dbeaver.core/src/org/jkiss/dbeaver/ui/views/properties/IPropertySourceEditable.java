/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import org.jkiss.dbeaver.model.edit.DBECommandContext;

/**
 * Editable property source
 */
public interface IPropertySourceEditable {

    boolean isEditable();

    DBECommandContext getCommandContext();
}
