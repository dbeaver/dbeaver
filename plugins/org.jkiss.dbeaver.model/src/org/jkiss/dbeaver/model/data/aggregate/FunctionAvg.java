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
package org.jkiss.dbeaver.model.data.aggregate;

import java.math.BigDecimal;
import java.math.MathContext;

import org.jkiss.code.Nullable;

/**
 * FunctionAvg
 */
public class FunctionAvg extends FunctionNumeric {

    private BigDecimal preciseResult;

    private double specialResult;
    private boolean hasSpecialValue;

    @Override
    public boolean accumulate(@Nullable Object value, boolean aggregateAsStrings) {
        Number num = getNumericForAggregation(value);
        if (num == null) {
            return false;
        }

        BigDecimal decimal = getBigDecimal(value);

        if (decimal != null) {
            if (preciseResult == null) {
                preciseResult = decimal;
            } else {
                preciseResult = preciseResult.add(decimal);
            }
        } else {
            specialResult += num.doubleValue();
            hasSpecialValue = true;
        }

        return true;
    }

    @Nullable
    @Override
    public Object getResult(int valueCount) {
        if (preciseResult == null && !hasSpecialValue) {
            return null;
        }
        if (valueCount <= 0) {
            return null;
        }

        if (hasSpecialValue) {
            return Double.isNaN(specialResult) ? null : specialResult / valueCount;
        }

        return preciseResult.divide(BigDecimal.valueOf(valueCount), MathContext.DECIMAL128);
    }
}
