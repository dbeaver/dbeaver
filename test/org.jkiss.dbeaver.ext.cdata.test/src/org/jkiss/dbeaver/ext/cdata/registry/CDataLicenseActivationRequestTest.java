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
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class CDataLicenseActivationRequestTest extends DBeaverUnitTest {
    @Test
    public void acceptsValidEmail() {
        for (CDataLicenseType type : CDataLicenseType.values()) {
            CDataLicenseActivationRequest request = new CDataLicenseActivationRequest(
                "Test User", "first.last+tag@example.com", type, "test-key"
            );
            Assertions.assertEquals("first.last+tag@example.com", request.email());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "user@@example.com", "../../../@../../../../qqqqqwwwwwwwwwwwwwwwwwqqqqqqqqqq"})
    public void rejectsInvalidEmailBeforeActivation(@NotNull String email) {
        assertInvalidEmail(email);
    }

    @Test
    public void rejectsOversizedEmailBeforeActivation() {
        assertInvalidEmail("a".repeat(255) + "@example.com");
    }

    @Test
    public void normalizesSurroundingWhitespace() {
        for (CDataLicenseType type : CDataLicenseType.values()) {
            CDataLicenseActivationRequest request = new CDataLicenseActivationRequest(
                "Test User", " user@example.com ", type, "test-key"
            );
            Assertions.assertEquals("user@example.com", request.email());
        }
    }

    private static void assertInvalidEmail(@NotNull String email) {
        for (CDataLicenseType type : CDataLicenseType.values()) {
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> new CDataLicenseActivationRequest("Test User", email, type, "test-key"));
        }
    }
}
