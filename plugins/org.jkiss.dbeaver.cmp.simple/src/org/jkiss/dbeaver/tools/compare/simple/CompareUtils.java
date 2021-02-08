/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.compare.simple;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

/**
 * Compare utils
 */
public class CompareUtils {

    public static boolean equalPropertyValues(Object value1, Object value2)
    {
        if (value1 instanceof DBSObject && value2 instanceof DBSObject) {
            for (DBSObject curValue1 = (DBSObject) value1, curValue2 = (DBSObject) value2;
                 curValue1 != null && curValue2 != null;
                 curValue1 = curValue1.getParentObject(), curValue2 = curValue2.getParentObject())
            {
                if (curValue1.getClass() != curValue2.getClass()) {
                    return false;
                }
                if (curValue1 instanceof DBPDataSourceContainer) {
                    return true;
                }
                if (!CommonUtils.equalObjects(curValue1.getName(), curValue2.getName())) {
                    return false;
                }
            }
            return true;
        } else {
            return CommonUtils.equalObjects(value1, value2);
        }
    }

}
