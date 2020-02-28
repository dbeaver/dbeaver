/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.search.data;

import org.eclipse.search.ui.ISearchResult;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.editors.data.AbstractDataEditor;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
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
    public void setInput(ISearchResult search, Object uiState) {
        foundObjects.clear();
        super.setInput(search, uiState);
    }

    @Override
    public void populateObjects(Collection<SearchDataObject> objects) {
        foundObjects.addAll(objects);
        super.populateObjects(objects);
    }

    private class DataSearchResultsControl extends SearchResultsControl {
        DataSearchResultsControl(Composite resultsGroup)
        {
            super(resultsGroup);

            setDoubleClickHandler(event -> {
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
                AbstractDataEditor.openNewDataEditor((DBNDatabaseNode) node, object.getFilter());
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
