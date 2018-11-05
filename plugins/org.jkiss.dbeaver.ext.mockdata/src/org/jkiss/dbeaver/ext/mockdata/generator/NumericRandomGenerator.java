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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mockdata.MockDataUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;

public class NumericRandomGenerator extends AbstractMockValueGenerator {

    @Override
    public Object generateOneValue(DBRProgressMonitor monitor) throws DBException, IOException {
        if (isGenerateNULL()) {
            return null;
        } else {
            long maxLength = attribute.getMaxLength();
            Integer scale = attribute.getScale();
            Integer precision = attribute.getPrecision();

            return MockDataUtils.generateNumeric(precision, scale, 0.0, (double) Short.MAX_VALUE);
        }
    }
}
