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

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.Comparator;

public class PostgreIntervalValueHandlerTest extends DBeaverUnitTest {

    PostgreIntervalValueHandler postgreIntervalValueHandler = new PostgreIntervalValueHandler();

    @Test
    public void getComparator_whenHappyCase_thenSuccess() {

        //GIVEN
        String interval1 = "9 days";
        String interval2 = "1 year";
        Comparator<Object> comparator = postgreIntervalValueHandler.getComparator();

        //WHEN
        int compare = comparator.compare(interval1, interval2);

        //THEN
        Assert.assertTrue(compare < 0);
    }

    @Test
    public void getComparator_whenNegativeDate_thenSuccess() {

        //GIVEN
        String interval1 = "9 days";
        String interval2 = "-1 year";
        Comparator<Object> comparator = postgreIntervalValueHandler.getComparator();

        //WHEN
        int compare = comparator.compare(interval1, interval2);

        //THEN
        Assert.assertTrue(compare > 0);
    }

    @Test
    public void getComparator_whenComplexDate_thenSuccess() {

        //GIVEN
        String interval1 = "3 year 2 mon 9 days";
        String interval2 = "3 year 3 mon 9 days";
        Comparator<Object> comparator = postgreIntervalValueHandler.getComparator();

        //WHEN
        int compare = comparator.compare(interval1, interval2);

        //THEN
        Assert.assertTrue(compare < 0);
    }

    @Test
    public void getComparator_whenComplexDateTime_thenSuccess() {

        //GIVEN
        String interval1 = "3 year 2 mon 9 days 06:10:32";
        String interval2 = "3 year 2 mon 9 days 05:10:31";
        Comparator<Object> comparator = postgreIntervalValueHandler.getComparator();

        //WHEN
        int compare = comparator.compare(interval1, interval2);

        //THEN
        Assert.assertTrue(compare > 0);
    }
}
