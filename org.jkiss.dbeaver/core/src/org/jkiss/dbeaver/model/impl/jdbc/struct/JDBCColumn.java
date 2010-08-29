/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.IObjectImageProvider;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractColumn;

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
        return JDBCUtils.getDataIcon(this).getImage();
    }
}
