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
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.Map;

/**
 * Simple string generator (lorem ipsum)
 */
public class StringTextGenerator extends AbstractStringValueGenerator {

    private String templateString;
    private int minLength = 1;
    private int maxLength = 100;

    @Override
    public void init(DBSDataManipulator container, DBSAttributeBase attribute, Map<Object, Object> properties) throws DBException {
        super.init(container, attribute, properties);

        templateString = CommonUtils.toString(properties.get("template")); //$NON-NLS-1$
        if (templateString == null) {
            throw new DBCException("Empty template string for simple string generator");
        }

        Integer min = (Integer) properties.get("minLength"); //$NON-NLS-1$
        if (min != null) {
            this.minLength = min;
        }
        if (minLength > templateString.length()) {
            minLength = templateString.length();
        }

        Integer max = (Integer) properties.get("maxLength"); //$NON-NLS-1$
        if (max != null) {
            this.maxLength = max;
        }

        if (maxLength == 0 || (attribute.getMaxLength() > 0 && maxLength > attribute.getMaxLength())) {
            maxLength = (int) attribute.getMaxLength();
        }
        if (maxLength > templateString.length()) { // TODO check templateString shouldn't be empty
            maxLength = templateString.length();
        }
        if (minLength > maxLength) {
            maxLength = minLength;
        }
    }

    @Override
    public void nextRow() {

    }

    @Override
    public Object generateOneValue(DBRProgressMonitor monitor) throws DBException, IOException {
        if (isGenerateNULL()) {
            return null;
        } else {
            int length = minLength + random.nextInt(maxLength - minLength + 1);
            int tplLength = templateString.length();
            int start = random.nextInt(tplLength);
            if (start > 0) {
                // Find word begin
                int wordStart = start;
                while (wordStart < tplLength && !Character.isWhitespace(templateString.charAt(wordStart - 1))) {
                    wordStart++;
                }
                if (wordStart < tplLength) {
                    start = wordStart;
                }
            }
            if (start + length < tplLength) {
                return tune(templateString.substring(start, start + length));
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(templateString.substring(start));
                int newlength = length - (tplLength - start);
                for (int i = 0; i < newlength / tplLength; i++) {
                    sb.append(templateString);
                }
                sb.append(templateString.substring(0, newlength % tplLength));
                return tune(sb.toString().trim());
            }
        }
    }
}
