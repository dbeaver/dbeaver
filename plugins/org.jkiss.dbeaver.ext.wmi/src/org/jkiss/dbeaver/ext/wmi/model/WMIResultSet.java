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
package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.data.DBDValueMeta;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.utils.CommonUtils;
import org.jkiss.wmi.service.*;

import java.util.*;

/**
 * WMI result set
 */
public class WMIResultSet implements DBCResultSet, DBCResultSetMetaData, DBCEntityMetaData
{
    private DBCSession session;
    private WMIClass classObject;
    private Collection<WMIObject> rows;
    private Iterator<WMIObject> iterator;
    private WMIObject row;
    private List<DBCAttributeMetaData> properties;

    public WMIResultSet(DBCSession session, WMIClass classObject, Collection<WMIObject> rows) throws WMIException
    {
        this.session = session;
        this.classObject = classObject;
        this.rows = rows;
        this.iterator = rows.iterator();
        this.row = null;
        {
            // Init meta properties
            WMIObject metaObject;
            if (classObject != null) {
                metaObject = classObject.getClassObject();
            } else if (!rows.isEmpty()) {
                metaObject = rows.iterator().next();
            } else {
                metaObject = null;
            }
            if (metaObject == null) {
                properties = Collections.emptyList();
            } else {
                Collection<WMIObjectAttribute> props = metaObject.getAttributes(WMIConstants.WBEM_FLAG_ALWAYS);
                properties = new ArrayList<>(props.size());
                int index = 0;
                for (WMIObjectAttribute prop : props) {
                    if (!prop.isSystem()) {
                        properties.add(new MetaProperty(prop, index++));
                    }
                }
            }
        }

    }

    @Override
    public DBCSession getSession()
    {
        return session;
    }

    @Override
    public DBCStatement getSourceStatement()
    {
        return null;
    }

    @Override
    public Object getAttributeValue(int index) throws DBCException
    {
        try {
            if (index >= properties.size()) {
                throw new DBCException("Column index " + index + " out of bounds (" + properties.size() + ")");
            }
            return row.getValue(properties.get(index).getName());
        } catch (WMIException e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

    @Nullable
    @Override
    public Object getAttributeValue(String name) throws DBCException {
        try {
            return row.getValue(name);
        } catch (WMIException e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

    @Override
    public DBDValueMeta getAttributeValueMeta(int index) throws DBCException
    {
        return null;
    }

    @Override
    public DBDValueMeta getRowMeta() throws DBCException
    {
        try {
            Collection<WMIQualifier> qualifiers = row.getQualifiers();
            if (CommonUtils.isEmpty(qualifiers)) {
                return null;
            }
            return new WMIValueMeta(qualifiers);
        } catch (WMIException e) {
            throw new DBCException("Can't read value qualifiers");
        }
    }

    @Override
    public boolean nextRow() throws DBCException
    {
        if (!this.iterator.hasNext()) {
            return false;
        }
        row = iterator.next();
        return true;
    }

    @Override
    public boolean moveTo(int position) throws DBCException
    {
        throw new DBCException("Not Implemented");
    }

    @NotNull
    @Override
    public DBCResultSetMetaData getMeta() throws DBCException
    {
        return this;
    }

    @Override
    public String getResultSetName() throws DBCException {
        return null;
    }

    @Override
    public void close()
    {
        for (WMIObject row : rows) {
            row.release();
        }
        rows.clear();
        row = null;
    }

    /////////////////////////////////////////////////////////////
    // DBCResultSetMetaData

    @NotNull
    @Override
    public List<DBCAttributeMetaData> getAttributes()
    {
        return properties;
    }

    /////////////////////////////////////////////////////////////
    // DBCTableMetaData

    @Nullable
    @Override
    public String getCatalogName() {
        return null;
    }

    @Nullable
    @Override
    public String getSchemaName() {
        return null;
    }

    @NotNull
    @Override
    public String getEntityName()
    {
        return classObject == null ? null : classObject.getName();
    }

    /////////////////////////////////////////////////////////////
    // Meta property

    private class MetaProperty implements DBCAttributeMetaData,DBPImageProvider
    {
        private final WMIObjectAttribute attribute;
        private final int index;

        private MetaProperty(WMIObjectAttribute attribute, int index)
        {
            this.attribute = attribute;
            this.index = index;
        }

        @NotNull
        @Override
        public String getName()
        {
            return attribute.getName();
        }

        @Override
        public long getMaxLength()
        {
            return 0;
        }

        @Override
        public String getTypeName()
        {
            return attribute.getTypeName();
        }

        @Override
        public String getFullTypeName() {
            return attribute.getTypeName();
        }

        @Override
        public int getTypeID()
        {
            return attribute.getType();
        }

        @Override
        public DBPDataKind getDataKind()
        {
            return WMIClassAttribute.getDataKindById(attribute.getType());
        }

        @Override
        public int getScale()
        {
            return 0;
        }

        @Override
        public int getPrecision()
        {
            return 0;
        }

        @Override
        public int getOrdinalPosition()
        {
            return index;
        }

        @Nullable
        @Override
        public Object getSource() {
            return null;
        }

        @NotNull
        @Override
        public String getLabel()
        {
            return attribute.getName();
        }

        @Nullable
        @Override
        public String getEntityName()
        {
            return classObject == null ? null : classObject.getName();
        }

        @Override
        public boolean isReadOnly()
        {
            return false;
        }

        @Nullable
        @Override
        public DBCEntityMetaData getEntityMetaData()
        {
            return WMIResultSet.this;
        }

        @Nullable
        @Override
        public DBPImage getObjectImage()
        {
            return WMIClassAttribute.getPropertyImage(attribute.getType());
        }

        @Override
        public boolean isRequired()
        {
            return false;
        }

        @Override
        public boolean isAutoGenerated() {
            return false;
        }

    }

}
