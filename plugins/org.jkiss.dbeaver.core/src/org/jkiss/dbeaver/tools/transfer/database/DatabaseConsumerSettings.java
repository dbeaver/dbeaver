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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferPipe;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * DatabaseConsumerSettings
 */
public class DatabaseConsumerSettings implements IDataTransferSettings {

    enum MappingType {
        unspecified,
        existing,
        create,
        skip
    }

    static class ContainerMapping {
        DBSDataContainer source;
        DBSDataManipulator target;
        String targetName;
        MappingType mappingType;
        Map<DBSAttributeBase, AttributeMapping> attributeMappings = new HashMap<DBSAttributeBase, AttributeMapping>();

        public ContainerMapping(DBSDataContainer source)
        {
            this.source = source;
            this.mappingType = MappingType.unspecified;
        }

        public boolean isCompleted()
        {
            for (Map.Entry<DBSAttributeBase, AttributeMapping> attr : attributeMappings.entrySet()) {
                if (attr.getValue().mappingType == MappingType.unspecified) {
                    return false;
                }
            }
            return true;
        }

        public String getTargetName()
        {
            switch (mappingType) {
                case existing: return DBUtils.getObjectFullName(target);
                case create: return targetName;
                case skip: return "[skip]";
                default: return "?";
            }
        }
    }

    static class AttributeMapping {
        DBSAttributeBase source;
        DBSEntityAttribute target;
        String targetName;
        DBSDataType targetType;
        MappingType mappingType;

        AttributeMapping(DBSAttributeBase source)
        {
            this.source = source;
            this.mappingType = MappingType.unspecified;
        }

        public String getTargetName()
        {
            switch (mappingType) {
                case existing: return DBUtils.getObjectFullName(target);
                case create: return targetName;
                case skip: return "[skip]";
                default: return "?";
            }
        }
    }

    private DBNNode containerNode;
    private Map<DBSDataContainer, ContainerMapping> dataMappings = new HashMap<DBSDataContainer, ContainerMapping>();

    public DatabaseConsumerSettings()
    {
    }

    public DBNNode getContainerNode()
    {
        return containerNode;
    }

    public void setContainerNode(DBNNode containerNode)
    {
        this.containerNode = containerNode;
    }

    public Map<DBSDataContainer, ContainerMapping> getDataMappings()
    {
        return dataMappings;
    }

    public boolean isCompleted(Collection<DataTransferPipe> pipes)
    {
        for (DataTransferPipe pipe : pipes) {
            if (pipe.getProducer() != null) {
                DBSDataContainer sourceObject = (DBSDataContainer)pipe.getProducer().getSourceObject();
                ContainerMapping containerMapping = dataMappings.get(sourceObject);
                if (containerMapping == null ||
                    containerMapping.mappingType == MappingType.unspecified ||
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
