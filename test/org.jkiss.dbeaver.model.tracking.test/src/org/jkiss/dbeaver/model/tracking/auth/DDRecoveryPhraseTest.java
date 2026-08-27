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
package org.jkiss.dbeaver.model.tracking.auth;

import org.jkiss.dbeaver.DBException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DDRecoveryPhraseTest {
    private static final String RECOVERY_PHRASE =
        "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";

    @Test
    void normalizesAndValidatesPhrase() throws Exception {
        Assertions.assertEquals(
            RECOVERY_PHRASE,
            DDRecoveryPhrase.normalizeAndValidate("  abandon\tabandon abandon abandon abandon abandon "
                + "abandon abandon abandon abandon abandon\nabout  ")
        );
    }

    @Test
    void rejectsWrongWordCount() {
        Assertions.assertThrows(
            DBException.class,
            () -> DDRecoveryPhrase.normalizeAndValidate("abandon abandon abandon")
        );
    }

    @Test
    void rejectsUnknownWord() {
        Assertions.assertThrows(
            DBException.class,
            () -> DDRecoveryPhrase.normalizeAndValidate(RECOVERY_PHRASE.replace("about", "notaword"))
        );
    }

    @Test
    void rejectsInvalidChecksum() {
        Assertions.assertThrows(
            DBException.class,
            () -> DDRecoveryPhrase.normalizeAndValidate("abandon ".repeat(12).trim())
        );
    }
}
