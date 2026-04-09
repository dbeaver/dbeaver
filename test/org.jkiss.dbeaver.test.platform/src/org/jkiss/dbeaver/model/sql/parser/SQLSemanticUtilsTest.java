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
package org.jkiss.dbeaver.model.sql.parser;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertArrayEquals;

public class SQLSemanticUtilsTest extends DBeaverUnitTest {

    static final String exampleQuery = """
        SELECT
            c.customer_id,
            c.first_name,
            c.last_name,
            c.email,
            f.title AS film_title,
            (
                SELECT COUNT(*)
                FROM rental r2
                JOIN inventory i2 ON r2.inventory_id = i2.inventory_id
                WHERE i2.film_id = f.film_id
            ) AS total_rentals_for_film
        FROM
            customer c
            JOIN dvdrental.public.rental r ON c.customer_id = r.customer_id
            JOIN public.inventory i ON r.inventory_id = i.inventory_id
            JOIN film f ON i.film_id = f.film_id
        ORDER BY
            c.customer_id,
            f.title;""";

    private static final List<String> referencedTables = List.of(
        "rental",
        "inventory",
        "customer",
        "dvdrental.public.rental",
        "public.inventory",
        "film"
    );

    @Test
    public void traverseQueryForTableNodesTest() {
        // given
        var query = new SQLQuery(null, exampleQuery);

        // then
        List<String> foundTables = new ArrayList<>();
        SQLSemanticUtils.traverseQueryForTableNodes(
            Objects.requireNonNull(query.getStatement()),
            t -> foundTables.add(t.getFullyQualifiedName())
        );

        assertCollectionsEqual(referencedTables, foundTables);
    }

    private static <T> void assertCollectionsEqual(@NotNull Collection<T> a, @NotNull Collection<T> b) {
        assertArrayEquals(a.toArray(), b.toArray());
    }
}
