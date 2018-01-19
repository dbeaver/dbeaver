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
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.utils.CommonUtils;

import java.util.Map;
import java.util.Random;

/**
 * Simple string generator (lorem ipsum)
 */
public class SimpleStringGenerator implements MockValueGenerator {

    private static Random random = new Random();

    private String templateString;

    @Override
    public void init(DBSDataManipulator container, Map<String, Object> properties) throws DBCException {
        templateString = CommonUtils.toString(properties.get("template"));
        if (templateString == null) {
            throw new DBCException("Empty template string for simple string generator");
        }
    }

    @Override
    public void nextRow() {

    }

    @Override
    public Object generateValue(DBDAttributeBinding attribute) throws DBCException {
        int length = (int) Math.min(10000, attribute.getMaxLength());
        int tplLength = templateString.length();
        int start = random.nextInt(tplLength);
        if (start + length < tplLength) {
            return templateString.substring(start, start + length);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(templateString.substring(start));
            int newlength = length - (tplLength - start);
            for (int i = 0; i < newlength / tplLength; i++) {
                sb.append(templateString);
            }
            sb.append(templateString.substring(0, newlength % tplLength));
            return sb.toString();
        }
    }

    @Override
    public void dispose() {

    }
}
