/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.utils.Decoder;

import org.jkiss.code.Decoder;

public class Decoder3 implements Decoder {
    @Override
    public int decode(int[] decodedBytes, byte[] obuf, int wp) {
        obuf[wp++] = (byte) (decodedBytes[0] << 2 & 0xfc | decodedBytes[1] >> 4 & 0x3);
        obuf[wp++] = (byte) (decodedBytes[1] << 4 & 0xf0 | decodedBytes[2] >> 2 & 0xf);
        obuf[wp] = (byte) (decodedBytes[2] << 6 & 0xc0 | decodedBytes[3] & 0x3f);
        return 3;
    }
}
