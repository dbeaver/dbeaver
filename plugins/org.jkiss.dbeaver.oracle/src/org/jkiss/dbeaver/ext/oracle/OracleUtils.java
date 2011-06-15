/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataTypeModifier;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;

/**
 * Oracle utils
 */
public class OracleUtils {

    static final Log log = LogFactory.getLog(OracleUtils.class);

    public static DBSDataType resolveDataType(DBRProgressMonitor monitor, OracleDataSource dataSource, String typeOwner, String typeName)
    {
        DBSDataType type = null;
        if (typeOwner != null) {
            try {
                final OracleSchema typeSchema = dataSource.getSchema(monitor, typeOwner);
                if (typeSchema == null) {
                    log.error("Type attr schema '" + typeOwner + "' not found");
                } else {
                    type = typeSchema.getDataType(monitor, typeName);
                }
            } catch (DBException e) {
                log.error(e);
            }
        } else {
            type = dataSource.getDataType(typeName);
        }
        if (type == null) {
            log.error("Data type '" + typeName + "' not found");
        }
        return type;
    }

    public static OracleDataTypeModifier resolveTypeModifier(String typeMod)
    {
        if (!CommonUtils.isEmpty(typeMod)) {
            try {
                return OracleDataTypeModifier.valueOf(typeMod);
            } catch (IllegalArgumentException e) {
                log.error(e);
            }
        }
        return null;
    }

}
