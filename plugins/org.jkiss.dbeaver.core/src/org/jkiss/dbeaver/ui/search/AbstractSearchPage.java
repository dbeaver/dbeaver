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
package org.jkiss.dbeaver.ui.search;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public abstract class AbstractSearchPage extends DialogPage implements ISearchPage {

    static final protected Log log = Log.getLog(AbstractSearchPage.class);

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
            UIUtils.showErrorDialog(getShell(), "Search error", "Can't perform search", e);
            return false;
        }
        return true;
    }

    protected static List<DBNNode> loadTreeState(DBRProgressMonitor monitor, DBPPreferenceStore store, String propName)
    {
        final List<DBNNode> result = new ArrayList<>();
        final String sources = store.getString(propName);
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
