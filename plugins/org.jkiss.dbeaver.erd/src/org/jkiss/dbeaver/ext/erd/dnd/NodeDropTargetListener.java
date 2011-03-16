/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 14, 2004
 */
package org.jkiss.dbeaver.ext.erd.dnd;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.dnd.AbstractTransferDropTargetListener;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.CreationFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.erd.model.DiagramObjectCollector;
import org.jkiss.dbeaver.ext.erd.model.ERDTable;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.dnd.DatabaseObjectTransfer;
import org.jkiss.dbeaver.ui.dnd.TreeNodeTransfer;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides a listener for dropping nodes onto the editor drawing
 */
public class NodeDropTargetListener extends AbstractTransferDropTargetListener {

    static final Log log = LogFactory.getLog(NodeDropTargetListener.class);

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
        CreateRequest request = new CreateRequest();
        request.setFactory(new CreationFactory() {
            public Object getNewObject()
            {
                Collection<DBSObject> objects = DatabaseObjectTransfer.getInstance().getObject();
                if (objects == null) {
                    final Collection<DBNNode> nodes = TreeNodeTransfer.getInstance().getObject();
                    if (CommonUtils.isEmpty(nodes)) {
                        return null;
                    }
                    List<DBSObject> tmpObjects = new ArrayList<DBSObject>();
                    for (DBNNode node : nodes) {
                        if (node instanceof DBNDatabaseNode && ((DBNDatabaseNode) node).getObject() != null) {
                            tmpObjects.add(((DBNDatabaseNode) node).getObject());
                        }
                    }
                    objects = tmpObjects;
                }
                final Collection<DBSObject> roots = objects;

                final List<ERDTable> tables = new ArrayList<ERDTable>();

                try {
                    //ERDEditorPart editor = ((ERDGraphicalViewer) getViewer()).getEditor();

                    DBeaverCore.getInstance().runInProgressService(new DBRRunnableWithProgress() {
                        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                        {
                            DiagramObjectCollector collector = new DiagramObjectCollector(
                                ((DiagramPart) getViewer().getRootEditPart().getContents()).getDiagram());
                            try {
                                collector.generateDiagramObjects(monitor, roots);
                            } catch (DBException e) {
                                throw new InvocationTargetException(e);
                            }
                            tables.addAll(collector.getDiagramTables());
                        }
                    });
                } catch (InvocationTargetException e) {
                    log.error(e.getTargetException());
                } catch (InterruptedException e) {
                    // interrupted
                }

                return tables;
            }

            public Object getObjectType()
            {
                return RequestConstants.REQ_CREATE;
            }
        });
        request.setLocation(getDropLocation());
        return request;
    }

}

