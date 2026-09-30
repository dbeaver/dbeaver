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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ClickhouseCloudJWTProviderTest {

    @Test
    void managedServiceHostsAreRecognized() {
        // Every managed control plane must be matched, otherwise the model refuses to sign in
        Assertions.assertNotNull(ClickhouseCloudJWTProvider.create("abc123.us-east-1.aws.clickhouse.cloud"));
        Assertions.assertNotNull(ClickhouseCloudJWTProvider.create("abc123.us-west-2.aws.clickhouse-staging.com"));
        Assertions.assertNotNull(ClickhouseCloudJWTProvider.create("abc123.eu-west-1.aws.clickhouse-dev.com"));
    }

    @Test
    void hostMatchingIsCaseInsensitive() {
        Assertions.assertNotNull(ClickhouseCloudJWTProvider.create("ABC123.US-EAST-1.AWS.CLICKHOUSE.CLOUD"));
    }

    @Test
    void otherHostsAreNotCloudServices() {
        // Self managed servers must fall back to another auth model rather than
        // attempting a token exchange that cannot work
        Assertions.assertNull(ClickhouseCloudJWTProvider.create("localhost"));
        Assertions.assertNull(ClickhouseCloudJWTProvider.create("clickhouse.example.com"));
        Assertions.assertNull(ClickhouseCloudJWTProvider.create(null));
        Assertions.assertNull(ClickhouseCloudJWTProvider.create(""));
        // Lookalike host that merely contains the suffix in the middle
        Assertions.assertNull(ClickhouseCloudJWTProvider.create("clickhouse.cloud.example.com"));
    }
}
