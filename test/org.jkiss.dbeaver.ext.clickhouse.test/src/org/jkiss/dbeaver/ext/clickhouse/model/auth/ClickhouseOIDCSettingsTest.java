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
package org.jkiss.dbeaver.ext.clickhouse.model.auth;

import org.jkiss.dbeaver.DBException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ClickhouseOIDCSettingsTest {

    @Test
    void entraTenantIsResolvedFromTheEmailDomain() throws DBException {
        // Entra resolves a tenant by any of its verified domains, which is what lets
        // the connection dialog ask for an email only
        Assertions.assertEquals(
            "https://login.microsoftonline.com/clickhouse.com/v2.0",
            settings(null, "user@clickhouse.com").resolveIssuer());
    }

    @Test
    void explicitIssuerWins() throws DBException {
        // Okta and other providers cannot be derived from a domain, so a configured
        // issuer must never be overridden
        String okta = "https://example.okta.com/oauth2/default";
        Assertions.assertEquals(okta, settings(okta, "user@clickhouse.com").resolveIssuer());
        Assertions.assertEquals(okta, settings(okta, null).resolveIssuer());
    }

    @Test
    void issuerIsRequiredWhenTheEmailCarriesNoDomain() {
        Assertions.assertThrows(DBException.class, () -> settings(null, null).resolveIssuer());
        Assertions.assertThrows(DBException.class, () -> settings(null, "").resolveIssuer());
        Assertions.assertThrows(DBException.class, () -> settings(null, "no-at-sign").resolveIssuer());
        Assertions.assertThrows(DBException.class, () -> settings(null, "trailing@").resolveIssuer());
        Assertions.assertThrows(DBException.class, () -> settings(null, "@leading.com").resolveIssuer());
    }

    @Test
    void validationAgreesWithResolution() {
        // The connection dialog must not accept settings the login would then reject
        Assertions.assertTrue(ClickhouseOIDCSettings.canResolveIssuer(null, "user@clickhouse.com"));
        Assertions.assertTrue(ClickhouseOIDCSettings.canResolveIssuer("https://example.okta.com/oauth2/default", null));
        Assertions.assertFalse(ClickhouseOIDCSettings.canResolveIssuer(null, "no-at-sign"));
        Assertions.assertFalse(ClickhouseOIDCSettings.canResolveIssuer(null, "trailing@"));
        Assertions.assertFalse(ClickhouseOIDCSettings.canResolveIssuer(null, "@leading.com"));
        Assertions.assertFalse(ClickhouseOIDCSettings.canResolveIssuer(null, null));
    }

    private static ClickhouseOIDCSettings settings(String issuer, String email) {
        return new ClickhouseOIDCSettings(issuer, "client-id", null, null, null, email, 0, false);
    }
}
