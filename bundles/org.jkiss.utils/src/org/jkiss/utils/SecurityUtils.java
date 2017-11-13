/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Come security-related functions.
 */
public class SecurityUtils {

    public static String ECRYPTION_ALGORYTHM = "MD5";

    private static java.util.Random random;
    private static java.util.Random secureRand;

    static {
        secureRand = new java.util.Random(System.currentTimeMillis());
        long secureInitializer = secureRand.nextLong();
        random = new java.util.Random(secureInitializer);
    }

    /**
     * Generate the random GUID
     */
    public static String generateGUID(boolean secure) {
        String localHostAddr;
        {
            java.net.InetAddress id;
            try {
                id = java.net.InetAddress.getLocalHost();
                localHostAddr = id.toString();
            } catch (java.net.UnknownHostException e) {
                localHostAddr = "localhost";
            }
        }

        long time = System.currentTimeMillis();
        long rand;

        if (secure) {
            rand = secureRand.nextLong();
        } else {
            rand = SecurityUtils.random.nextLong();
        }

        // This StringBuilder can be a long as you need; the MD5
        // hash will always return 128 bits.  You can change
        // the seed to include anything you want here.
        // You could even stream a file through the MD5 making
        // the odds of guessing it at least as great as that
        // of guessing the contents of the file!
        StringBuilder sb = new StringBuilder(32);
        sb.append(localHostAddr)
            .append(":")
            .append(Long.toString(time))
            .append(":")
            .append(Long.toString(rand));


        byte[] array;
        try {
            MessageDigest md5 = MessageDigest.getInstance(ECRYPTION_ALGORYTHM);
            md5.update(sb.toString().getBytes());
            array = md5.digest();
        } catch (NoSuchAlgorithmException e) {
            // Too bad. Lets get simple random numbers
            array = new byte[16];
            random.nextBytes(array);
        }
        sb.setLength(0);
        for (int j = 0; j < array.length; ++j) {
            int b = array[j] & 0xFF;
            if (b < 0x10) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(b));
        }

        String raw = sb.toString().toUpperCase();
        sb.setLength(0);
        sb.append(raw.substring(0, 8));
        sb.append("-");
        sb.append(raw.substring(8, 12));
        sb.append("-");
        sb.append(raw.substring(12, 16));
        sb.append("-");
        sb.append(raw.substring(16, 20));
        sb.append("-");
        sb.append(raw.substring(20));

        return sb.toString();
    }

    public static String generateUniqueId() {
        long curTime = System.currentTimeMillis();
        int random = secureRand.nextInt();
        if (random < 0) {
            random = -random;
        }

        return
            Long.toString(curTime, Character.MAX_RADIX) +
                Integer.toString(random, Character.MAX_RADIX);
    }

    public static String makeDigest(
        String userAlias,
        String userPassword) {
        try {
            if (userPassword == null) {
                userPassword = "";
            }
            MessageDigest md5 =
                MessageDigest.getInstance(ECRYPTION_ALGORYTHM);
            md5.update(userAlias.getBytes());

            return Base64.encode(md5.digest(userPassword.getBytes()));
        } catch (NoSuchAlgorithmException toCatch) {
            return "*";
        }
    }

    public static String makeDigest(
        String userPassword) {
        try {
            MessageDigest md5 =
                MessageDigest.getInstance(ECRYPTION_ALGORYTHM);

            return Base64.encode(md5.digest(userPassword.getBytes()));
        } catch (NoSuchAlgorithmException toCatch) {
            return "*";
        }
    }

    /**
     * Generate a random password of the given length.
     */
    public static String generatePassword(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder pass = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            pass.append(
                PASSWORD_ALPHABET[random.nextInt(PASSWORD_ALPHABET.length)]
            );
        }
        return (pass.toString());
    }

    /**
     * Generate a random password of the default length (8).
     */
    public static String generatePassword() {
        return generatePassword(DEFAULT_PASSWORD_LENGTH);
    }

    public static Random getRandom() {
        return random;
    }

    /**
     * Default length for passwords
     */
    public static final int DEFAULT_PASSWORD_LENGTH = 8;

    /**
     * Alphabet consisting of upper and lowercase letters A-Z and
     * the digits 0-9.
     */
    public static final char[] PASSWORD_ALPHABET = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
        'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
        'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
        'w', 'x', 'y', 'z', '0', '1', '2', '3',
        '4', '5', '6', '7', '8', '9',
    };

}
