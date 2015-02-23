/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.tools.compare;

import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
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
                if (curValue1 instanceof DBSDataSourceContainer) {
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
