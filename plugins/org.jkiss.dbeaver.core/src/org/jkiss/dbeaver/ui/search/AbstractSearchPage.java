/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.search;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public abstract class AbstractSearchPage extends DialogPage implements ISearchPage {

    static final protected Log log = Log.getLog(AbstractSearchPage.class);

    private static final Map<Class<? extends AbstractSearchPage>, String> searchStateCache = new IdentityHashMap<>();

    protected ISearchPageContainer container;

    protected AbstractSearchPage(String title) {
        super(title);
    }

    @Override
    public void setContainer(ISearchPageContainer container) {
        this.container = container;
    }

    @Override
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);
        if (visible) {
            updateEnablement();
        }
    }

    protected abstract void updateEnablement();

    protected abstract ISearchQuery createQuery() throws DBException;

    protected abstract void loadState(DBPPreferenceStore store);
    protected abstract void saveState(DBPPreferenceStore store);

    @Override
    public void createControl(Composite parent) {
        loadState(DBeaverCore.getGlobalPreferenceStore());
    }

    @Override
    public boolean performAction() {
        try {
            saveState(DBeaverCore.getGlobalPreferenceStore());
            NewSearchUI.runQueryInBackground(createQuery());
        } catch (DBException e) {
            DBUserInterface.getInstance().showError("Search error", "Can't perform search", e);
            return false;
        }
        return true;
    }

    protected void saveTreeState(DatabaseNavigatorTree tree)
    {
        // Object sources
        StringBuilder sourcesString = new StringBuilder();
        for (Object obj : ((CheckboxTreeViewer) tree.getViewer()).getCheckedElements()) {
            DBNNode node = (DBNNode) obj;
            if (node instanceof DBNDatabaseNode && ((DBNDatabaseNode) node).getObject() instanceof DBSDataContainer) {
                if (sourcesString.length() > 0) {
                    sourcesString.append("|"); //$NON-NLS-1$
                }
                sourcesString.append(node.getNodeItemPath());
            }
        }
        searchStateCache.put(getClass(), sourcesString.toString());
    }

    protected List<DBNNode> loadTreeState(DBRProgressMonitor monitor)
    {
        final List<DBNNode> result = new ArrayList<>();
        final String sources = searchStateCache.get(getClass());
        if (!CommonUtils.isEmpty(sources)) {
            // Keep broken datasources to make connect attempt only once
            Set<DBNDataSource> brokenDataSources = new HashSet<>();

            // Find all nodes
            StringTokenizer st = new StringTokenizer(sources, "|"); //$NON-NLS-1$
            while (st.hasMoreTokens()) {
                String nodePath = st.nextToken();
                try {
                    DBNDataSource dsNode = DBeaverCore.getInstance().getNavigatorModel().getDataSourceByPath(nodePath);
                    if (dsNode == null || brokenDataSources.contains(dsNode)) {
                        continue;
                    }

                    DBNNode node = DBeaverCore.getInstance().getNavigatorModel().getNodeByPath(monitor, dsNode.getOwnerProject(), nodePath);
                    if (node != null) {
                        result.add(node);
                    } else {
                        brokenDataSources.add(dsNode);
                    }
                } catch (DBException e) {
                    log.error(e);
                }
            }
        }
        return result;
    }


}
