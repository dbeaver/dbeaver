/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.eclipse.ui.IEditorInput;
import org.jkiss.dbeaver.model.edit.DBEObjectCommander;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * IDatabaseNodeEditorInput
 */
public interface IDatabaseNodeEditorInput extends IEditorInput, IDataSourceProvider {

    DBNDatabaseNode getTreeNode();

    DBSObject getDatabaseObject();

    String getDefaultPageId();

    DBEObjectCommander getObjectCommander();
}
