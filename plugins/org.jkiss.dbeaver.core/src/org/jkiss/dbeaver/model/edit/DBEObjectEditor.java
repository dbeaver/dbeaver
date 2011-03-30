/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;

/**
 * Object editor
 */
public interface DBEObjectEditor <OBJECT_TYPE extends DBPObject> extends DBEObjectManager<OBJECT_TYPE> {

    DBEPropertyHandler<OBJECT_TYPE> makePropertyHandler(OBJECT_TYPE object, IPropertyDescriptor property);

}
