/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Object editor
 */
public interface JDBCNestedEditor<OBJECT_TYPE extends DBSObject> {

    String getNestedDeclaration(DBPObject owner, JDBCObjectEditor.ObjectChangeCommand<OBJECT_TYPE> command);

}
