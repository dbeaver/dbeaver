/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.auth;

import org.bouncycastle.crypto.PasswordConverter;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.jkiss.code.NotNull;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Argon2IdHasher {

    private static final int SALT_LEN = 16;
    private static final int HASH_LEN = 32;
    private static final int ITERATIONS = 2;
    private static final int MEMORY_KIB = 32 * 1024;
    private static final int PARALLELISM = 1;

    private static final Pattern PHC = Pattern.compile(
        "^\\$(argon2id)\\$v=(\\d+)\\$m=(\\d+),t=(\\d+),p=(\\d+)\\$([A-Za-z0-9+/=]+)\\$([A-Za-z0-9+/=]+)$");
    private static final SecureRandom RNG = new SecureRandom();

    private Argon2IdHasher() {
    }

    /**
     * Return PHC string $argon2id$v=19$m=...,t=...,p=...$<base64(salt)>$<base64(hash)>
     * <a href="https://github.com/P-H-C/phc-string-format/blob/master/phc-sf-spec.md>phc string</a>
     */
    @NotNull
    public static String hash(@NotNull String password) {

        byte[] salt = new byte[SALT_LEN];
        RNG.nextBytes(salt);

        byte[] hash = computeHash(password.getBytes(), salt,
            ITERATIONS, MEMORY_KIB, PARALLELISM, HASH_LEN);

        Base64.Encoder b64 = Base64.getEncoder();
        String saltB64 = b64.encodeToString(salt);
        String hashB64 = b64.encodeToString(hash);

        return "$argon2id$v=19"
            + String.format("$m=%d,t=%d,p=%d", MEMORY_KIB, ITERATIONS, PARALLELISM)
            + "$" + saltB64
            + "$" + hashB64;
    }

    private static byte[] computeHash(byte[] pwd, byte[] salt,
                                      int iterations, int memoryKiB, int parallelism, int outLen) {

        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(iterations)
            .withMemoryAsKB(memoryKiB)
            .withParallelism(parallelism)
            .withSalt(salt)
            .withCharToByteConverter(PasswordConverter.UTF8)
            .build();

        Argon2BytesGenerator gen = new Argon2BytesGenerator();
        gen.init(params);

        byte[] out = new byte[outLen];
        gen.generateBytes(pwd, out, 0, out.length);
        return out;
    }

    private static Phc parsePhc(String phc) {
        Matcher m = PHC.matcher(phc);
        if (!m.matches()) throw new IllegalArgumentException("Invalid PHC format");

        String variant = m.group(1);
        int version = Integer.parseInt(m.group(2));
        int mKiB = Integer.parseInt(m.group(3));
        int t = Integer.parseInt(m.group(4));
        int p = Integer.parseInt(m.group(5));
        byte[] salt = Base64.getDecoder().decode(m.group(6));
        byte[] hash = Base64.getDecoder().decode(m.group(7));

        return new Phc(variant, version, mKiB, t, p, salt, hash);
    }

    public static boolean verify(String storedPhc, String rawPassword) {

        Phc parsed = parsePhc(storedPhc);
        if (!"argon2id".equals(parsed.variant) || parsed.version != 19) {
            throw new IllegalArgumentException("Unsupported Argon2 variant/version in PHC");
        }
        byte[] recomputed = computeHash(
            rawPassword.getBytes(),
            parsed.salt,
            parsed.t,
            parsed.mKiB,
            parsed.p,
            parsed.hash.length
        );

        return MessageDigest.isEqual(recomputed, parsed.hash);
    }

    private record Phc(String variant,
        int version,
        int mKiB,
        int t,
        int p,
        byte[] salt,
        byte[] hash
    ) {}

}
