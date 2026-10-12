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

import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class CDataDriverInfoTest extends DBeaverUnitTest {
    @Test
    public void documentationLinkUsesSourceAndVersionFromOrderSku() {
        var salesforce = JSONUtils.GSON.fromJson("""
            {"order_sku": "RFRN-VSDBVR", "version_year": "2025", "current_version_letter": "M"}
            """, CDataDriverInfo.class);
        var amazonS3 = JSONUtils.GSON.fromJson("""
            {"order_sku": "SXRM-VSDBVR", "version_year": "2026", "current_version_letter": "N"}
            """, CDataDriverInfo.class);

        Assertions.assertEquals("RFRN-VSDBVR", salesforce.orderSku());
        Assertions.assertEquals("https://cdn.cdata.com/help/RFN/jdbc/", salesforce.documentationUrl());
        Assertions.assertEquals("https://cdn.cdata.com/help/SXM/jdbc/", amazonS3.documentationUrl());
    }

    @Test
    public void documentationLinkIsAbsentWithoutUsableOrderSku() {
        for (String json : List.of("{}", "{\"order_sku\": null}", "{\"order_sku\": \"\"}", "{\"order_sku\": \"RFR\"}")) {
            var driver = JSONUtils.GSON.fromJson(json, CDataDriverInfo.class);

            Assertions.assertNull(driver.documentationUrl());
        }
    }
}
