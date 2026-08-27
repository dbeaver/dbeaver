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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class DDRecoveryPhrase {

    private static final int WORD_COUNT = 12;
    private static final int ENTROPY_BYTES = 16;
    private static final Map<String, Integer> WORD_INDICES = buildWordIndices();

    private DDRecoveryPhrase() {
    }

    @NotNull
    public static String normalize(@NotNull String phrase) {
        return String.join(" ", phrase.trim().split("\\s+"));
    }

    /**
     * Normalizes the phrase and validates its BIP-39 checksum, so an entry mistake is caught
     * with a specific reason here rather than surfacing later as an opaque decryption failure.
     */
    @NotNull
    public static String normalizeAndValidate(@NotNull String phrase) throws DBException {
        String normalized = normalize(phrase);
        String[] words = normalized.split(" ");
        if (words.length != WORD_COUNT) {
            throw new DBException("The recovery phrase must contain " + WORD_COUNT + " words");
        }

        byte[] entropy = new byte[ENTROPY_BYTES];
        int checksum = 0;
        try {
            for (int wordIndex = 0; wordIndex < words.length; wordIndex++) {
                Integer index = WORD_INDICES.get(words[wordIndex]);
                if (index == null) {
                    throw new DBException("The recovery phrase contains an unknown word: " + words[wordIndex]);
                }
                for (int bit = 0; bit < 11; bit++) {
                    int valueBit = (index >> (10 - bit)) & 1;
                    int phraseBit = wordIndex * 11 + bit;
                    if (phraseBit < entropy.length * Byte.SIZE) {
                        entropy[phraseBit / Byte.SIZE] |= (byte) (valueBit << (7 - phraseBit % Byte.SIZE));
                    } else {
                        checksum = (checksum << 1) | valueBit;
                    }
                }
            }

            int expectedChecksum;
            try {
                expectedChecksum = (MessageDigest.getInstance("SHA-256").digest(entropy)[0] & 0xFF) >> 4;
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
            if (checksum != expectedChecksum) {
                throw new DBException("The recovery phrase checksum is invalid - check the words and try again");
            }
            return normalized;
        } finally {
            Arrays.fill(entropy, (byte) 0);
        }
    }

    @NotNull
    private static Map<String, Integer> buildWordIndices() {
        Map<String, Integer> indices = new HashMap<>(DDBip39Wordlist.WORDS.size());
        for (int i = 0; i < DDBip39Wordlist.WORDS.size(); i++) {
            indices.put(DDBip39Wordlist.WORDS.get(i), i);
        }
        return Map.copyOf(indices);
    }
}
