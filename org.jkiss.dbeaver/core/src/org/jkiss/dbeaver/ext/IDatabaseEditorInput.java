/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.eclipse.ui.IEditorInput;
import org.jkiss.dbeaver.model.meta.DBMModel;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * IDatabaseEditorInput
 */
public interface IDatabaseEditorInput extends IEditorInput {

    DBMNode getTreeNode();

    DBSObject getDatabaseObject();

    String getDefaultPageId();
}
