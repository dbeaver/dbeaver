package org.jkiss.dbeaver.ext.oceanbase.mysql.model;

import java.sql.ResultSet;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.MySQLUtils;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.utils.CommonUtils;

public class OceanbaseMySQLViewColumn extends MySQLTableColumn {
    public OceanbaseMySQLViewColumn(MySQLTableBase table, ResultSet dbResult) throws DBException {
        super(table);
        loadInfo(dbResult);
        setPersisted(true);
    }

    public OceanbaseMySQLViewColumn(DBRProgressMonitor monitor, MySQLTableBase table, DBSEntityAttribute source)
            throws DBException {
        super(monitor, table, source);
    }

    private void loadInfo(ResultSet dbResult) throws DBException {
        setName(JDBCUtils.safeGetString(dbResult, "Field"));
        String typeName = JDBCUtils.safeGetString(dbResult, "Type");
        assert typeName != null;
        setTypeName(typeName);
        setFullTypeName(typeName);
        setValueType(MySQLUtils.typeNameToValueType(typeName.split("\\(")[0]));
        DBSDataType dataType = getDataSource().getLocalDataType(typeName);
        long charLength = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_CHARACTER_MAXIMUM_LENGTH);
        if (charLength <= 0) {
            if (dataType != null) {
                setMaxLength(CommonUtils.toInt(dataType.getPrecision()));
            }
        } else {
            setMaxLength(charLength);
        }
        setRequired(!"YES".equals(JDBCUtils.safeGetString(dbResult, "Null")));
        String defaultValue = JDBCUtils.safeGetString(dbResult, "Default");
        if (defaultValue != null) {
            DBPDataKind dataKind = getDataKind();

            if (dataKind == DBPDataKind.STRING && !SQLConstants.NULL_VALUE.equals(defaultValue)
                    && !SQLUtils.isStringQuoted(getDataSource(), defaultValue)) {
                defaultValue = SQLUtils.quoteString(getDataSource(), defaultValue);
            } else if (dataKind == DBPDataKind.DATETIME && !defaultValue.isEmpty()
                    && Character.isDigit(defaultValue.charAt(0))) {
                defaultValue = "'" + defaultValue + "'";
            } else {
                setDefaultValue(defaultValue);
            }
        }
        setExtraInfo(JDBCUtils.safeGetString(dbResult, "Extra"));
    }

}
