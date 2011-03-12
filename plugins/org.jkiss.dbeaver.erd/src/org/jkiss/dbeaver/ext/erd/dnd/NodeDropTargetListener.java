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
import org.jkiss.dbeaver.ext.erd.model.ERDTable;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.ui.dnd.TreeNodeTransfer;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

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
                final Collection<DBNNode> nodes = TreeNodeTransfer.getInstance().getObject();
                if (CommonUtils.isEmpty(nodes)) {
                    return null;
                }

                final List<ERDTable> tables = new ArrayList<ERDTable>();

                try {
                    DBeaverCore.getInstance().runInProgressService(new DBRRunnableWithProgress() {
                        EntityDiagram diagram;
                        Map<DBSTable, ERDTable> tableMap = new HashMap<DBSTable, ERDTable>();
                        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                        {
                            diagram = ((DiagramPart) getViewer().getRootEditPart().getContents()).getDiagram();
                            tableMap.putAll(diagram.getTableMap());

                            for (DBNNode node : nodes) {
                                if (monitor.isCanceled()) {
                                    break;
                                }
                                if (node instanceof DBNDatabaseNode) {
                                    final DBSObject object = ((DBNDatabaseNode) node).getObject();
                                    if (object instanceof DBSTable) {
                                        addTable(monitor, (DBSTable) object);
                                    } else if (object instanceof DBSEntityContainer) {
                                        try {
                                            final Collection<? extends DBSEntity> children = ((DBSEntityContainer) object).getChildren(monitor);
                                            if (!CommonUtils.isEmpty(children)) {
                                                for (DBSEntity entity : children) {
                                                    if (entity instanceof DBSTable) {
                                                        addTable(monitor, (DBSTable)entity);
                                                    }
                                                }
                                            }
                                        } catch (DBException e) {
                                            throw new InvocationTargetException(e);
                                        }
                                    }
                                }
                            }

                            // Add new relations
                            for (ERDTable erdTable : tables) {
                                erdTable.addRelations(monitor, tableMap, false);
                            }
                        }

                        private void addTable(DBRProgressMonitor monitor, DBSTable object)
                        {
                            DBSTable table = (DBSTable) object;
                            if (diagram.containsTable(table)) {
                                // Avoid duplicates
                                return;
                            }
                            ERDTable erdTable = ERDTable.fromObject(monitor, table);
                            tables.add(erdTable);
                            tableMap.put(table, erdTable);
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

