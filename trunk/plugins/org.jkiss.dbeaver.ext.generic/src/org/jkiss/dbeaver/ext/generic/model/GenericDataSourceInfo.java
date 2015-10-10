/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.jkiss.utils.CommonUtils;

/**
 * Generic data source info
 */
class GenericDataSourceInfo extends JDBCDataSourceInfo {

    private final boolean supportsLimits;
    private boolean supportsMultipleResults;

    public GenericDataSourceInfo(DBPDriver driver, JDBCDatabaseMetaData metaData)
    {
        super(metaData);
        supportsLimits = CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_LIMITS), true);
        setSupportsResultSetScroll(CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_SCROLL), false));
        supportsMultipleResults = CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_MULTIPLE_RESULTS), false);
    }

    @Override
    public boolean supportsResultSetLimit() {
        return supportsLimits;
    }

    @Override
    public boolean supportsMultipleResults() {
        return supportsMultipleResults;
    }

}
