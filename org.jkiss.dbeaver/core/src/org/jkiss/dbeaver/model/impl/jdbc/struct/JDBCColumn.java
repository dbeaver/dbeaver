/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.IObjectImageProvider;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.meta.AbstractColumn;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * JDBC abstract column
 */
public abstract class JDBCColumn extends AbstractColumn implements IObjectImageProvider {
    protected JDBCColumn()
    {
    }

    protected JDBCColumn(String name, String typeName, int valueType, int ordinalPosition, int maxLength, int scale,
                         int radix, int precision, boolean nullable, String description)
    {
        super(name, typeName, valueType, ordinalPosition, maxLength, scale, radix, precision, nullable, description);
    }

    public Image getObjectImage()
    {
        DBSDataKind dataKind = JDBCUtils.getDataKind(this);
        switch (dataKind) {
            case BOOLEAN:
                return DBIcon.TYPE_BOOLEAN.getImage();
            case STRING:
                return DBIcon.TYPE_STRING.getImage();
            case NUMERIC:
                if (getValueType() == java.sql.Types.BIT) {
                    return DBIcon.TYPE_BOOLEAN.getImage();
                } else {
                    return DBIcon.TYPE_NUMBER.getImage();
                }
            case DATETIME:
                return DBIcon.TYPE_DATETIME.getImage();
            case BINARY:
                return DBIcon.TYPE_BINARY.getImage();
            case LOB:
                return DBIcon.TYPE_LOB.getImage();
            default:
                return DBIcon.TYPE_UNKNOWN.getImage();
        }
    }
}
