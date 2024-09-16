/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.config.migration.wizards.navicat;

import org.jkiss.utils.CommonUtils;

import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Arrays;

public class NavicatEncrypt {
    private static final String NAVICAT_CODE = "3DC5CA39";
    private byte[] _iv;
    private SecretKeySpec keySpec;
    private javax.crypto.Cipher _chiperCrypt;
    private javax.crypto.Cipher _chiperDecrypt;

    public NavicatEncrypt()
    {
        initKey();
        initChiperEncrypt();
        initChiperDecrypt();
        initIV();
    }

    private void initKey()
    {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.update(NAVICAT_CODE.getBytes("US-ASCII"), 0, NAVICAT_CODE.length());
            byte[] key = md.digest();
            keySpec = new SecretKeySpec(key, "Blowfish");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private void initChiperEncrypt()
    {
        try {
            // Must use NoPadding
            _chiperCrypt = javax.crypto.Cipher.getInstance("Blowfish/ECB/NoPadding");
            _chiperCrypt.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private void initChiperDecrypt()
    {
        try {
            // Must use NoPadding
            _chiperDecrypt = javax.crypto.Cipher.getInstance("Blowfish/ECB/NoPadding");
            _chiperDecrypt.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private void initIV()
    {
        try {
            byte[] initVec = CommonUtils.parseHexString("FFFFFFFFFFFFFFFF");
            _iv = _chiperCrypt.doFinal(initVec);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private void xorBytes(byte[] a, byte[] b)
    {
        for (int i = 0; i < (a.length); i++) {
            int aVal = a[i] & 0xff; // convert byte to integer
            int bVal = b[i] & 0xff;
            a[i] = (byte) (aVal ^ bVal); // xor aVal and bVal and typecast to
                                         // byte
        }
    }

    private void xorBytes(byte[] a, byte[] b, int l)
    {
        for (int i = 0; i < l; i++) {
            int aVal = a[i] & 0xff; // convert byte to integer
            int bVal = b[i] & 0xff;
            a[i] = (byte) (aVal ^ bVal); // xor aVal and bVal and typecast to
                                         // byte
        }
    }

    public String encrypt(String inputString)
    {
        try {
            byte[] inData = inputString.getBytes("US-ASCII");
            byte[] outData = encrypt(inData);

            return CommonUtils.toHexString(outData);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    private byte[] encrypt(byte[] inData)
    {
        try {
            byte[] CV = Arrays.copyOf(_iv, _iv.length);
            byte[] ret = new byte[inData.length];
            int left = inData.length % 8;
            int rounded = Math.floorDiv(inData.length, 8);

            for (int i = 0; i < rounded; i++) {
                byte[] tmp = Arrays.copyOfRange(inData, i * 8, (i * 8) + 8);

                xorBytes(tmp, CV);

                tmp = _chiperCrypt.doFinal(tmp);

                xorBytes(CV, tmp);

                for (int j = 0; j < tmp.length; j++) {
                    ret[i * 8 + j] = tmp[j];
                }
            }

            if (left != 0) {
                CV = _chiperCrypt.doFinal(CV);
                byte[] tmp = Arrays.copyOfRange(inData, rounded * 8, (rounded * 8) + left);

                xorBytes(tmp, CV, left);
                for (int j = 0; j < tmp.length; j++) {
                    ret[rounded * 8 + j] = tmp[j];
                }
            }

            return ret;

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public String decrypt(String hexString)
    {
        try {
            byte[] inData = CommonUtils.parseHexString(hexString);
            byte[] outData = decrypt(inData);

            return new String(outData);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    private byte[] decrypt(byte[] inData)
    {
        try {
            byte[] CV = Arrays.copyOf(_iv, _iv.length);
            byte[] ret = new byte[inData.length];
            int left = inData.length % 8;
            int rounded = Math.floorDiv(inData.length, 8);

            for (int i = 0; i < rounded; i++) {
                byte[] tmp = Arrays.copyOfRange(inData, i * 8, (i * 8) + 8);
                tmp = _chiperDecrypt.doFinal(tmp);
                xorBytes(tmp, CV);

                for (int j = 0; j < tmp.length; j++) {
                    ret[(i * 8) + j] = tmp[j];
                }

                for (int j = 0; j < CV.length; j++) {
                    CV[j] = (byte) (CV[j] ^ inData[i * 8 + j]);
                }
            }

            if (left != 0) {
                CV = _chiperDecrypt.doFinal(CV);
                byte[] tmp = Arrays.copyOfRange(inData, rounded * 8, (rounded * 8) + left);

                xorBytes(tmp, CV, left);
                for (int j = 0; j < tmp.length; j++) {
                    ret[rounded * 8 + j] = tmp[j];
                }
            }

            return ret;

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
