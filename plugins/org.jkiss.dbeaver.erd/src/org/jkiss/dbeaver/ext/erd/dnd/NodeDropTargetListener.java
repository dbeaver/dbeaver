/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 14, 2004
 */
package org.jkiss.dbeaver.ext.erd.dnd;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.dnd.AbstractTransferDropTargetListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.CreationFactory;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.dnd.TreeNodeTransfer;

import java.util.Collection;

/**
 * Provides a listener for dropping nodes onto the editor drawing
 */
public class NodeDropTargetListener extends AbstractTransferDropTargetListener {

    public NodeDropTargetListener(EditPartViewer viewer)
    {
        super(viewer, TreeNodeTransfer.getInstance());
    }

    @Override
    protected void updateTargetRequest()
    {

    }

    protected Request createTargetRequest()
    {
        final Collection<DBNNode> object = TreeNodeTransfer.getInstance().getObject();

        CreateRequest request = new CreateRequest();
        request.setFactory(new CreationFactory() {
            public Object getNewObject()
            {
                return object;
            }

            public Object getObjectType()
            {
                return RequestConstants.REQ_CREATE;
            }
        });
        return request;
    }

}

