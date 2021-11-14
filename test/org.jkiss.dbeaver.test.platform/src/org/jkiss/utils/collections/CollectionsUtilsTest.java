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
package org.jkiss.utils.collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CollectionsUtilsTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testIsEmptyCollection() {
        Collection<Character> value = new ArrayList<>();
        value.add('a');
        assertTrue(CollectionUtils.isEmpty((Collection<Character>) null));
        assertTrue(CollectionUtils.isEmpty(new ArrayList<Character>()));
        assertFalse(CollectionUtils.isEmpty(value));
    }

    @Test
    public void testIsEmptyMap() {
        Map<Integer, Character> value = new HashMap<>();
        value.put(0, 'a');
        assertTrue(CollectionUtils.isEmpty((Map<Integer, Character>) null));
        assertTrue(CollectionUtils.isEmpty(new HashMap<Integer, Character>()));
        assertFalse(CollectionUtils.isEmpty(value));
    }

    @Test
    public void testSafeCollection() {
        Collection<Character> list = new ArrayList<>();
        assertEquals(list, CollectionUtils.safeCollection(null));
        assertEquals(list, CollectionUtils.safeCollection(list));
    }

    @Test
    public void testSafeList() {
        List<Character> list = new ArrayList<>();
        assertEquals(list, CollectionUtils.safeList(null));
        assertEquals(list, CollectionUtils.safeList(list));
    }

    @Test
    public void testCopyList() {
        Collection<Integer> list = new ArrayList<>();
        assertEquals(list, CollectionUtils.copyList(null));

        list.add(0);
        assertEquals(list, CollectionUtils.copyList(list));
    }

    @Test
    public void testGetItem() {
        Collection<String> collectionList = new ArrayList<>();
        collectionList.add("a");
        assertEquals("a", CollectionUtils.getItem(collectionList, 0));

        Set<String> collectionSet = new LinkedHashSet<>();
        collectionSet.add("a");
        collectionSet.add("b");
        assertEquals("b", CollectionUtils.getItem(collectionSet, 1));
    }
    
    @Test
    public void testFilterCollection() {
        Collection<Object> collection = new ArrayList<>();
        collection.add("a");
        collection.add(1);
        assertArrayEquals(new String[] { "a" }, CollectionUtils.filterCollection(collection, String.class).toArray());
    }
    
    @Test
    public void testGroup() {
        final List<String> values = Arrays.asList("aaa", "abb", "bbb", "bab", "ccc");
        final Map<Character, List<String>> groups = CollectionUtils.group(values, x -> x.charAt(0));
        assertEquals(Arrays.asList("aaa", "abb"), groups.get('a'));
        assertEquals(Arrays.asList("bbb", "bab"), groups.get('b'));
        assertEquals(Collections.singletonList("ccc"), groups.get('c'));
    }
}
