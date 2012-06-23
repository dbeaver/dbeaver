/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.meta;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * Property value editor
 */
public interface IPropertyValueEditorProvider {

    CellEditor createCellEditor(Composite parent, Object object, Property property);

}
