/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mockdata.MockDataUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;

import java.io.IOException;
import java.util.Map;

public class NumericSequenceGenerator extends AbstractMockValueGenerator {

    private long start = 1;
    private long step = 1;
    private boolean reverse = false;

    @Override
    public void init(DBSDataManipulator container, DBSAttributeBase attribute, Map<Object, Object> properties) throws DBException {
        super.init(container, attribute, properties);

        Long startProperty = getLongProperty(properties, "start"); //$NON-NLS-1$
        if (startProperty != null) {
            this.start = startProperty;
        }

        Long stepProperty = getLongProperty(properties, "step"); //$NON-NLS-1$
        if (stepProperty != null) {
            this.step = stepProperty;
        }

        Boolean reverse = (Boolean) properties.get("reverse"); //$NON-NLS-1$
        if (reverse != null) {
            this.reverse = reverse;
        }
    }

    @Override
    public Object generateOneValue(DBRProgressMonitor monitor) throws DBException, IOException {
        if (isGenerateNULL()) {
            return null;
        } else {
            long value = this.start;
            if (reverse) {
                start -= step;
            } else {
                start += step;
            }
            Integer precision = attribute.getPrecision();
            if (precision == null || precision < MockDataUtils.INTEGER_PRECISION) { // TODO ???
                return (int)(value);
            }
            if (precision < MockDataUtils.BYTE_PRECISION) {
                return (byte)(value);
            }
            if (precision < MockDataUtils.SHORT_PRECISION) {
                return (short)(value);
            }
            if (precision < MockDataUtils.LONG_PRECISION) {
                return new Long(value);
            }

            return value;
        }
    }
}
