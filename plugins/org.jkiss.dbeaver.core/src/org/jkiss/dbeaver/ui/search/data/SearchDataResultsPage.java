/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.search.data;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.NavigatorUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetProvider;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.editors.data.DatabaseDataEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.search.AbstractSearchResultsPage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SearchDataResultsPage extends AbstractSearchResultsPage<SearchDataObject> {

    private List<SearchDataObject> foundObjects = new ArrayList<SearchDataObject>();

    @Override
    protected AbstractSearchResultsPage<SearchDataObject>.SearchResultsControl createResultControl(Composite parent) {
        return new DataSearchResultsControl(parent);
    }

    @Override
    protected DBNNode getNodeFromObject(SearchDataObject object) {
        return object.getNode();
    }

    @Override
    public void populateObjects(DBRProgressMonitor monitor, Collection<SearchDataObject> objects) {
        foundObjects.addAll(objects);
        super.populateObjects(monitor, objects);
    }

    private class DataSearchResultsControl extends SearchResultsControl {
        public DataSearchResultsControl(Composite resultsGroup)
        {
            super(resultsGroup);

            setDoubleClickHandler(new IDoubleClickListener() {
                @Override
                public void doubleClick(DoubleClickEvent event)
                {
                    // Run default node action
                    DBNNode node = NavigatorUtils.getSelectedNode(getItemsViewer());
                    if (!(node instanceof DBNDatabaseNode) || !node.allowsOpen()) {
                        return;
                    }
                    Object objectValue = getObjectValue(node);
                    if (!(objectValue instanceof SearchDataObject)) {
                        return;
                    }
                    SearchDataObject object = (SearchDataObject) objectValue;
                    IEditorPart entityEditor = NavigatorHandlerObjectOpen.openEntityEditor(
                        (DBNDatabaseNode) node,
                        DatabaseDataEditor.class.getName(),
                        DBeaverUI.getActiveWorkbenchWindow()
                    );
                    if (entityEditor instanceof EntityEditor) {
                        Object selectedPage = ((EntityEditor) entityEditor).getSelectedPage();
                        if (selectedPage instanceof ResultSetProvider) {
                            ResultSetViewer resultSetViewer = ((ResultSetProvider) selectedPage).getResultSetViewer();
                            resultSetViewer.setDataFilter(object.getFilter(), true);
                        }
                    }
                }
            });
        }

        @Override
        protected Class<?>[] getListBaseTypes(Collection<DBNNode> items) {
            return new Class<?>[] {DBPNamedObject.class, SearchDataObject.class};
        }

        @Override
        protected Object getObjectValue(DBNNode item) {
            for (SearchDataObject obj : foundObjects) {
                if (obj.getNode() == item) {
                    return obj;
                }
            }
            return item;
        }
    }

}
