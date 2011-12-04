/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.eclipse.ui.IEditorInput;

/**
 * Nested editor input factory
 */
public interface IDatabaseEditorInputFactory {

    IEditorInput createNestedEditorInput(IDatabaseEditorInput mainInput);

}
