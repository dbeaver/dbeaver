/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.search.data;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.search.AbstractSearchResultsPage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SearchDataResultsPage extends AbstractSearchResultsPage<SearchDataObject> {

    private List<SearchDataObject> foundObjects = new ArrayList<>();

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
                    ResultSetViewer.openNewDataEditor((DBNDatabaseNode) node, object.getFilter());
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
