/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStringValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.BeanUtils;

import java.sql.SQLException;
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interval value handler.
 */
public class PostgreIntervalValueHandler extends JDBCStringValueHandler {

    private static final Log log = Log.getLog(PostgreIntervalValueHandler.class);

    public static final PostgreIntervalValueHandler INSTANCE = new PostgreIntervalValueHandler();

    private static final DecimalFormat SECONDS_FORMAT;

    private static final long MILLISECONDS_IN_SECOND = 1000;
    private static final long MILLISECONDS_IN_MINUTE = 60 * MILLISECONDS_IN_SECOND;
    private static final long MILLISECONDS_IN_HOUR = 60 * MILLISECONDS_IN_MINUTE;
    private static final long MILLISECONDS_IN_DAY = 24 * MILLISECONDS_IN_HOUR;
    private static final long MILLISECONDS_IN_MONTH = 30 * MILLISECONDS_IN_DAY;
    private static final long MILLISECONDS_IN_YEAR = 365 * MILLISECONDS_IN_DAY;

    // parsing values like: 3 mons 15 days, 00:00:00.12, -9 days
    private static final String intervalRegex = "(?i)(?<sign>-)?\\s*" +
        "(?:(?<years>\\d+)\\s+years?)?\\s*" +
        "(?:(?<months>\\d+)\\s+mon(?:s|ths)?)?\\s*" +
        "(?:(?<days>\\d+)\\s+days?)?\\s*" +
        "(?:(?<time>\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?))?\\b";
    private static final Pattern pattern = Pattern.compile(intervalRegex);

    static {
        SECONDS_FORMAT = new DecimalFormat("0.00####");
        DecimalFormatSymbols dfs = SECONDS_FORMAT.getDecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        SECONDS_FORMAT.setDecimalFormatSymbols(dfs);
    }

    @Override
    protected Object fetchColumnValue(
        DBCSession session,
        JDBCResultSet resultSet,
        DBSTypedObject type,
        int index)
        throws SQLException
    {
        return resultSet.getString(index);
    }

    @Override
    public void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value) throws SQLException {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else {
            statement.setObject(paramIndex, value.toString(), Types.OTHER);
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        if (value != null && value.getClass().getName().equals(PostgreConstants.PG_INTERVAL_CLASS)) {
            try {
                Number years = (Number) BeanUtils.readObjectProperty(value, "years");
                Number months = (Number) BeanUtils.readObjectProperty(value, "months");
                Number days = (Number) BeanUtils.readObjectProperty(value, "days");
                Number hours = (Number) BeanUtils.readObjectProperty(value, "hours");
                Number minutes = (Number) BeanUtils.readObjectProperty(value, "minutes");
                Number seconds = (Number) BeanUtils.readObjectProperty(value, "seconds");
                StringBuilder str = new StringBuilder();
                if (years != null && years.intValue() > 0) str.append(years).append(" year").append(years.intValue() > 1 ? "s" : "").append(" ");
                if (months != null && months.intValue() > 0) str.append(months).append(" month").append(months.intValue() > 1 ? "s" : "").append(" ");
                if (days != null && days.intValue() > 0) str.append(days).append(" day").append(days.intValue() > 1 ? "s" : "").append(" ");
                str
                    .append(hours).append(":")
                    .append(minutes).append(":")
                    .append(seconds).append(SECONDS_FORMAT.format(seconds));
                return str.toString();
            } catch (Throwable e) {
                log.debug(e);
            }
        }
        return super.getValueDisplayString(column, value, format);
    }


    @Override
    public Comparator<Object> getComparator() {
        return (o1, o2) -> {
            long leftInterval = getSecondsFromInterval((String) o1);
            long rightInterval = getSecondsFromInterval((String) o2);
            return Long.compare(leftInterval, rightInterval);
        };
    }

    private long getSecondsFromInterval(String interval) {

        Matcher matcher = pattern.matcher(interval);
        long totalSeconds = 0;
        if (matcher.find()) {
            String sign = matcher.group("sign") != null ? "-" : "";
            String years = matcher.group("years");
            String months = matcher.group("months");
            String days = matcher.group("days");
            String time = matcher.group("time");

            if (years != null) {
                totalSeconds += Long.parseLong(years) * MILLISECONDS_IN_YEAR;
            }
            if (months != null) {
                totalSeconds += Long.parseLong(months) * MILLISECONDS_IN_MONTH;
            }
            if (days != null) {
                totalSeconds += Long.parseLong(days) * MILLISECONDS_IN_DAY;
            }
            if (time != null) {
                String[] timeParts = time.split(":");
                totalSeconds += (long) Integer.parseInt(timeParts[0]) * MILLISECONDS_IN_HOUR;
                totalSeconds += (long) Integer.parseInt(timeParts[1]) * MILLISECONDS_IN_MINUTE;
                long intValue = (long) Double.parseDouble(timeParts[2]);
                totalSeconds += intValue * MILLISECONDS_IN_SECOND;
                totalSeconds += (long) (Double.parseDouble(timeParts[2]) - intValue);
            }
            if ("-".equals(sign)) {
                totalSeconds = -totalSeconds;
            }
        }

        return totalSeconds;
    }
}