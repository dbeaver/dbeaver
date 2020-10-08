package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

public class SQLServerTypedObject implements DBSTypedObject {
    private String typeName;
    private int typeId;
    private DBPDataKind dataKind;
    private int scale;
    private int precision;
    private int maxLength;

    public SQLServerTypedObject(String typeName, int typeId, DBPDataKind dataKind, int scale, int precision, int maxLength) {
        this.typeName = typeName;
        this.typeId = typeId;
        this.dataKind = dataKind;
        this.scale = scale;
        this.precision = precision;
        this.maxLength = maxLength;
    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    @Override
    public String getFullTypeName() {
        return typeName;
    }

    @Override
    public int getTypeID() {
        return typeId;
    }

    @Override
    public DBPDataKind getDataKind() {
        return dataKind;
    }

    @Override
    public Integer getScale() {
        return scale;
    }

    @Override
    public Integer getPrecision() {
        return precision;
    }

    @Override
    public long getMaxLength() {
        return maxLength;
    }

    @Override
    public long getTypeModifiers() {
        return 0;
    }
}
