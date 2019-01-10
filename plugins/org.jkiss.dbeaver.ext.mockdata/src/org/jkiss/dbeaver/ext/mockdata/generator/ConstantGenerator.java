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
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

public class ConstantGenerator extends AbstractMockValueGenerator {

    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy"); //$NON-NLS-1$

    private Object value;

    @Override
    public void init(DBSDataManipulator container, DBSAttributeBase attribute, Map<Object, Object> properties) throws DBException {
        super.init(container, attribute, properties);

        Object value = properties.get("value"); //$NON-NLS-1$
        if (value != null) {
            if (attribute.getDataKind() == DBPDataKind.DATETIME) {
                try {
                    this.value = DATE_FORMAT.parse((String) value);
                } catch (ParseException e) {
                    throw new DBException("Can't parse the value '" + value + "' as a date", e);
                }
            } else {
                this.value = value;
            }
        } else {
            if (attribute.getDataKind() == DBPDataKind.STRING) {
                this.value = "";
            }
        }
    }

    @Override
    public Object generateOneValue(DBRProgressMonitor monitor) throws DBException, IOException {
        if (isGenerateNULL()) {
            return null;
        } else {
            return value;
        }
    }
}
