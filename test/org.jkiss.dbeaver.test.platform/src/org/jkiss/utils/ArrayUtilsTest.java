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
package org.jkiss.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class ArrayUtilsTest {

    @Test
    public void testIsEmpty() {
        Object[] arr = null;
        Assertions.assertTrue(ArrayUtils.isEmpty(arr));
        Assertions.assertTrue(ArrayUtils.isEmpty(new Object[]{}));
        Assertions.assertFalse(ArrayUtils.isEmpty(new Object[]{0}));
    }

    @Test
    public void testIsEmptyShort() {
        short[] arr = null;
        Assertions.assertTrue(ArrayUtils.isEmpty(arr));
        Assertions.assertTrue(ArrayUtils.isEmpty(new short[]{}));
        Assertions.assertFalse(ArrayUtils.isEmpty(new short[]{(short) 0}));
    }

    @Test
    public void testContainsShort() {
        short[] arr = null;
        Assertions.assertFalse(ArrayUtils.contains(arr, (short) 0));
        Assertions.assertFalse(ArrayUtils.contains(new short[]{}, (short) 0));
        Assertions.assertFalse(ArrayUtils.contains(new short[]{(short) 1}, (short) 0));
        Assertions.assertTrue(ArrayUtils.contains(new short[]{(short) 0}, (short) 0));
    }

    @Test
    public void testContainsChar() {
        char[] arr = null;
        Assertions.assertFalse(ArrayUtils.contains(arr, 'a'));
        Assertions.assertFalse(ArrayUtils.contains(new char[]{}, 'a'));
        Assertions.assertFalse(ArrayUtils.contains(new char[]{'b'}, 'a'));
        Assertions.assertTrue(ArrayUtils.contains(new char[]{'a'}, 'a'));
    }

    @Test
    public void testIsEmptyInt() {
        int[] arr = null;
        Assertions.assertTrue(ArrayUtils.isEmpty(arr));
        Assertions.assertTrue(ArrayUtils.isEmpty(new int[]{}));
        Assertions.assertFalse(ArrayUtils.isEmpty(new int[]{0}));
    }

    @Test
    public void testContainsInt() {
        int[] arr = null;
        Assertions.assertFalse(ArrayUtils.contains(arr, 0));
        Assertions.assertFalse(ArrayUtils.contains(new int[]{}, 0));
        Assertions.assertFalse(ArrayUtils.contains(new int[]{1}, 0));
        Assertions.assertTrue(ArrayUtils.contains(new int[]{0}, 0));
    }

    @Test
    public void testIsEmptyLong() {
        long[] arr = null;
        Assertions.assertTrue(ArrayUtils.isEmpty(arr));
        Assertions.assertTrue(ArrayUtils.isEmpty(new long[]{}));
        Assertions.assertFalse(ArrayUtils.isEmpty(new long[]{0L}));
    }

    @Test
    public void testContainsLong() {
        long[] arr = null;
        Assertions.assertFalse(ArrayUtils.contains(arr, 0L));
        Assertions.assertFalse(ArrayUtils.contains(new long[]{}, 0L));
        Assertions.assertFalse(ArrayUtils.contains(new long[]{1L}, 0L));
        Assertions.assertTrue(ArrayUtils.contains(new long[]{0L}, 0L));
    }

    @Test
    public void testContainsObjectType() {
        String[] arr = null;
        Assertions.assertFalse(ArrayUtils.contains(arr, "a"));
        Assertions.assertFalse(ArrayUtils.contains(new String[]{}, "a"));
        Assertions.assertFalse(ArrayUtils.contains(new String[]{"b"}, "a"));
        Assertions.assertTrue(ArrayUtils.contains(new String[]{"a"}, "a"));
    }

    @Test
    public void testContainsIgnoreCase() {
        Assertions.assertFalse(ArrayUtils.containsIgnoreCase(new String[]{}, "A"));
        Assertions.assertFalse(ArrayUtils.containsIgnoreCase(new String[]{"a"}, null));
        Assertions.assertFalse(ArrayUtils.containsIgnoreCase(new String[]{"b"}, "A"));
        Assertions.assertTrue(ArrayUtils.containsIgnoreCase(new String[]{"a"}, "A"));
        Assertions.assertTrue(ArrayUtils.containsIgnoreCase(new String[]{"a"}, "a"));
    }

    @Test
    public void testContainsRef() {
        Assertions.assertFalse(ArrayUtils.containsRef(new String[]{}, "a"));
        Assertions.assertFalse(ArrayUtils.containsRef(new String[]{"b"}, "a"));
        Assertions.assertTrue(ArrayUtils.containsRef(new String[]{"a"}, "a"));
    }

    @Test
    public void testContains() {
        Assertions.assertFalse(ArrayUtils.containsAny(new String[]{}, new String[]{"a"}));
        Assertions.assertFalse(ArrayUtils.containsAny(new String[]{"b"}, new String[]{"a"}));
        Assertions.assertTrue(ArrayUtils.containsAny(new String[]{"a"}, new String[]{"a"}));
    }

    @Test
    public void testConcatArrays() {
        Assertions.assertArrayEquals(new String[]{"a", "b"}, ArrayUtils.concatArrays(new String[]{"a"}, new String[]{"b"}));
    }

    @Test
    public void testSafeArray() {
        String[] arr = null;
        List<Object> emptyList = new ArrayList<>();
        Assertions.assertEquals(emptyList, ArrayUtils.safeArray(null));

        List<String> list = new ArrayList<>();
        list.add("a");
        Assertions.assertEquals(list, ArrayUtils.safeArray(new String[]{"a"}));
    }

    @Test
    public void testIndexOf() {
        Assertions.assertEquals(-1, ArrayUtils.indexOf(new String[]{"a"}, "b"));
        Assertions.assertEquals(0, ArrayUtils.indexOf(new String[]{"a"}, "a"));
    }

    @Test
    public void testIndexOfByte() {
        Assertions.assertEquals(1, ArrayUtils.indexOf(new byte[]{(byte) 'a', (byte) 'b', 'c'}, 1, (byte) 'b'));
        Assertions.assertEquals(-1, ArrayUtils.indexOf(new byte[]{(byte) 'a', (byte) 'b', 'c'}, 1, (byte) 'a'));
    }

    @Test
    public void testDeleteArea() {
        Assertions.assertArrayEquals(new Object[]{'a', 'c'}, ArrayUtils.deleteArea(Object.class, new Object[]{'a', 'b', 'c'}, 1, 1));
        Assertions.assertArrayEquals(new Object[]{'a'}, ArrayUtils.deleteArea(Object.class, new Object[]{'a', 'b', 'c'}, 1, 2));
    }

    @Test
    public void testInsertArea() {
        Assertions.assertArrayEquals(new Object[]{'a', 'b', 'c'}, ArrayUtils.insertArea(Object.class, new Object[]{'a', 'c'}, 1, new Object[]{'b'}));
    }

    @Test
    public void testAdd() {
        Assertions.assertArrayEquals(new Object[]{'a', 'b', 'c'}, ArrayUtils.add(Object.class, new Object[]{'a', 'b'}, 'c'));
    }


    @Test
    public void testRemove() {
        Assertions.assertArrayEquals(new Object[]{1L, 3L}, ArrayUtils.remove(Object.class, new Object[]{1L, 2L, 3L}, 2L));
        Assertions.assertArrayEquals(new Object[]{1L, 2L}, ArrayUtils.remove(Object.class, new Object[]{1L, 2L, 3L}, 3L));
        Assertions.assertArrayEquals(new Object[]{1L, 2L, 3L}, ArrayUtils.remove(Object.class, new Object[]{1L, 2L, 3L}, 4L));
    }

    @Test
    public void testToArray() {
        List<String> list = new ArrayList<>();
        list.add("a");
        Assertions.assertArrayEquals(new Object[]{"a"}, ArrayUtils.toArray(Object.class, list));
    }
}
