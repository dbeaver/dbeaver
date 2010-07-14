package org.jkiss.dbeaver.ext.erd.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgraph.graph.DefaultGraphModel;

/**
 * ERDModel
 */
public class ERDModel extends DefaultGraphModel {

    static final Log log = LogFactory.getLog(ERDModel.class);

    public ERDModel()
    {
    }

/*
    public void addRoot(DBSObject object)
    {
        ERDNode node = null;
        if (object instanceof DBSTable) {
            node = new ERDTable((DBSTable)object);
        } else if (object instanceof DBSSchema) {
            node = new ERDSchema((DBSSchema)object);
        } else {
            log.warn("Unsupported object type: " + object);
        }
        if (node != null) {
            // Set black border
            GraphConstants.setBorderColor(node.getAttributes(), Color.black);
            GraphConstants.setBackground(node.getAttributes(), awtTableBackground);
            GraphConstants.setOpaque(node.getAttributes(), true);
            // Add a Floating Port
            node.addPort();

            roots.add(node);
        }
    }

    public void addRootObject(Object object)
    {
        roots.add(object);
    }

    public ERDNode getNode(DBSObject object)
    {
        for (Object modelObject : roots) {
            if (modelObject instanceof ERDNode) {
                if (((ERDNode) modelObject).getUserObject() == object) {
                    return (ERDNode) modelObject;
                }
            }
        }
        return null;
    }
*/
}
