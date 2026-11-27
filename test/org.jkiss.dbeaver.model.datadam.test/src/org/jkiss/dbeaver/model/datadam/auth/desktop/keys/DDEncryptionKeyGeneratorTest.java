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
package org.jkiss.dbeaver.model.datadam.auth.desktop.keys;

import org.jkiss.dbeaver.model.datadam.auth.DDEncryptionKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.SecureRandom;
import java.util.Arrays;

class DDEncryptionKeyGeneratorTest {
    @Test
    void matchesOfficialBip39EnglishVectors() throws Exception {
        Assertions.assertEquals(2048, DDBip39Wordlist.WORDS.size());

        String zero = generatorFor((byte) 0x00).generate();
        Assertions.assertEquals("abandon ".repeat(11) + "about", zero);
        Assertions.assertEquals(zero, DDEncryptionKey.normalizeAndValidate(zero));

        String ones = generatorFor((byte) 0xFF).generate();
        Assertions.assertEquals("zoo ".repeat(11) + "wrong", ones);
        Assertions.assertEquals(ones, DDEncryptionKey.normalizeAndValidate(ones));
    }

    private static DDEncryptionKeyGenerator generatorFor(byte entropy) {
        SecureRandom random = Mockito.mock(SecureRandom.class);
        Mockito.doAnswer(invocation -> {
            Arrays.fill(invocation.getArgument(0, byte[].class), entropy);
            return null;
        }).when(random).nextBytes(Mockito.any(byte[].class));
        return new DDEncryptionKeyGenerator(random);
    }
}
