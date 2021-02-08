/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dashboard.model;

import org.jfree.chart.axis.NumberTickUnitSource;
import org.jfree.chart.axis.StandardTickUnitSource;
import org.jfree.chart.axis.TickUnitSource;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.ByteNumberFormat;

import java.time.Duration;

/**
 * DashboardUtils
 */
public class DashboardUtils {

    private static final Log log = Log.getLog(DashboardUtils.class);

    public static long parseDuration(String duration, long defValue) {
        if (!duration.startsWith("PT")) duration = "PT" + duration;
        duration = duration.replace(" ", "");
        try {
            Duration newDuration = Duration.parse(duration);
            return newDuration.toMillis();
        } catch (Exception e1) {
            // Ignore
            return defValue;
        }

    }

    public static String formatDuration(long duration) {
        return Duration.ofMillis(duration).toString().substring(2);
    }

    public static TickUnitSource getTickUnitsSource(DashboardValueType valueType) {
        switch (valueType) {
            case decimal:
                return new NumberTickUnitSource(false);
            case integer:
            case percent:
                return new NumberTickUnitSource(true);
            case bytes:
                return new NumberTickUnitSource(true, new ByteNumberFormat());
            default:
                return new StandardTickUnitSource();
        }
    }
}
