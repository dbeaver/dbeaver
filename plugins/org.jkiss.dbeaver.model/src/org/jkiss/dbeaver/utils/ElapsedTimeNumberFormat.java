/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.utils;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * Elapsed time formatter
 */
public class ElapsedTimeNumberFormat extends NumberFormat {
    private static final long serialVersionUID = 1;

    public ElapsedTimeNumberFormat() {
        super();
    }

    /**
     * Formats a number into the specified string buffer.
     *
     * @param number  the number to format.
     * @param toAppendTo  the string buffer.
     * @param pos  the field position (ignored here).
     *
     * @return The string buffer.
     */
    @Override
    public StringBuffer format(double number, StringBuffer toAppendTo,
                               FieldPosition pos) {
        return toAppendTo.append(RuntimeUtils.formatExecutionTime((long) number));
    }

    /**
     * Formats a number into the specified string buffer.
     *
     * @param number  the number to format.
     * @param toAppendTo  the string buffer.
     * @param pos  the field position (ignored here).
     *
     * @return The string buffer.
     */
    @Override
    public StringBuffer format(long number, StringBuffer toAppendTo,
                               FieldPosition pos) {
        return toAppendTo.append(RuntimeUtils.formatExecutionTime(number));
    }

    /**
     * This method returns <code>null</code> for all inputs.  This class cannot
     * be used for parsing.
     *
     * @param source  the source string.
     * @param parsePosition  the parse position.
     *
     * @return <code>null</code>.
     */
    @Override
    public Number parse(String source, ParsePosition parsePosition) {
        return null;
    }

}