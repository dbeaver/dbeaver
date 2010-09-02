/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.eclipse.ui.IEditorInput;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * IDatabaseEditorInput
 */
public interface IDatabaseEditorInput extends IEditorInput {

    DBPDataSource getDataSource();

    DBNNode getTreeNode();

    DBSObject getDatabaseObject();

    String getDefaultPageId();

}
