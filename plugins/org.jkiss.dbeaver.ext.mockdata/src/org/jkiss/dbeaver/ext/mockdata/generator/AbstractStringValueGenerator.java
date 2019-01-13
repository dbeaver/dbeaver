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
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;

import java.util.Map;

public abstract class AbstractStringValueGenerator extends AbstractMockValueGenerator {

    private boolean lowercase;
    private boolean uppercase;

    @Override
    public void init(DBSDataManipulator container, DBSAttributeBase attribute, Map<Object, Object> properties) throws DBException {
        super.init(container, attribute, properties);

        Boolean lowercase = (Boolean) properties.get("lowercase"); //$NON-NLS-1$
        if (lowercase != null) {
            this.lowercase = lowercase;
        }

        Boolean uppercase = (Boolean) properties.get("uppercase"); //$NON-NLS-1$
        if (uppercase != null) {
            this.uppercase = uppercase;
        }
    }

    protected String tune(String value) {
        if (value == null) {
            return null;
        }
        if (uppercase) {
            return value.toUpperCase();
        }
        if (lowercase) {
            return value.toLowerCase();
        }
        return value;
    }
}
