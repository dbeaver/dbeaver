/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
    public boolean supportsCatalogSelection;
    public boolean supportsSchemaSelection;
    private boolean supportsMultipleResults;
    private boolean multipleResultsFailsOnMaxRows;
    private boolean supportsNullableUniqueConstraints;
    private final boolean supportsTransactionsForDDL;
    private final boolean supportsConstraints;

    public GenericDataSourceInfo(DBPDriver driver, JDBCDatabaseMetaData metaData)
    {
        super(metaData);
        supportsLimits = CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_LIMITS), true);
        setSupportsResultSetScroll(CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_SCROLL), false));
        supportsMultipleResults = CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_MULTIPLE_RESULTS), false);
        multipleResultsFailsOnMaxRows = CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_MULTIPLE_RESULTS_FAILS_ON_MAX_ROWS), false);
        supportsTransactionsForDDL = CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_TRANSACTIONS_FOR_DDL), true);
        setReadOnlyData(CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_READ_ONLY_DATA), false));
        setReadOnlyMetaData(CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_READ_ONLY_META_DATA), false));
        supportsCatalogSelection = CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_CATALOG_SELECTION), true);
        supportsSchemaSelection = CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_SCHEMA_SELECTION), true);
        supportsNullableUniqueConstraints = false;
        supportsConstraints = CommonUtils.getBoolean(driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_CONSTRAINTS), true);

        final Object supportsReferences = driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_REFERENCES);
        if (supportsReferences != null) {
            this.setSupportsReferences(CommonUtils.toBoolean(supportsReferences));
        }

        final Object supportsIndexes = driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_INDEXES);
        if (supportsIndexes != null) {
            this.setSupportsIndexes(CommonUtils.toBoolean(supportsIndexes));
        }

        final Object supportsViews = driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_VIEWS);
        if (supportsViews != null) {
            this.setSupportsViews(CommonUtils.toBoolean(supportsViews));
        }

        final Object supportsStoredCode = driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_STORED_CODE);
        if (supportsStoredCode != null) {
            this.setSupportsStoredCode(CommonUtils.toBoolean(supportsStoredCode));
        }

        final Object supportsCatalogSelection = driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_CATALOG_SELECTION);
        if (supportsCatalogSelection != null) {
            this.supportsCatalogSelection = CommonUtils.toBoolean(supportsCatalogSelection);
        }
        final Object supportSchemaSelection = driver.getDriverParameter(GenericConstants.PARAM_SUPPORTS_SCHEMA_SELECTION);
        if (supportSchemaSelection != null) {
            this.supportsSchemaSelection = CommonUtils.toBoolean(supportSchemaSelection);
        }
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

    public boolean supportsCatalogSelection() {
        return supportsCatalogSelection;
    }

    public boolean supportsSchemaSelection() {
        return supportsSchemaSelection;
    }

    @Override
    public boolean supportsMultipleResults() {
        return supportsMultipleResults;
    }

    @Override
    public boolean isMultipleResultsFailsOnMaxRows() {
        return multipleResultsFailsOnMaxRows;
    }

    @Override
    public boolean supportsTransactionsForDDL() {
        return super.supportsTransactionsForDDL() && supportsTransactionsForDDL;
    }

    public boolean supportsTableConstraints() {
        return supportsConstraints;
    }
}
