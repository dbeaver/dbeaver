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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class DateRandomGenerator extends AbstractMockValueGenerator {

    private static final Log log = Log.getLog(DateRandomGenerator.class);

    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy"); //$NON-NLS-1$

    public static final long DEFAULT_START_DATE = -946771200000L; // January 1, 1940
    public static final long DAY_RANGE          = 24 * 60 * 60 * 1000; // 1 day
    public static final long YEAR_RANGE         = 365 * DAY_RANGE; // 1 years
    public static final long DEFAULT_RANGE      = 100L * YEAR_RANGE; // 100 years

    private long startDate = Long.MAX_VALUE;
    private long endDate = Long.MAX_VALUE;

    @Override
    public void init(DBSDataManipulator container, DBSAttributeBase attribute, Map<Object, Object> properties) throws DBException {
        super.init(container, attribute, properties);

        String fromDate = (String) properties.get("startDate"); //$NON-NLS-1$
        if (fromDate != null) {
            try {
                this.startDate = DATE_FORMAT.parse(fromDate).getTime();
            } catch (ParseException e) {
                log.error("Error parse Start Date '" + fromDate + "'.", e);
            }
        }

        String toDate = (String) properties.get("endDate"); //$NON-NLS-1$
        if (toDate != null) {
            try {
                this.endDate = DATE_FORMAT.parse(toDate).getTime();
            } catch (ParseException e) {
                log.error("Error parse End Date '" + toDate + "'.", e);
            }
        }

        if (startDate != Long.MAX_VALUE && endDate != Long.MAX_VALUE && (startDate > endDate)) { // swap start & end
            long l = startDate;
            startDate = endDate;
            endDate = l;
        }

        if (endDate != Long.MAX_VALUE) {
            endDate += DAY_RANGE - 1; // include whole the day
        }
    }

    @Override
    public Object generateOneValue(DBRProgressMonitor monitor) throws DBException, IOException {
        if (isGenerateNULL()) {
            return null;
        } else {
            if (startDate != Long.MAX_VALUE && endDate != Long.MAX_VALUE) {
                return new Date(
                        startDate + (Math.abs(random.nextLong()) % (endDate - startDate)));
            } else if (startDate != Long.MAX_VALUE) {
                return new Date(
                        startDate + (Math.abs(random.nextLong()) % DEFAULT_RANGE));
            } else if (endDate != Long.MAX_VALUE) {
                return new Date(
                        (endDate - DEFAULT_RANGE) + (Math.abs(random.nextLong()) % DEFAULT_RANGE));
            }
            return new Date(
                    DEFAULT_START_DATE + (Math.abs(random.nextLong()) % DEFAULT_RANGE));
        }
    }
}
