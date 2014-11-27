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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSearcher;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.search.IObjectSearchListener;
import org.jkiss.dbeaver.ui.search.IObjectSearchQuery;

import java.util.HashSet;
import java.util.Set;

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

            //monitor.subTask("Collect tables");
            Set<DBPDataSource> dataSources = new HashSet<DBPDataSource>();
            for (DBSDataSearcher searcher : params.sources) {
                dataSources.add(searcher.getDataSource());
            }

            // Search
            long flags = 0;
            if (params.caseSensitive) flags |= DBSDataSearcher.FLAG_CASE_SENSITIVE;
            if (params.fastSearch) flags |= DBSDataSearcher.FLAG_FAST_SEARCH;
            if (params.searchNumbers) flags |= DBSDataSearcher.FLAG_SEARCH_NUMBERS;
            if (params.searchLOBs) flags |= DBSDataSearcher.FLAG_SEARCH_LOBS;
            int objectsFound = 0;
            monitor.beginTask(
                "Search \"" + searchString + "\" in " + params.sources.size() + " table(s) / " + dataSources.size() + " database(s)",
                params.sources.size());
            try {
                for (DBSDataSearcher searcher : params.sources) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    monitor.subTask(DBUtils.getObjectFullName(searcher));
                    SearchTableMonitor searchMonitor = new SearchTableMonitor();
                    DBCSession session = searcher.getDataSource().openSession(searchMonitor, DBCExecutionPurpose.UTIL, "Search rows");
                    try {
                        DBDDataReceiver dataReceiver = new TestDataReceiver();
                        searcher.findRows(session, dataReceiver, searchString, flags);
                    }
                    finally {
                        session.close();
                    }
                    if (objectsFound >= params.maxResults) {
                        break;
                    }

                    monitor.worked(1);
                }
/*
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
                monitor.done();
            }
        } finally {
            listener.searchFinished();
        }
    }

    public static SearchDataQuery createQuery(SearchDataParams params)
        throws DBException
    {
        return new SearchDataQuery(params);
    }

    private class SearchTableMonitor extends VoidProgressMonitor {

        private boolean canceled;

        private SearchTableMonitor() {
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }
    }

    private class TestDataReceiver implements DBDDataReceiver {

        @Override
        public void fetchStart(DBCSession session, DBCResultSet resultSet) throws DBCException {

        }

        @Override
        public void fetchRow(DBCSession session, DBCResultSet resultSet) throws DBCException {

        }

        @Override
        public void fetchEnd(DBCSession session) throws DBCException {

        }

        @Override
        public void close() {

        }
    }

}
