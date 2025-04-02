/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.navigator.breadcrumb;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.LocalCacheProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.controls.breadcrumb.BreadcrumbViewer;
import org.jkiss.utils.ArrayUtils;

/**
 * A {@link DBNNode}-oriented specialization of {@link BreadcrumbViewer}.
 */
public class NodeBreadcrumbViewer extends BreadcrumbViewer {
    private static final Log log = Log.getLog(NodeBreadcrumbViewer.class);

    public NodeBreadcrumbViewer(@NotNull Composite parent, int style) {
        super(parent, style);

        setLabelProvider(new BreadcrumbNodeLabelProvider());
        setContentProvider(new BreadcrumbNodeContentProvider(false));
        setDropDownContentProvider(new BreadcrumbNodeContentProvider(true));

        addOpenListener(e -> openEditor(e.getSelection()));
        addDoubleClickListener(e -> openEditor(e.getSelection()));
    }

    private static void openEditor(@NotNull ISelection selection) {
        if (selection instanceof IStructuredSelection ss && ss.getFirstElement() instanceof DBNNode node) {
            DBWorkbench.getPlatformUI().openEntityEditor(node, null);
        }
    }

    private static class BreadcrumbNodeLabelProvider extends LabelProvider {
        @Override
        public Image getImage(Object element) {
            return DBeaverIcons.getImage(((DBNNode) element).getNodeIconDefault());
        }

        @Override
        public String getText(Object element) {
            return ((DBNNode) element).getNodeDisplayName();
        }
    }

    private record BreadcrumbNodeContentProvider(boolean allowFoldersOnly) implements ITreeContentProvider {
        @Override
        public Object[] getElements(Object inputElement) {
            DBNNode child = (DBNNode) inputElement;
            DBNNode parent = child.getParentNode();
            if (parent != null) {
                return getChildren(parent);
            }
            return new Object[0];
        }

        @Override
        public Object getParent(Object element) {
            DBNNode child = (DBNNode) element;
            if (child instanceof DBNDataSource) {
                return null; // don't show anything below data sources
            }

            DBNNode parent = child.getParentNode();
            while (parent instanceof DBNDatabaseFolder) {
                parent = parent.getParentNode(); // skip folder nodes
            }

            return parent;
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            var children = getCachedChildren((DBNNode) parentElement);
            if (children != null) {
                return children;
            }
            return new Object[0];
        }

        @Override
        public boolean hasChildren(Object element) {
            if (!allowFoldersOnly || element instanceof DBNLocalFolder) {
                return !ArrayUtils.isEmpty(getCachedChildren((DBNNode) element));
            } else {
                return false;
            }
        }

        @Nullable
        private static DBNNode[] getCachedChildren(@NotNull DBNNode parent) {
            try {
                return parent.getChildren(new LocalCacheProgressMonitor(new VoidProgressMonitor()));
            } catch (DBException e) {
                log.error("Error getting children", e); // should not happen
                return null;
            }
        }
    }
}
