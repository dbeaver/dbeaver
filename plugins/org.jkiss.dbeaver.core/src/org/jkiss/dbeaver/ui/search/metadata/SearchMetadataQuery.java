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
package org.jkiss.dbeaver.ui.search.metadata;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SearchMetadataQuery implements ISearchQuery {

    private static final Log log = Log.getLog(SearchMetadataQuery.class);

    private final DBSStructureAssistant structureAssistant;
    private final SearchMetadataParams params;
    private SearchMetadataResult searchResult;

    private SearchMetadataQuery(
        DBSStructureAssistant structureAssistant,
        SearchMetadataParams params)
    {
        this.structureAssistant = structureAssistant;
        this.params = params;
    }

    @Override
    public String getLabel()
    {
        return params.getObjectNameMask();
    }

    @Override
    public boolean canRerun() {
        return true;
    }

    @Override
    public boolean canRunInBackground() {
        return true;
    }

    @Override
    public ISearchResult getSearchResult() {
        if (searchResult == null) {
            searchResult = new SearchMetadataResult(this);
        }
        return searchResult;
    }

    @Override
    public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
        try {
            List<DBSObjectType> objectTypes = params.getObjectTypes();
            String objectNameMask = params.getObjectNameMask();

            if (params.getMatchType() == SearchMetadataConstants.MATCH_INDEX_STARTS_WITH) {
                if (!objectNameMask.endsWith("%")) { //$NON-NLS-1$
                    objectNameMask = objectNameMask + "%"; //$NON-NLS-1$
                }
            } else if (params.getMatchType() == SearchMetadataConstants.MATCH_INDEX_CONTAINS) {
                if (!objectNameMask.startsWith("%")) { //$NON-NLS-1$
                    objectNameMask = "%" + objectNameMask; //$NON-NLS-1$
                }
                if (!objectNameMask.endsWith("%")) { //$NON-NLS-1$
                    objectNameMask = objectNameMask + "%"; //$NON-NLS-1$
                }
            }

            DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
            DBRProgressMonitor localMonitor = RuntimeUtils.makeMonitor(monitor);
            Collection<DBSObjectReference> objects = structureAssistant.findObjectsByMask(
                localMonitor,
                params.getParentObject(),
                objectTypes.toArray(new DBSObjectType[objectTypes.size()]),
                objectNameMask,
                params.isCaseSensitive(),
                true,
                params.getMaxResults());
            for (DBSObjectReference reference : objects) {
                if (monitor.isCanceled()) {
                    break;
                }
                try {
                    DBSObject object = reference.resolveObject(localMonitor);
                    if (object != null) {
                        DBNNode node = navigatorModel.getNodeByObject(localMonitor, object, false);
                        if (node != null) {
                            searchResult.addObjects(Collections.singletonList(node));
                        }
                    }
                } catch (DBException e) {
                    log.error(e);
                }
            }
            return Status.OK_STATUS;
        } catch (DBException e) {
            return GeneralUtils.makeExceptionStatus(e);
        }
    }

    public static SearchMetadataQuery createQuery(
        DBPDataSource dataSource,
        SearchMetadataParams params)
        throws DBException
    {
        DBSStructureAssistant assistant = DBUtils.getAdapter(DBSStructureAssistant.class, dataSource);
        if (dataSource == null || assistant == null) {
            throw new DBException("Can't obtain database structure assistance from [" + dataSource + "]");
        }
        return new SearchMetadataQuery(assistant, params);
    }


}
