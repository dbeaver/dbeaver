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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/** Creates a 12-word BIP-39 English phrase from 128 bits of random entropy. */
public class DDEncryptionKeyGenerator {
    private static final int ENTROPY_BYTES = 16;
    private static final int WORD_COUNT = 12;
    private static final int BITS_PER_WORD = 11;

    private final SecureRandom random;

    public DDEncryptionKeyGenerator() {
        this(new SecureRandom());
    }

    DDEncryptionKeyGenerator(@NotNull SecureRandom random) {
        this.random = random;
    }

    @NotNull
    public String generate() throws DBException {
        byte[] entropy = new byte[ENTROPY_BYTES];
        byte[] checksum = null;
        try {
            random.nextBytes(entropy);
            checksum = MessageDigest.getInstance("SHA-256").digest(entropy);
            StringBuilder phrase = new StringBuilder();
            for (int word = 0; word < WORD_COUNT; word++) {
                int index = 0;
                for (int bit = 0; bit < BITS_PER_WORD; bit++) {
                    int offset = word * BITS_PER_WORD + bit;
                    int value = offset < ENTROPY_BYTES * Byte.SIZE
                        ? (entropy[offset / Byte.SIZE] >>> (7 - offset % Byte.SIZE)) & 1
                        : (checksum[0] >>> (7 - (offset - ENTROPY_BYTES * Byte.SIZE))) & 1;
                    index = (index << 1) | value;
                }
                if (word > 0) {
                    phrase.append(' ');
                }
                phrase.append(DDBip39Wordlist.WORDS.get(index));
            }
            return phrase.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new DBException("Could not generate DataDam recovery phrase", e);
        } finally {
            Arrays.fill(entropy, (byte) 0);
            if (checksum != null) {
                Arrays.fill(checksum, (byte) 0);
            }
        }
    }
}
