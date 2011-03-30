package org.jkiss.dbeaver.model.meta;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Property value editor
 */
public interface IPropertyValueEditor<OBJECT_TYPE extends DBSObject> {

    CellEditor createCellEditor(Composite parent, Property property);



}
