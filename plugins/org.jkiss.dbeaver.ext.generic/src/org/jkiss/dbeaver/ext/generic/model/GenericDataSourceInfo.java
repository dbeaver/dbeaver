/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
public class GenericDataSourceInfo extends JDBCDataSourceInfo {

    private final boolean supportsLimits;
    private boolean supportsMultipleResults;
    private boolean supportsNullableUniqueConstraints;
    private final boolean supportsSetArray;
    private final boolean supportsTransactionsForDDL;
    private final boolean supportsConstraints;

    public GenericDataSourceInfo(DBPDriver driver, JDBCDatabaseMetaData metaData)
    {
        super(metaData);
        supportsLimits = CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_LIMITS), true);
        setSupportsResultSetScroll(CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_SCROLL), false));
        supportsMultipleResults = CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_MULTIPLE_RESULTS), false);
        supportsSetArray = CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_SET_ARRAY), false);
        supportsTransactionsForDDL = CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_TRANSACTIONS_FOR_DDL), true);
        setReadOnly(CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_READ_ONLY_DATA), false));

        supportsNullableUniqueConstraints = false;
        supportsConstraints = CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_CONSTRAINTS), true);
    }

    @Override
    public boolean supportsResultSetLimit() {
        return supportsLimits;
    }

    @Override
    public boolean supportsNullableUniqueConstraints() {
        return supportsNullableUniqueConstraints;
    }

    public void setSupportsNullableUniqueConstraints(boolean supportsNullableUniqueConstraints) {
        this.supportsNullableUniqueConstraints = supportsNullableUniqueConstraints;
    }

    @Override
    public boolean supportsMultipleResults() {
        return supportsMultipleResults;
    }

    @Override
    public boolean supportsSetArray() {
        return supportsSetArray;
    }

    @Override
    public boolean supportsTransactionsForDDL() {
        return super.supportsTransactionsForDDL() && supportsTransactionsForDDL;
    }

    public boolean supportsTableConstraints() {
        return supportsConstraints;
    }
}
