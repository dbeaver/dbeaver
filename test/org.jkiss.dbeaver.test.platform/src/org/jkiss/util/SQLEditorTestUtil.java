/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SQLEditorTestUtil {

    public static final String CURSOR = "<-|";

    private SQLEditorTestUtil() {
    }

    /**
     * Parses the given SQL queries and extracts cursor positions marked by a specific placeholder.
     * The cursor placeholder is removed from the queries, and the method returns a map where
     * the keys are the modified queries (without the cursor marker), and the values are arrays
     * of integers representing the original positions of the cursor markers before removal.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String query = "SELECT * FROM users WHERE name = '<-|Tima'";
     * Map<String, int[]> result = getCursorPositions(query);
     *
     * // Output:
     * // Key: "SELECT * FROM users WHERE name = 'Tima'"
     * // Value: [29] (cursor marker was originally at index 29)
     * }</pre>
     *
     * @param queries The SQL queries containing cursor markers.
     * @return A map where the key is the query with cursor markers removed,
     *         and the value is an array of integers representing the positions
     *         of the removed markers in the original query.
     */
    public static Map<String, int[]> getCursorPositions(String... queries) {
        var result = new HashMap<String, int[]>();

        for (String query : queries) {
            List<Integer> positions = new ArrayList<>();
            StringBuilder sb = new StringBuilder(query);
            int index = 0;
            int adjustment = 0;
            while ((index = sb.indexOf(CURSOR, index)) != -1) {
                positions.add(index - adjustment);
                sb.delete(index, index + CURSOR.length());
                adjustment += CURSOR.length();
            }
            String modifiedQuery = sb.toString();
            int[] indexes = positions.stream()
                .mapToInt(i -> i)
                .toArray();
            result.put(modifiedQuery, indexes);
        }

        return result;
    }
}
