/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.navigator.database;

import org.jkiss.dbeaver.model.navigator.DBNNode;

/**
 * Tree content
 */
class DatabaseNavigatorContent {

    private final DBNNode rootNode;

    DatabaseNavigatorContent(DBNNode rootNode)
    {
        this.rootNode = rootNode;
    }

    public DBNNode getRootNode()
    {
        return rootNode;
    }
}
