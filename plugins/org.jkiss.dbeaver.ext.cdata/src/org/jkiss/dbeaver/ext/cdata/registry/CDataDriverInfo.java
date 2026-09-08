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

import org.jkiss.code.NotNull;

public record CDataDriverInfo(
    @NotNull String dataSource,
    @NotNull String artifactId,
    @NotNull String driverName,
    @NotNull String driverSku,
    @NotNull String versionChar,
    int versionYear,
    @NotNull String orderSku,
    @NotNull CDataDriverTier tier,
    int annualPriceUsd,
    @NotNull String purchaseUrl
) {
    static final String ARTIFACT_SUFFIX = "-jdbc";

    /**
     * Token CData uses in the driver package and in the JDBC URL:
     * {@code cdata.jdbc.<jdbcName>.<...>Driver} and {@code jdbc:<jdbcName>:}.
     * Every CData artifact is named {@code <jdbcName>-jdbc}.
     */
    @NotNull
    public String jdbcName() {
        return artifactId.substring(0, artifactId.length() - ARTIFACT_SUFFIX.length());
    }

    @NotNull
    public String mavenVersionPattern() {
        return "{" + (versionYear - 2000) + "\\..*}";
    }
}
