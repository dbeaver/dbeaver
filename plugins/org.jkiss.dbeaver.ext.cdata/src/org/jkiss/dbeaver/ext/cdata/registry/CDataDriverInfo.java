/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cdata.registry;

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public record CDataDriverInfo(
    @SerializedName("datasource") @NotNull String dataSource,
    @SerializedName("objname") @NotNull String objectName,
    @SerializedName("driver_name") @NotNull String driverName,
    @SerializedName("version_year") int versionYear,
    @NotNull CDataDriverTier tier,
    @SerializedName("purchase_url") @NotNull String purchaseUrl,
    @SerializedName("datatype_id") @NotNull String dataTypeId,
    @SerializedName("order_sku") @Nullable String orderSku
) {
    private static final String DOCUMENTATION_URL_PREFIX = "https://cdn.cdata.com/help/";

    @Nullable
    public String documentationUrl() {
        if (orderSku == null || orderSku.length() < 4) {
            return null;
        }
        return DOCUMENTATION_URL_PREFIX + orderSku.substring(0, 2) + orderSku.charAt(3) + "/jdbc/";
    }

    @NotNull
    public String artifactId() {
        return objectName + "-jdbc";
    }

    @NotNull
    public String jdbcName() {
        return objectName;
    }

    @NotNull
    public String mavenVersionPattern() {
        return "{" + (versionYear - 2000) + "\\..*}";
    }
}
