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
package com.dbeaver.db.cdata.registry;

import org.jkiss.code.NotNull;

public record CDataDriverInfo(
    @NotNull String dataSource,
    @NotNull String driverName,
    @NotNull String driverSku,
    @NotNull String versionChar,
    int versionYear,
    @NotNull String orderSku,
    @NotNull CDataDriverTier tier,
    int annualPriceUsd,
    @NotNull String purchaseUrl
) {
    @NotNull
    public String artifactId() {
        return switch (dataSource) {
            case "adwords" -> "googleads-jdbc";
            case "athena" -> "amazonathena-jdbc";
            case "authorizedotnet" -> "authorizenet-jdbc";
            case "azure" -> "azuretables-jdbc";
            case "azureactivedirectory" -> "azuread-jdbc";
            case "azureanalysisservices" -> "aas-jdbc";
            case "azuredatalake" -> "adls-jdbc";
            case "bigquery" -> "googlebigquery-jdbc";
            case "blackbaudfenxt" -> "financialedgenxt-jdbc";
            case "bridge" -> "jdbcodbc-jdbc";
            case "certinia" -> "financialforce-jdbc";
            case "concur" -> "sapconcur-jdbc";
            case "couchdb" -> "apachecouchdb-jdbc";
            case "dataverse" -> "cds-jdbc";
            case "dfp" -> "googleadsmanager-jdbc";
            case "doubleclick" -> "googlecm-jdbc";
            case "dynamodb" -> "amazondynamodb-jdbc";
            case "eloqua" -> "oracleeloqua-jdbc";
            case "eloquareporting" -> "oracleeloquareporting-jdbc";
            case "epicorkinetic" -> "epicorerp-jdbc";
            case "exact" -> "exactonline-jdbc";
            case "ganalytics" -> "googleanalytics-jdbc";
            case "gsheets" -> "googlesheets-jdbc";
            case "hbase" -> "apachehbase-jdbc";
            case "hive" -> "apachehive-jdbc";
            case "ibminformix" -> "informix-jdbc";
            case "impala" -> "apacheimpala-jdbc";
            case "intacct" -> "sageintacct-jdbc";
            case "kafka" -> "apachekafka-jdbc";
            case "msplanner" -> "microsoftplanner-jdbc";
            case "myobaccountright" -> "myob-jdbc";
            case "oracledb" -> "oracleoci-jdbc";
            case "pardot" -> "salesforcepardot-jdbc";
            case "phoenix" -> "apachephoenix-jdbc";
            case "qbonline" -> "quickbooksonline-jdbc";
            case "raisersedgenxt" -> "raiseredgenxt-jdbc";
            case "sagecloudaccounting" -> "sagebcaccounting-jdbc";
            case "salesforcedc" -> "salesforcedata360-jdbc";
            case "salesforcemarketing" -> "sfmarketingcloud-jdbc";
            case "sap" -> "saperp-jdbc";
            case "saphybris" -> "saphybrisc4c-jdbc";
            case "spark" -> "sparksql-jdbc";
            case "veeva" -> "veevavault-jdbc";
            default -> dataSource + "-jdbc";
        };
    }

    @NotNull
    public String jdbcName() {
        return artifactId().substring(0, artifactId().length() - "-jdbc".length());
    }

    @NotNull
    public String mavenVersionPattern() {
        return "{" + (versionYear - 2000) + "\\..*}";
    }
}
