/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.ui.search.database;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.ui.search.IObjectSearchListener;
import org.jkiss.dbeaver.ui.search.IObjectSearchQuery;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SearchDatabaseObjectsQuery implements IObjectSearchQuery {

    static final Log log = LogFactory.getLog(SearchDatabaseObjectsDialog.class);

    private final DBSStructureAssistant structureAssistant;
    private final SearchDatabaseObjectsParams params;

    private SearchDatabaseObjectsQuery(
        DBSStructureAssistant structureAssistant,
        SearchDatabaseObjectsParams params)
    {
        this.structureAssistant = structureAssistant;
        this.params = params;
    }

    @Override
    public void runQuery(DBRProgressMonitor monitor, IObjectSearchListener listener)
        throws InvocationTargetException, InterruptedException
    {
        try {
            List<DBSObjectType> objectTypes = params.getObjectTypes();
            String objectNameMask = params.getObjectNameMask();

            if (params.getMatchType() == SearchDatabaseConstants.MATCH_INDEX_STARTS_WITH) {
                if (!objectNameMask.endsWith("%")) { //$NON-NLS-1$
                    objectNameMask = objectNameMask + "%"; //$NON-NLS-1$
                }
            } else if (params.getMatchType() == SearchDatabaseConstants.MATCH_INDEX_CONTAINS) {
                if (!objectNameMask.startsWith("%")) { //$NON-NLS-1$
                    objectNameMask = "%" + objectNameMask; //$NON-NLS-1$
                }
                if (!objectNameMask.endsWith("%")) { //$NON-NLS-1$
                    objectNameMask = objectNameMask + "%"; //$NON-NLS-1$
                }
            }

            DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
            Collection<DBSObjectReference> objects = structureAssistant.findObjectsByMask(
                monitor,
                params.getParentObject(),
                objectTypes.toArray(new DBSObjectType[objectTypes.size()]),
                objectNameMask,
                params.isCaseSensitive(),
                params.getMaxResults());
            for (DBSObjectReference reference : objects) {
                try {
                    DBSObject object = reference.resolveObject(monitor);
                    if (object != null) {
                        DBNNode node = navigatorModel.getNodeByObject(monitor, object, true);
                        if (node != null) {
                            listener.objectsFound(Collections.singleton(node));
                        }
                    }
                } catch (DBException e) {
                    log.error(e);
                }
            }
        } catch (Throwable ex) {
            if (ex instanceof InvocationTargetException) {
                throw (InvocationTargetException) ex;
            } else {
                throw new InvocationTargetException(ex);
            }
        }
    }

    public static SearchDatabaseObjectsQuery createQuery(
        DBPDataSource dataSource,
        SearchDatabaseObjectsParams params)
        throws DBException
    {
        DBSStructureAssistant assistant = DBUtils.getAdapter(DBSStructureAssistant.class, dataSource);
        if (dataSource == null || assistant == null) {
            throw new DBException("Can't obtain database structure assistance from [" + dataSource + "]");
        }
        return new SearchDatabaseObjectsQuery(assistant, params);
    }


}
