/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.struct;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class DBSObjectFilterTest {

    @Test
    void conjunctionDoesNotAllowUserFilterToBroadenConnectionFilter() {
        DBSObjectFilter connectionFilter = new DBSObjectFilter();
        connectionFilter.setInclude(List.of("prod_%"));
        connectionFilter.setExclude(List.of("prod_internal%"));

        DBSObjectFilter userFilter = new DBSObjectFilter();
        userFilter.setInclude(List.of("%_eu"));
        userFilter.setUserFilter(true);

        DBSObjectFilter effectiveFilter = DBSObjectFilter.conjunction(connectionFilter, userFilter);

        Assertions.assertNotNull(effectiveFilter);
        Assertions.assertTrue(effectiveFilter.matches("prod_eu"));
        Assertions.assertFalse(effectiveFilter.matches("test_eu"));
        Assertions.assertFalse(effectiveFilter.matches("prod_us"));
        Assertions.assertFalse(effectiveFilter.matches("prod_internal_eu"));
    }

    @Test
    void conjunctionIgnoresEmptyAndDisabledFilters() {
        DBSObjectFilter connectionFilter = new DBSObjectFilter();
        connectionFilter.setInclude(List.of("prod_%"));

        DBSObjectFilter emptyUserFilter = new DBSObjectFilter();
        emptyUserFilter.setUserFilter(true);

        Assertions.assertSame(connectionFilter, DBSObjectFilter.conjunction(connectionFilter, emptyUserFilter));

        emptyUserFilter.setInclude(List.of("test_%"));
        emptyUserFilter.setEnabled(false);

        Assertions.assertSame(connectionFilter, DBSObjectFilter.conjunction(connectionFilter, emptyUserFilter));
    }

    @Test
    void conjunctionMatchesAliasesAsTheSameObject() {
        DBSObjectFilter connectionFilter = new DBSObjectFilter();
        connectionFilter.setInclude(List.of("base_name"));
        DBSObjectFilter userFilter = new DBSObjectFilter();
        userFilter.setInclude(List.of("overloaded_name"));

        DBSObjectFilter effectiveFilter = DBSObjectFilter.conjunction(connectionFilter, userFilter);

        Assertions.assertNotNull(effectiveFilter);
        Assertions.assertTrue(effectiveFilter.matchesAny("base_name", "overloaded_name"));
        Assertions.assertFalse(effectiveFilter.matchesAny("other_name", "other_overload"));
    }

    @Test
    void conjunctionCannotBeMutatedDirectly() {
        DBSObjectFilter firstFilter = new DBSObjectFilter();
        firstFilter.setInclude(List.of("first_%"));
        DBSObjectFilter secondFilter = new DBSObjectFilter();
        secondFilter.setInclude(List.of("%_second"));
        DBSObjectFilter effectiveFilter = DBSObjectFilter.conjunction(firstFilter, secondFilter);

        Assertions.assertNotNull(effectiveFilter);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> effectiveFilter.setEnabled(false));
        Assertions.assertThrows(UnsupportedOperationException.class, effectiveFilter::getInclude);
        Assertions.assertThrows(UnsupportedOperationException.class, effectiveFilter::getExclude);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> effectiveFilter.setInclude(List.of("other_%")));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> effectiveFilter.setExclude(List.of("excluded_%")));
    }

    @Test
    void conjunctionStopsApplyingAComponentDisabledAfterComposition() {
        DBSObjectFilter connectionFilter = new DBSObjectFilter();
        connectionFilter.setInclude(List.of("prod_%"));
        DBSObjectFilter userFilter = new DBSObjectFilter();
        userFilter.setInclude(List.of("%_eu"));
        DBSObjectFilter effectiveFilter = DBSObjectFilter.conjunction(connectionFilter, userFilter);

        Assertions.assertNotNull(effectiveFilter);
        Assertions.assertFalse(effectiveFilter.matches("prod_us"));

        userFilter.setEnabled(false);

        Assertions.assertTrue(effectiveFilter.matches("prod_us"));
        Assertions.assertTrue(effectiveFilter.matchesAny("prod_us", "overloaded_name"));
    }

}
