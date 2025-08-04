package org.jkiss.dbeaver.ext.cubrid.data;

import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.data.DefaultValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCColumnMetaData;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.BeanUtils;

public class CubridValueHandlerProvider implements DBDValueHandlerProvider {

    @Override
    public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDFormatSettings preferences, DBSTypedObject typedObject) {
        boolean isEnableOID = DBWorkbench.getPlatform().getPreferenceStore().getBoolean(CubridConstants.OID_NAVIGATOR);
        JDBCColumnMetaData columnMeta = (JDBCColumnMetaData) typedObject;
        String columnName = columnMeta.getName();
        String tableName = columnMeta.getEntityName();
        String typeName = typedObject.getTypeName();
        if (isEnableOID && CubridConstants.CLASS.equals(typeName) && columnName.equals(tableName)) {
            return new CubridOIDValueHandler();
        }
        return null;
    }

    class CubridOIDValueHandler extends DefaultValueHandler {
        @Override
        public Object fetchValueObject(DBCSession session, DBCResultSet resultSet, DBSTypedObject type, int index) throws DBCException {
            String oidValue = null;
            Object originalValue = resultSet.getAttributeValue(index);
            try {
                oidValue = (String) BeanUtils.invokeObjectMethod(originalValue, "getOidString");
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return oidValue;
        }
    }
}
