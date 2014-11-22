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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.search.IObjectSearchListener;
import org.jkiss.dbeaver.ui.search.IObjectSearchQuery;

public class SearchDataQuery implements IObjectSearchQuery {

    static final Log log = LogFactory.getLog(SearchDataQuery.class);

    private final SearchDataParams params;

    private SearchDataQuery(SearchDataParams params)
    {
        this.params = params;
    }

    @Override
    public String getLabel()
    {
        return params.getSearchString();
    }

    @Override
    public void runQuery(DBRProgressMonitor monitor, IObjectSearchListener listener)
        throws DBException
    {
        listener.searchStarted();
        try {
            String searchString = params.getSearchString();

/*
            DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
            Collection<DBSObjectReference> objects = structureAssistant.findObjectsByMask(
                monitor,
                params.getParentObject(),
                objectTypes.toArray(new DBSObjectType[objectTypes.size()]),
                searchString,
                params.isCaseSensitive(),
                params.getMaxResults());
            List<DBNNode> nodes = new ArrayList<DBNNode>();
            for (DBSObjectReference reference : objects) {
                if (monitor.isCanceled()) {
                    break;
                }
                try {
                    DBSObject object = reference.resolveObject(monitor);
                    if (object != null) {
                        DBNNode node = navigatorModel.getNodeByObject(monitor, object, false);
                        if (node != null) {
                            nodes.add(node);
                        }
                    }
                } catch (DBException e) {
                    log.error(e);
                }
            }
            if (!nodes.isEmpty()) {
                listener.objectsFound(monitor, nodes);
            }
*/
        } finally {
            listener.searchFinished();
        }
    }

    public static SearchDataQuery createQuery(SearchDataParams params)
        throws DBException
    {
        return new SearchDataQuery(params);
    }


}
