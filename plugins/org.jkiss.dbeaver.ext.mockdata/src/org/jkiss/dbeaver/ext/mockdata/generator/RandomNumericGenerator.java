/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2010-2017 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mockdata.generator;

import org.jkiss.dbeaver.ext.mockdata.model.MockValueGenerator;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;

public class RandomNumericGenerator implements MockValueGenerator {

    private  static int LONG_PRECISION    = String.valueOf(Long.MAX_VALUE).length();    // 19
    private  static int INTEGER_PRECISION = String.valueOf(Integer.MAX_VALUE).length(); // 10
    private  static int SHORT_PRECISION   = String.valueOf(Short.MAX_VALUE).length();   // 5
    private  static int BYTE_PRECISION    = String.valueOf(Byte.MAX_VALUE).length();    // 3

    private Random random = new Random();

    @Override
    public void init(DBSDataManipulator container, Map<String, Object> properties) throws DBCException {

    }

    @Override
    public void nextRow() {

    }

    @Override
    public Object generateValue(DBSAttributeBase attribute) throws DBCException {
        long maxLength = attribute.getMaxLength();
        Integer scale = attribute.getScale();
        Integer precision = attribute.getPrecision();
        // Integers
        if ((scale == null || scale == 0) && (precision != null && precision != 0)) {
            if (precision < BYTE_PRECISION) {
                return new Byte((byte) random.nextInt(degree(r(precision))));
            }
            if (precision < SHORT_PRECISION) {
                return new Short((short) random.nextInt(degree(r(precision))));
            }
            if (precision < INTEGER_PRECISION) {
                return new Integer(random.nextInt(degree(r(precision))));
            }
            if (precision < LONG_PRECISION) {
                return new Long(random.nextLong());
            }

            // Default integer number
            return null; // TODO new BigInteger();
        }
        // Non-integers
        else {
            if (precision != null && precision > 0) {
                int scl = scale != null ? scale : 0;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < precision; i++) {
                    sb.append(random.nextInt(10));
                    if (i == scale) {
                        sb.append('.');
                    }
                }
                return new BigDecimal(sb.reverse().toString());
            } else {
                return new BigDecimal(random.nextLong()); // TODO
            }
        }
    }

    @Override
    public void dispose() {

    }

    private static int degree(int d) {
        int result = 10;
        for (int i = 0; i < d - 1; i++) {
            result *= 10;
        }
        return result;
    }

    private int r(int i) {
        return random.nextInt(i);
    }
}
