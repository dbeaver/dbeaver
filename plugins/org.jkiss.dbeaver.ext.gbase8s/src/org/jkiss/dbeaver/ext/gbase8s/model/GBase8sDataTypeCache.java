/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.gbase8s.model;

import org.jkiss.dbeaver.ext.generic.model.GenericDataType;
import org.jkiss.dbeaver.ext.generic.model.GenericDataTypeCache;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

/**
 * @author Chao Tian
 */
public class GBase8sDataTypeCache extends GenericDataTypeCache {

    public GBase8sDataTypeCache(GenericStructContainer owner) {
        super(owner);
    }

    @Override
    protected GenericDataType makeDataType(JDBCResultSet dbResult, String name, int valueType) {
        return new GBase8sDataType(owner, valueType, name,
                JDBCUtils.safeGetString(dbResult, JDBCConstants.LOCAL_TYPE_NAME),
                JDBCUtils.safeGetBoolean(dbResult, JDBCConstants.UNSIGNED_ATTRIBUTE),
                JDBCUtils.safeGetInt(dbResult, JDBCConstants.SEARCHABLE) != 0,
                JDBCUtils.safeGetInt(dbResult, JDBCConstants.PRECISION),
                JDBCUtils.safeGetInt(dbResult, JDBCConstants.MINIMUM_SCALE),
                JDBCUtils.safeGetInt(dbResult, JDBCConstants.MAXIMUM_SCALE));
    }
}
