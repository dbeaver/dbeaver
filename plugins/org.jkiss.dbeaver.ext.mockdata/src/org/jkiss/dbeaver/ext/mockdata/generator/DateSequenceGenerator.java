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

public class DateSequenceGenerator extends AbstractMockValueGenerator {

    private static final Log log = Log.getLog(DateSequenceGenerator.class);

    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy"); //$NON-NLS-1$

    public static final long DAY_RANGE = 24 * 60 * 60 * 1000; // 1 day

    private long startDate = Long.MAX_VALUE;
    private boolean reverse = false;
    private long step = DAY_RANGE; // in ms

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

        // default is today
        if (startDate == Long.MAX_VALUE) {
            startDate = new Date().getTime();
        }

        Integer step = (Integer) properties.get("step"); //$NON-NLS-1$
        if (step != null) {
            this.step = step * DAY_RANGE;
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
            long value = this.startDate;
            if (reverse) {
                startDate -= step;
            } else {
                startDate += step;
            }

            return new Date(value);
        }
    }
}
