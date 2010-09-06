/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.navigator.database;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.views.navigator.database.load.TreeLoadService;
import org.jkiss.dbeaver.ui.views.navigator.database.load.TreeLoadVisualizer;
import org.jkiss.dbeaver.ui.views.navigator.database.load.TreeLoadNode;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * DatabaseNavigatorContentProvider
*/
class DatabaseNavigatorContentProvider implements IStructuredContentProvider, ITreeContentProvider
{
    static final Log log = LogFactory.getLog(DatabaseNavigatorContentProvider.class);

    private static final Object[] EMPTY_CHILDREN = new Object[0];

    private DatabaseNavigatorView view;

    DatabaseNavigatorContentProvider(DatabaseNavigatorView view)
    {
        this.view = view;
    }

    public void inputChanged(Viewer v, Object oldInput, Object newInput)
    {
    }

    public void dispose()
    {
    }

    public Object[] getElements(Object parent)
    {
        return getChildren(parent);
    }

    public Object getParent(Object child)
    {
        DBNNode node = (DBNNode)child;//view.getMetaModel().findNode(child);
        if (node == null || node.getParentNode() == null) {
            return null;
        }
        return node.getParentNode();//.getObject();
    }

    public Object[] getChildren(final Object parent)
    {
        if (parent instanceof TreeLoadNode) {
            return null;
        }
        if (!(parent instanceof DBNNode)) {
            log.error("Bad parent type: " + parent);
            return null;
        }
        final DBNNode parentNode = (DBNNode)parent;//view.getMetaModel().findNode(parent);
/*
        if (parentNode == null) {
            log.error("Can't find parent node '" + ((DBSObject) parent).getName() + "' in model");
            return EMPTY_CHILDREN;
        }
*/
        if (!parentNode.hasNavigableChildren()) {
            return EMPTY_CHILDREN;
        }
        if (parentNode.isLazyNode()) {
            return TreeLoadVisualizer.expandChildren(
                view.getViewer(),
                new TreeLoadService("Loading", parentNode));
        } else {
            try {
                // Read children with null monitor cos' it's not a lazy node
                // and no blocking prooccess will occure
                List<? extends DBNNode> children = parentNode.getChildren(VoidProgressMonitor.INSTANCE);
                if (CommonUtils.isEmpty(children)) {
                    return new Object[0];
                } else {
                    return children.toArray();
                }
                //return DBNNode.convertNodesToObjects(children);
            }
            catch (Throwable ex) {
                if (ex instanceof InvocationTargetException) {
                    ex = ((InvocationTargetException)ex).getTargetException();
                }
                DBeaverUtils.showErrorDialog(
                    view.getSite().getShell(),
                    "Navigator error",
                    ex.getMessage(),
                    ex);
                // Collapse this item
                view.getSite().getShell().getDisplay().asyncExec(new Runnable() {
                    public void run()
                    {
                        view.getViewer().collapseToLevel(parent, 1);
                        view.getViewer().refresh(parent);
                    }
                });
                return EMPTY_CHILDREN;
            }
        }
    }

    public boolean hasChildren(Object parent)
    {
        return parent instanceof DBNNode && ((DBNNode) parent).hasNavigableChildren();
    }

/*
    public void cancelLoading(Object parent)
    {
        if (!(parent instanceof DBSObject)) {
            log.error("Bad parent type: " + parent);
        }
        DBSObject object = (DBSObject)parent;
        object.getDataSource().cancelCurrentOperation();
    }
*/

}
