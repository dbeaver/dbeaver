/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.navigator.database;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.IDBNListener;

public class DatabaseNavigatorTree extends Composite implements IDBNListener
{
    static final Log log = LogFactory.getLog(DatabaseNavigatorTree.class);

    private TreeViewer viewer;
    private DBNModel model;

    public DatabaseNavigatorTree(Composite parent, DBNNode rootNode, int style)
    {
        super(parent, SWT.NONE);
        this.setLayout(new FillLayout());
        this.model = rootNode.getModel();
        this.model.addListener(this);
        addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                if (model != null) {
                    model.removeListener(DatabaseNavigatorTree.this);
                    model = null;
                }
            }
        });

        // Create tree
        // TODO: there are problems with this tree when we have a lot of items.
        // TODO: I may set SWT.SINGLE style and it'll solve the problem at least when traversing tree
        // TODO: But we need multiple selection (to copy, export, etc)
        // TODO: need to do something with it
        int treeStyle = SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | style;
        this.viewer = new TreeViewer(this, treeStyle);
        this.viewer.setUseHashlookup(true);
        this.viewer.setLabelProvider(new DatabaseNavigatorLabelProvider(this.viewer));
        this.viewer.setContentProvider(new DatabaseNavigatorContentProvider(this.viewer));
        this.viewer.setInput(rootNode);
    }

    public TreeViewer getViewer()
    {
        return viewer;
    }

    public void nodeChanged(final DBNEvent event)
    {
        switch (event.getAction()) {
            case ADD:
            case REMOVE:
                final DBNNode parentNode = event.getNode().getParentNode();
                if (parentNode != null) {
                    asyncExec(new Runnable() { public void run() {
                        if (!viewer.getControl().isDisposed()) {
                            if (!parentNode.isDisposed()) {
                                viewer.refresh(parentNode);
                            }
                        }
                    }});
                }
                break;
            case UPDATE:
                asyncExec(new Runnable() { public void run() {
                    if (!viewer.getControl().isDisposed()) {
                        if (event.getNode() != null) {
                            switch (event.getNodeChange()) {
                                case LOAD:
                                    viewer.expandToLevel(event.getNode(), 1);
                                    viewer.refresh(event.getNode());
                                    break;
                                case UNLOAD:
                                    viewer.collapseToLevel(event.getNode(), -1);
                                    viewer.refresh(event.getNode());
                                    break;
                                case REFRESH:
                                    viewer.update(event.getNode(), null);
                                    break;
                                case LOCK:
                                case UNLOCK:
                                    viewer.refresh(event.getNode());
                                    break;
                            }
                        } else {
                            log.warn("Null node object");
                        }
                    }
                }});
                break;
            default:
                break;
        }
    }

    private void asyncExec(Runnable runnable)
    {
        Display.getDefault().asyncExec(runnable);
    }

    public void showNode(DBNNode node) {
        viewer.reveal(node);
        viewer.setSelection(new StructuredSelection(node));
    }

}