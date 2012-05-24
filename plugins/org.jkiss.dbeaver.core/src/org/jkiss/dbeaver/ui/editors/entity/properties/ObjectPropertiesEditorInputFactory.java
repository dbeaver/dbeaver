/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity.properties;

import org.eclipse.ui.IEditorInput;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.ext.IDatabaseEditorInputFactory;

/**
 * ObjectPropertiesEditorInputFactory
 */
public class ObjectPropertiesEditorInputFactory implements IDatabaseEditorInputFactory {

    @Override
    public IEditorInput createNestedEditorInput(IDatabaseEditorInput mainInput)
    {
        return mainInput;// new ObjectPropertiesEditorInput(mainInput);
    }

}
