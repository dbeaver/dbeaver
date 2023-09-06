
package org.jkiss.dbeaver.ext.yashandb.debug.internal.impl;

import com.yashandb.core.DataType;
import org.jkiss.dbeaver.debug.DBGVariable;
import org.jkiss.dbeaver.debug.DBGVariableType;

public class YashanDBDebugVariable implements DBGVariable<String> {

    private final String name;
    private final int lineNumber;
    private final int oid;
    private final String val;

    private int dataType=0;

    @Override
    public String getVal() {

        return val;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DBGVariableType getType() {
        switch (dataType){
            case DataType.UNKNOWN:
                return DBGVariableType.UNKNOWN;
            case DataType.BOOLEAN:
                return DBGVariableType.BOOLEAN;
            case DataType.TINYINT:
                return DBGVariableType.TINYINT;
            case DataType.SMALLINT:
                return DBGVariableType.SMALLINT;
            case DataType.INTEGER:
                return DBGVariableType.INTEGER;
            case DataType.BIGINT:
                return DBGVariableType.BIGINT;
            case DataType.UTINYINT:
                return DBGVariableType.UTINYINT;
            case DataType.USMALLINT:
                return DBGVariableType.USMALLINT;
            case DataType.UINTEGER:
                return DBGVariableType.UINTEGER;
            case DataType.UBIGINT:
                return DBGVariableType.UBIGINT;
            case DataType.FLOAT:
                return DBGVariableType.FLOAT;
            case DataType.DOUBLE:
                return DBGVariableType.DOUBLE;
            case DataType.NUMBER:
                return DBGVariableType.NUMBER;
            case DataType.DATE:
                return DBGVariableType.DATE;
            case DataType.SHORTDATE:
                return DBGVariableType.SHORTDATE;
            case DataType.SHORTTIME:
                return DBGVariableType.SHORTTIME;
            case DataType.TIMESTAMP:
                return DBGVariableType.TIMESTAMP;
            case DataType.TIMESTAMP_TZ:
                return DBGVariableType.TIMESTAMP_TZ;
            case DataType.TIMESTAMP_LTZ:
                return DBGVariableType.TIMESTAMP_LTZ;
            case DataType.YM_INTERVAL:
                return DBGVariableType.YM_INTERVAL;
            case DataType.DS_INTERVAL:
                return DBGVariableType.DS_INTERVAL;
            case DataType.CHAR:
                return DBGVariableType.CHAR;
            case DataType.NCHAR:
                return DBGVariableType.NCHAR;
            case DataType.VARCHAR:
                return DBGVariableType.VARCHAR;
            case DataType.NVARCHAR:
                return DBGVariableType.NVARCHAR;
            case DataType.RAW:
                return DBGVariableType.RAW;
            case DataType.CLOB:
                return DBGVariableType.CLOB;
            case DataType.BLOB:
                return DBGVariableType.BLOB;
            case DataType.BIT:
                return DBGVariableType.BIT;
            case DataType.ROWID:
                return DBGVariableType.ROWID;
            case DataType.NCLOB:
                return DBGVariableType.NCLOB;
            case DataType.CURSOR:
                return DBGVariableType.CURSOR;
            case DataType.JSON:
                return DBGVariableType.JSON;
            case DataType.RECORD:
                return DBGVariableType.RECORD;
            default:
                return DBGVariableType.UNKNOWN;
        }

    }
    public YashanDBDebugVariable(String name,  int linenumber, int oid, String val, int dataType) {
        super();
        this.name = name;
        this.lineNumber = linenumber;
        this.oid = oid;
        this.val = val;
        this.dataType=dataType;
    }



    public int getOid() {
        return oid;
    }

    @Override
    public String toString() {
        return "YashanDBDebugVariable{" +
                "name='" + name + '\'' +
                ", lineNumber=" + lineNumber +
                ", oid=" + oid +
                ", val='" + val + '\'' +
                '}';
    }

	@Override
	public int getLineNumber() {
		// TODO Auto-generated method stub
		return 0;
	}
}
