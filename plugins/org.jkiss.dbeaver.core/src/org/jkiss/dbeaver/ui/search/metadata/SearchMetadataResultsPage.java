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
package org.jkiss.dbeaver.ui.search.metadata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.Page;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.controls.itemlist.NodeListControl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

public class SearchMetadataResultsPage extends Page {

    static final Log log = LogFactory.getLog(SearchMetadataResultsPage.class);
    private IPageSite pageSite;
    private SearchResultsControl itemList;
    private IActionBars actionBars;

    @Override
	public IPageSite getSite() {
		return pageSite;
	}

	@Override
	public void init(IPageSite site) {
        this.pageSite = pageSite;
	}

	@Override
	public void createControl(Composite parent) {
        {
            itemList = new SearchResultsControl(parent);
            itemList.createProgressPanel();
            itemList.setInfo(CoreMessages.dialog_search_objects_item_list_info);
            GridData gd = new GridData(GridData.FILL_BOTH);
            itemList.setLayoutData(gd);
            //itemList.addFocusListener(new ItemsFocusListener());
        }
    }

	@Override
	public void dispose() {

	}

	@Override
	public Control getControl() {
		return itemList;
	}

	@Override
	public void setActionBars(IActionBars actionBars) {
        this.actionBars = actionBars;
	}

	@Override
	public void setFocus() {
        itemList.setFocus();
	}
        private class SearchResultsControl extends NodeListControl {
            public SearchResultsControl(Composite resultsGroup)
            {
                super(resultsGroup, SWT.BORDER, null, DBeaverCore.getInstance().getNavigatorModel().getRoot(), null);
            }

            @Override
            protected void fillCustomToolbar(ToolBarManager toolbarManager) {
            }

            public ObjectsLoadVisualizer createVisualizer()
            {
                return new ObjectsLoadVisualizer() {
                    @Override
                    public void completeLoading(Collection<DBNNode> items)
                    {
                        super.completeLoading(items);
                    }
                };
            }

            @Override
            protected LoadingJob<Collection<DBNNode>> createLoadService()
            {
                throw new UnsupportedOperationException();
/*
                DBNNode selectedNode = getSelectedNode();
                DBSObjectContainer parentObject = null;
                if (selectedNode instanceof DBSWrapper && ((DBSWrapper)selectedNode).getObject() instanceof DBSObjectContainer) {
                    parentObject = (DBSObjectContainer) ((DBSWrapper)selectedNode).getObject();
                }

                DBPDataSource dataSource = getSelectedDataSource();
                DBSStructureAssistant assistant = getSelectedStructureAssistant();
                if (dataSource == null || assistant == null) {
                    throw new IllegalStateException("No active datasource");
                }
                java.util.List<DBSObjectType> objectTypes = new ArrayList<DBSObjectType>();
                for (TableItem item : typesTable.getItems()) {
                    if (item.getChecked()) {
                        objectTypes.add((DBSObjectType) item.getData());
                    }
                }
                String objectNameMask = nameMask;

                // Save search query
                if (!searchHistory.contains(objectNameMask)) {
                    searchHistory.add(objectNameMask);
                    searchText.add(objectNameMask);
                }

                if (matchTypeIndex == SearchMetadataConstants.MATCH_INDEX_STARTS_WITH) {
                    if (!objectNameMask.endsWith("%")) { //$NON-NLS-1$
                        objectNameMask = objectNameMask + "%"; //$NON-NLS-1$
                    }
                } else if (matchTypeIndex == SearchMetadataConstants.MATCH_INDEX_CONTAINS) {
                    if (!objectNameMask.startsWith("%")) { //$NON-NLS-1$
                        objectNameMask = "%" + objectNameMask; //$NON-NLS-1$
                    }
                    if (!objectNameMask.endsWith("%")) { //$NON-NLS-1$
                        objectNameMask = objectNameMask + "%"; //$NON-NLS-1$
                    }
                }

                return LoadingUtils.createService(
                    new ObjectSearchService(dataSource, assistant, parentObject, objectTypes, objectNameMask, caseSensitive, maxResults),
                    itemList.createVisualizer());
*/
            }
        }

    private class ObjectSearchService extends DatabaseLoadService<Collection<DBNNode>> {

        private final DBSStructureAssistant structureAssistant;
        private final DBSObject parentObject;
        private final java.util.List<DBSObjectType> objectTypes;
        private final String objectNameMask;
        private final boolean caseSensitive;
        private final int maxResults;

        private ObjectSearchService(
            DBPDataSource dataSource,
            DBSStructureAssistant structureAssistant,
            DBSObject parentObject,
            java.util.List<DBSObjectType> objectTypes,
            String objectNameMask,
            boolean caseSensitive,
            int maxResults)
        {
            super("Find objects", dataSource);
            this.structureAssistant = structureAssistant;
            this.parentObject = parentObject;
            this.objectTypes = objectTypes;
            this.objectNameMask = objectNameMask;
            this.caseSensitive = caseSensitive;
            this.maxResults = maxResults;
        }

        @Override
        public Collection<DBNNode> evaluate()
            throws InvocationTargetException, InterruptedException
        {
            try {
                DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
                java.util.List<DBNNode> nodes = new ArrayList<DBNNode>();
                Collection<DBSObjectReference> objects = structureAssistant.findObjectsByMask(
                    getProgressMonitor(),
                    parentObject,
                    objectTypes.toArray(new DBSObjectType[objectTypes.size()]),
                    objectNameMask,
                    caseSensitive,
                    maxResults);
                for (DBSObjectReference reference : objects) {
                    try {
                        DBSObject object = reference.resolveObject(getProgressMonitor());
                        if (object != null) {
                            DBNNode node = navigatorModel.getNodeByObject(getProgressMonitor(), object, true);
                            if (node != null) {
                                nodes.add(node);
                            }
                        }
                    } catch (DBException e) {
                        log.error(e);
                    }
                }
                return nodes;
            } catch (Throwable ex) {
                if (ex instanceof InvocationTargetException) {
                    throw (InvocationTargetException)ex;
                } else {
                    throw new InvocationTargetException(ex);
                }
            }
        }
    }

}
