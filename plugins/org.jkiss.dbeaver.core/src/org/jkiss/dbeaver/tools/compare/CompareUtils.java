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
