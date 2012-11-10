package org.jkiss.dbeaver.runtime.qm;

import org.jkiss.dbeaver.runtime.qm.meta.QMMObject;
import org.jkiss.dbeaver.runtime.qm.meta.QMMSessionInfo;
import org.jkiss.dbeaver.runtime.qm.meta.QMMStatementInfo;
import org.jkiss.dbeaver.runtime.qm.meta.QMMTransactionInfo;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Object type
 */
public enum QMObjectType {

    session(QMMSessionInfo.class),
    txn(QMMTransactionInfo.class),
    query(QMMStatementInfo.class);

    private final Class<? extends QMMObject> type;

    private QMObjectType(Class<? extends QMMObject> type)
    {
        this.type = type;
    }

    public Class<? extends QMMObject> getType()
    {
        return type;
    }

    public static String toString(Collection<QMObjectType> objectTypes)
    {
        List<String> names = new ArrayList<String>(objectTypes.size());
        for (QMObjectType type : objectTypes) {
            names.add(type.name());
        }
        return CommonUtils.makeString(names, ',');
    }

    public static Collection<QMObjectType> fromString(String str)
    {
        List<QMObjectType> objectTypes = new ArrayList<QMObjectType>();
        for (String otName : CommonUtils.splitString(str, ',')) {
            try {
                objectTypes.add(QMObjectType.valueOf(otName));
            } catch (IllegalArgumentException e) {
                // just scrip bad names
            }
        }
        return objectTypes;
    }
}
