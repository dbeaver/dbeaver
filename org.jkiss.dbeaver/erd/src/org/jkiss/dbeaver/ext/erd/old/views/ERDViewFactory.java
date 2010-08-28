/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.views;

import org.jgraph.graph.DefaultCellViewFactory;
import org.jgraph.graph.VertexView;
import org.jkiss.dbeaver.ext.erd.old.model.ERDTable;

/**
 * ERDViewFactory
 */
public class ERDViewFactory extends DefaultCellViewFactory {

    protected VertexView createVertexView(Object v) {
        if (v instanceof ERDTable) {
            VertexView view = new ERDEntityView((ERDTable)v);
            view.setCell(v);
            return view;
        } else {
            return super.createVertexView(v);
        }
    }

}
