/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.bigquery;

import org.jkiss.code.DynamicCall;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.bigquery.model.BQDataSource;
import org.jkiss.dbeaver.ext.generic.GenericDataSourceProvider;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

public class BQDataSourceProvider extends GenericDataSourceProvider<BQDataSource> {
    private static final String DRIVER_PARAM_PARTNER_ATTRIBUTION = "supports-partner-attribution";

    @DynamicCall
    public BQDataSourceProvider() {
        super(BQDataSource.class);
    }

    protected BQDataSourceProvider(@NotNull Class<? extends BQDataSource> dsClass) {
        super(dsClass);
    }

    @NotNull
    @Override
    public String getConnectionURL(@NotNull DBPDriver driver, @NotNull DBPConnectionConfiguration connectionInfo) {
        //jdbc:bigquery://https://www.googleapis.com/bigquery/v2:443;ProjectId={server};OAuthType=0;OAuthServiceAcctEmail={user};OAuthPvtKeyPath={host};
        StringBuilder url = new StringBuilder();
        url.append("jdbc:bigquery://").append(connectionInfo.getHostName());
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            url.append(":").append(connectionInfo.getHostPort());
        }
        if (CommonUtils.toBoolean(driver.getDriverParameter(DRIVER_PARAM_PARTNER_ATTRIBUTION), false)) {
            String product = GeneralUtils.getProductName()
                .trim()
                .replace(" ", "+")
                .replace("/", "-");
            url.append(";PartnerToken=\"")
                .append(product)
                .append("/v")
                .append(GeneralUtils.getProductVersion())
                .append("(GPN:DBeaver;Environment)\"");
        }
        return url.toString();
    }
}
