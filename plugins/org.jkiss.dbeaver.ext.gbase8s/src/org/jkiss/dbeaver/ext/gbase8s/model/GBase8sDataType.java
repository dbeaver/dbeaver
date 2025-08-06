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

import java.sql.Types;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataType;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;

/**
 * @author Chao Tian
 */
public class GBase8sDataType extends GenericDataType {

    private static final Log log = Log.getLog(GBase8sDataType.class);

    // [JDBC: GBase 8s - VARCHAR2]
    public static final int VARCHAR2 = 63;

    // [JDBC: GBase 8s - NVARCHAR2]
    public static final int NVARCHAR2 = 64;

    public GBase8sDataType(GenericStructContainer owner, int valueType, String name, String remarks, boolean unsigned,
            boolean searchable, int precision, int minScale, int maxScale) {
        super(owner, valueType, name, remarks, unsigned, searchable, precision, minScale, maxScale);

        // Check for VARCHAR2/NVARCHAR2 type for strings
        if (valueType == VARCHAR2 || valueType == NVARCHAR2) {
            log.warn("Inconsistent string data type name/id: " + name + "(" + valueType + "). Setting to "
                    + Types.VARCHAR);
            setTypeID(Types.VARCHAR);
        }

    }

}
