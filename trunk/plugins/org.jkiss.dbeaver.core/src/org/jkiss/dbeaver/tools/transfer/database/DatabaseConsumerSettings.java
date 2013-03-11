/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 * eugene.fradkin@gmail.com
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
package org.jkiss.dbeaver.tools.transfer.database;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferPipe;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DatabaseConsumerSettings
 */
public class DatabaseConsumerSettings implements IDataTransferSettings {

    private DBNDatabaseNode containerNode;
    private Map<DBSDataContainer, DatabaseMappingContainer> dataMappings = new LinkedHashMap<DBSDataContainer, DatabaseMappingContainer>();

    public DatabaseConsumerSettings()
    {
    }

    public DBSObjectContainer getContainer()
    {
        if (containerNode == null) {
            return null;
        }
        DBSObject object = containerNode.getObject();
        return object instanceof DBSObjectContainer ? (DBSObjectContainer) object :  null;
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

    @Override
    public void loadSettings(IDialogSettings dialogSettings)
    {
    }

    @Override
    public void saveSettings(IDialogSettings dialogSettings)
    {
    }

}
