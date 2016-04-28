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
package org.jkiss.dbeaver.tools.transfer.database;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.Log;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferSettings;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DatabaseConsumerSettings
 */
public class DatabaseConsumerSettings implements IDataTransferSettings {

    private static final Log log = Log.getLog(DatabaseConsumerSettings.class);

    private String containerNodePath;
    private DBNDatabaseNode containerNode;
    private Map<DBSDataContainer, DatabaseMappingContainer> dataMappings = new LinkedHashMap<>();
    private boolean openNewConnections = true;
    private boolean useTransactions = true;
    private int commitAfterRows = 10000;
    private boolean openTableOnFinish = true;

    public DatabaseConsumerSettings()
    {
    }

    @Nullable
    public DBSObjectContainer getContainer()
    {
        if (containerNode == null) {
            return null;
        }
        return DBUtils.getAdapter(DBSObjectContainer.class, containerNode.getObject());
    }

    public DBNDatabaseNode getContainerNode()
    {
        return containerNode;
    }

    public void setContainerNode(DBNDatabaseNode containerNode)
    {
        this.containerNode = containerNode;
    }

    public Map<DBSDataContainer, DatabaseMappingContainer> getDataMappings()
    {
        return dataMappings;
    }

    public DatabaseMappingContainer getDataMapping(DBSDataContainer dataContainer)
    {
        return dataMappings.get(dataContainer);
    }

    public boolean isCompleted(Collection<DataTransferPipe> pipes)
    {
        for (DataTransferPipe pipe : pipes) {
            if (pipe.getProducer() != null) {
                DBSDataContainer sourceObject = (DBSDataContainer)pipe.getProducer().getSourceObject();
                DatabaseMappingContainer containerMapping = dataMappings.get(sourceObject);
                if (containerMapping == null ||
                    containerMapping.getMappingType() == DatabaseMappingType.unspecified ||
                    !containerMapping.isCompleted())
                {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isOpenTableOnFinish()
    {
        return openTableOnFinish;
    }

    public void setOpenTableOnFinish(boolean openTableOnFinish)
    {
        this.openTableOnFinish = openTableOnFinish;
    }

    public boolean isOpenNewConnections()
    {
        return openNewConnections;
    }

    public void setOpenNewConnections(boolean openNewConnections)
    {
        this.openNewConnections = openNewConnections;
    }

    public boolean isUseTransactions()
    {
        return useTransactions;
    }

    public void setUseTransactions(boolean useTransactions)
    {
        this.useTransactions = useTransactions;
    }

    public int getCommitAfterRows()
    {
        return commitAfterRows;
    }

    public void setCommitAfterRows(int commitAfterRows)
    {
        this.commitAfterRows = commitAfterRows;
    }

    @Nullable
    DBPDataSource getTargetDataSource(DatabaseMappingObject attrMapping)
    {
        DBSObjectContainer container = getContainer();
        if (container != null) {
            return container.getDataSource();
        } else if (attrMapping.getTarget() != null) {
            return attrMapping.getTarget().getDataSource();
        } else {
            return null;
        }
    }

    @Override
    public void loadSettings(IRunnableContext runnableContext, DataTransferSettings dataTransferSettings, IDialogSettings dialogSettings)
    {
        containerNodePath = dialogSettings.get("container");
        if (dialogSettings.get("openNewConnections") != null) {
            openNewConnections = dialogSettings.getBoolean("openNewConnections");
        }
        if (dialogSettings.get("useTransactions") != null) {
            useTransactions = dialogSettings.getBoolean("useTransactions");
        }
        if (dialogSettings.get("commitAfterRows") != null) {
            commitAfterRows = dialogSettings.getInt("commitAfterRows");
        }
        if (dialogSettings.get("openTableOnFinish") != null) {
            openTableOnFinish = dialogSettings.getBoolean("openTableOnFinish");
        }
        {
            List<DataTransferPipe> dataPipes = dataTransferSettings.getDataPipes();
            if (!dataPipes.isEmpty()) {
                IDataTransferConsumer consumer = dataPipes.get(0).getConsumer();
                if (consumer instanceof DatabaseTransferConsumer) {
                    final DBSDataManipulator targetObject = ((DatabaseTransferConsumer) consumer).getTargetObject();
                    if (targetObject != null) {
                        containerNode = DBeaverCore.getInstance().getNavigatorModel().findNode(
                            targetObject.getParentObject()
                        );
                    }
                }
            }
        }
    }

    @Override
    public void saveSettings(IDialogSettings dialogSettings)
    {
        if (containerNode != null) {
            dialogSettings.put("container", containerNode.getNodeItemPath());
        }
        dialogSettings.put("openNewConnections", openNewConnections);
        dialogSettings.put("useTransactions", useTransactions);
        dialogSettings.put("commitAfterRows", commitAfterRows);
        dialogSettings.put("openTableOnFinish", openTableOnFinish);
    }

    @NotNull
    public String getContainerFullName()
    {
        DBSObjectContainer container = getContainer();
        return container == null ? "" :
            container instanceof DBPDataSource ?
                DBUtils.getObjectFullName(container) :
                DBUtils.getObjectFullName(container) + " [" + container.getDataSource().getContainer().getName() + "]";
    }

    public void loadNode() {
        if (containerNode == null && !CommonUtils.isEmpty(containerNodePath)) {
            if (!CommonUtils.isEmpty(containerNodePath)) {
                try {
                    DBeaverUI.runInProgressDialog(new DBRRunnableWithProgress() {
                        @Override
                        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                        {
                            try {
                                DBNNode node = DBeaverCore.getInstance().getNavigatorModel().getNodeByPath(
                                    monitor,
                                    containerNodePath);
                                if (node instanceof DBNDatabaseNode) {
                                    containerNode = (DBNDatabaseNode) node;
                                }
                            } catch (DBException e) {
                                throw new InvocationTargetException(e);
                            }
                        }
                    });
                } catch (InvocationTargetException e) {
                    log.error("Error getting container node", e.getTargetException());
                }
            }
        }
    }
}
