package org.jkiss.dbeaver.ext.erd.views;

import org.jgraph.graph.DefaultCellViewFactory;
import org.jgraph.graph.VertexView;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphCell;
import org.jkiss.dbeaver.ext.erd.model.ERDTable;

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
