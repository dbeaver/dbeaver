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
    @ParameterizedTest
    @ValueSource(strings = {
        "user@example.com",
        "first.last+tag@example.co.uk",
        "user_name@example-domain.com",
        "USER@EXAMPLE.COM",
        "user@sub.example.com",
        "user@xn--bcher-kva.de",
        "o'connor@example.com",
        "!#$%&'*+/=?^_`{|}~-@example.com"
    })
    public void acceptsValidEmail(@NotNull String email) {
        Assertions.assertTrue(CDataLicenseActivationRequest.isValidEmail(email));
        for (CDataLicenseType type : CDataLicenseType.values()) {
            CDataLicenseActivationRequest request = new CDataLicenseActivationRequest("Test User", email, type, "test-key");
            Assertions.assertEquals(email, request.email());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "user",
        "@example.com",
        "user@",
        "user@localhost",
        "user@@example.com",
        ".user@example.com",
        "user.@example.com",
        "first..last@example.com",
        "user@.example.com",
        "user@example..com",
        "user@example.com.",
        "user@-example.com",
        "user@example-.com",
        "user@exa_mple.com",
        "first last@example.com",
        "user@exa mple.com",
        "user@example.com\nother@example.com",
        "user@example.com\u0000",
        "name[1]@example.com",
        "../../../@../../../../qqqqqwwwwwwwwwwwwwwwwwqqqqqqqqqq"
    })
    public void rejectsInvalidEmailBeforeActivation(@NotNull String email) {
        assertInvalidEmail(email);
    }

    @Test
    public void enforcesEmailLengthLimits() {
        String longestEmail = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
        Assertions.assertEquals(254, longestEmail.length());
        Assertions.assertTrue(CDataLicenseActivationRequest.isValidEmail(longestEmail));
        Assertions.assertDoesNotThrow(() -> new CDataLicenseActivationRequest("Test User", longestEmail, CDataLicenseType.TRIAL, null));
        assertInvalidEmail(longestEmail + "d");
        assertInvalidEmail("a".repeat(65) + "@example.com");
        assertInvalidEmail("user@" + "a".repeat(64) + ".com");
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
        Assertions.assertFalse(CDataLicenseActivationRequest.isValidEmail(email));
        for (CDataLicenseType type : CDataLicenseType.values()) {
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> new CDataLicenseActivationRequest("Test User", email, type, "test-key"));
        }
    }
}
