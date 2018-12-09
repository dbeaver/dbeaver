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
import org.jkiss.dbeaver.ext.mockdata.model.MockValueGenerator;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSAttributeEnumerable;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.io.IOException;
import java.util.*;

public abstract class AbstractMockValueGenerator implements MockValueGenerator {

    public static final int UNIQUE_VALUES_SET_SIZE = 1000000;
    public static final int UNIQUE_VALUE_GEN_ATTEMPTS = 100;

    protected DBSEntity dbsEntity;
    protected DBSAttributeBase attribute;

    protected Random random = new Random();
    protected int nullsPersent = 10;
    private boolean isFirstRun = true;
    private boolean isUnique;
    private Set<Object> uniqueValues;

    /**
     * Should be run before the generateValue call
     */
    @Override
    public void init(DBSDataManipulator container, DBSAttributeBase attribute, Map<Object, Object> properties) throws DBException {
        this.dbsEntity = (DBSEntity) container;
        this.attribute = attribute;

        if (attribute.isRequired()) {
            nullsPersent = 0;
        } else {
            if (properties.get("nulls") != null) {
                nullsPersent = (int) properties.get("nulls");
            }
        }
        if (nullsPersent > 100) {
            nullsPersent = 100;
        } else
        if (nullsPersent < 0) {
            nullsPersent = 0;
        }
    }

    @Override
    public void nextRow() {
    }

    @Override
    public void dispose() {
    }

    @Override
    public Object generateValue(DBRProgressMonitor monitor) throws DBException, IOException {
        if (isFirstRun) {
            isFirstRun = false;
            isUnique = (MockDataUtils.checkUnique(monitor, dbsEntity, attribute) == MockDataUtils.UNIQ_TYPE.SINGLE);
            if (isUnique && (attribute instanceof DBSAttributeEnumerable)) {
                uniqueValues = new HashSet<>();
                Collection<DBDLabelValuePair> valuePairs = readColumnValues(monitor, (DBSAttributeEnumerable) attribute, UNIQUE_VALUES_SET_SIZE);
                for (DBDLabelValuePair pair : valuePairs) {
                    uniqueValues.add(pair.getValue());
                }

            }
        }
        if (isUnique && uniqueValues != null) {
            int attempts = 0;
            Object value = null;
            while (value == null || uniqueValues.contains(value)) {
                if (attempts > UNIQUE_VALUE_GEN_ATTEMPTS) {
                    throw new DBException("\n      Can't generate appropriate unique value for the '" + attribute.getName() + "' <" + attribute.getFullTypeName() + "> attribute.\n" +
                            "      Try to change the generator or its parameters.\n");
                }
                if (monitor.isCanceled()) {
                    return null;
                }
                value = generateOneValue(monitor);
                attempts++;
            }
            uniqueValues.add(value);
            return value;
        } else {
            return generateOneValue(monitor);
        }
    }

    protected abstract Object generateOneValue(DBRProgressMonitor monitor) throws DBException, IOException;

    protected boolean isGenerateNULL() {
        if ((nullsPersent > 0) && ((nullsPersent == 100) || (random.nextInt(100) <= nullsPersent))) {
            return true;
        }
        else {
            return false;
        }
    }

    protected Collection<DBDLabelValuePair> readColumnValues(DBRProgressMonitor monitor, DBSAttributeEnumerable column, int number) throws DBException {
        DBCSession session = DBUtils.openUtilSession(monitor, dbsEntity, "Read value enumeration");
        return column.getValueEnumeration(session, null, number);
    }

    protected Boolean getBooleanProperty(Map<Object, Object> properties, String propName) {
        Object prop = properties.get(propName);
        if (prop != null) {
            if (prop instanceof Boolean) {
                return  (Boolean) prop;
            } else {
                return Boolean.valueOf(prop.toString());
            }
        }
        return null;
    }

    protected Double getDoubleProperty(Map<Object, Object> properties, String propName) {
        Object prop = properties.get(propName);
        if (prop != null) {
            if (prop instanceof Double) {
                return  (Double) prop;
            } else {
                return Double.valueOf(prop.toString());
            }
        }
        return null;
    }

    protected Long getLongProperty(Map<Object, Object> properties, String propName) {
        Object prop = properties.get(propName);
        if (prop != null) {
            if (prop instanceof Long) {
                return  (Long) prop;
            } else {
                return Long.valueOf(prop.toString());
            }
        }
        return null;
    }
}
