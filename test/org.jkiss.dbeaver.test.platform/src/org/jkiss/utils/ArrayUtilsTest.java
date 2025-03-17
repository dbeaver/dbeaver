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
package org.jkiss.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ArrayUtilsTest {

    @Test
    public void testIsEmpty() {
        Object[] arr = null;
        Assert.assertTrue(ArrayUtils.isEmpty(arr));
        Assert.assertTrue(ArrayUtils.isEmpty(new Object[]{}));
        Assert.assertFalse(ArrayUtils.isEmpty(new Object[]{0}));
    }

    @Test
    public void testIsEmptyShort() {
        short[] arr = null;
        Assert.assertTrue(ArrayUtils.isEmpty(arr));
        Assert.assertTrue(ArrayUtils.isEmpty(new short[]{}));
        Assert.assertFalse(ArrayUtils.isEmpty(new short[]{(short) 0}));
    }

    @Test
    public void testContainsShort() {
        short[] arr = null;
        Assert.assertFalse(ArrayUtils.contains(arr, (short) 0));
        Assert.assertFalse(ArrayUtils.contains(new short[]{}, (short) 0));
        Assert.assertFalse(ArrayUtils.contains(new short[]{(short) 1}, (short) 0));
        Assert.assertTrue(ArrayUtils.contains(new short[]{(short) 0}, (short) 0));
    }

    @Test
    public void testContainsChar() {
        char[] arr = null;
        Assert.assertFalse(ArrayUtils.contains(arr, 'a'));
        Assert.assertFalse(ArrayUtils.contains(new char[]{}, 'a'));
        Assert.assertFalse(ArrayUtils.contains(new char[]{'b'}, 'a'));
        Assert.assertTrue(ArrayUtils.contains(new char[]{'a'}, 'a'));
    }

    @Test
    public void testIsEmptyInt() {
        int[] arr = null;
        Assert.assertTrue(ArrayUtils.isEmpty(arr));
        Assert.assertTrue(ArrayUtils.isEmpty(new int[]{}));
        Assert.assertFalse(ArrayUtils.isEmpty(new int[]{0}));
    }

    @Test
    public void testContainsInt() {
        int[] arr = null;
        Assert.assertFalse(ArrayUtils.contains(arr, 0));
        Assert.assertFalse(ArrayUtils.contains(new int[]{}, 0));
        Assert.assertFalse(ArrayUtils.contains(new int[]{1}, 0));
        Assert.assertTrue(ArrayUtils.contains(new int[]{0}, 0));
    }

    @Test
    public void testIsEmptyLong() {
        long[] arr = null;
        Assert.assertTrue(ArrayUtils.isEmpty(arr));
        Assert.assertTrue(ArrayUtils.isEmpty(new long[]{}));
        Assert.assertFalse(ArrayUtils.isEmpty(new long[]{0L}));
    }

    @Test
    public void testContainsLong() {
        long[] arr = null;
        Assert.assertFalse(ArrayUtils.contains(arr, 0L));
        Assert.assertFalse(ArrayUtils.contains(new long[]{}, 0L));
        Assert.assertFalse(ArrayUtils.contains(new long[]{1L}, 0L));
        Assert.assertTrue(ArrayUtils.contains(new long[]{0L}, 0L));
    }

    @Test
    public void testContainsObjectType() {
        String[] arr = null;
        Assert.assertFalse(ArrayUtils.contains(arr, "a"));
        Assert.assertFalse(ArrayUtils.contains(new String[]{}, "a"));
        Assert.assertFalse(ArrayUtils.contains(new String[]{"b"}, "a"));
        Assert.assertTrue(ArrayUtils.contains(new String[]{"a"}, "a"));
    }

    @Test
    public void testContainsIgnoreCase() {
        Assert.assertFalse(ArrayUtils.containsIgnoreCase(new String[]{}, "A"));
        Assert.assertFalse(ArrayUtils.containsIgnoreCase(new String[]{"a"}, null));
        Assert.assertFalse(ArrayUtils.containsIgnoreCase(new String[]{"b"}, "A"));
        Assert.assertTrue(ArrayUtils.containsIgnoreCase(new String[]{"a"}, "A"));
        Assert.assertTrue(ArrayUtils.containsIgnoreCase(new String[]{"a"}, "a"));
    }

    @Test
    public void testContainsRef() {
        Assert.assertFalse(ArrayUtils.containsRef(new String[]{}, "a"));
        Assert.assertFalse(ArrayUtils.containsRef(new String[]{"b"}, "a"));
        Assert.assertTrue(ArrayUtils.containsRef(new String[]{"a"}, "a"));
    }

    @Test
    public void testContains() {
        Assert.assertFalse(ArrayUtils.containsAny(new String[]{}, new String[]{"a"}));
        Assert.assertFalse(ArrayUtils.containsAny(new String[]{"b"}, new String[]{"a"}));
        Assert.assertTrue(ArrayUtils.containsAny(new String[]{"a"}, new String[]{"a"}));
    }

    @Test
    public void testConcatArrays() {
        Assert.assertArrayEquals(new String[]{"a", "b"}, ArrayUtils.concatArrays(new String[]{"a"}, new String[]{"b"}));
    }

    @Test
    public void testSafeArray() {
        String[] arr = null;
        List<Object> emptyList = new ArrayList<>();
        Assert.assertEquals(emptyList, ArrayUtils.safeArray(null));

        List<String> list = new ArrayList<>();
        list.add("a");
        Assert.assertEquals(list, ArrayUtils.safeArray(new String[]{"a"}));
    }

    @Test
    public void testIndexOf() {
        Assert.assertEquals(-1, ArrayUtils.indexOf(new String[]{"a"}, "b"));
        Assert.assertEquals(0, ArrayUtils.indexOf(new String[]{"a"}, "a"));
    }

    @Test
    public void testIndexOfByte() {
        Assert.assertEquals(1, ArrayUtils.indexOf(new byte[]{(byte) 'a', (byte) 'b', 'c'}, 1, (byte) 'b'));
        Assert.assertEquals(-1, ArrayUtils.indexOf(new byte[]{(byte) 'a', (byte) 'b', 'c'}, 1, (byte) 'a'));
    }

    @Test
    public void testDeleteArea() {
        Assert.assertArrayEquals(new Object[]{'a', 'c'}, ArrayUtils.deleteArea(Object.class, new Object[]{'a', 'b', 'c'}, 1, 1));
        Assert.assertArrayEquals(new Object[]{'a'}, ArrayUtils.deleteArea(Object.class, new Object[]{'a', 'b', 'c'}, 1, 2));
    }

    @Test
    public void testInsertArea() {
        Assert.assertArrayEquals(new Object[]{'a', 'b', 'c'}, ArrayUtils.insertArea(Object.class, new Object[]{'a', 'c'}, 1, new Object[]{'b'}));
    }

    @Test
    public void testAdd() {
        Assert.assertArrayEquals(new Object[]{'a', 'b', 'c'}, ArrayUtils.add(Object.class, new Object[]{'a', 'b'}, 'c'));
    }


    @Test
    public void testRemove() {
        Assert.assertArrayEquals(new Object[]{1L, 3L}, ArrayUtils.remove(Object.class, new Object[]{1L, 2L, 3L}, 2L));
        Assert.assertArrayEquals(new Object[]{1L, 2L}, ArrayUtils.remove(Object.class, new Object[]{1L, 2L, 3L}, 3L));
        Assert.assertArrayEquals(new Object[]{1L, 2L, 3L}, ArrayUtils.remove(Object.class, new Object[]{1L, 2L, 3L}, 4L));
    }

    @Test
    public void testToArray() {
        List<String> list = new ArrayList<>();
        list.add("a");
        Assert.assertEquals(new String[]{"a"}, ArrayUtils.toArray(Object.class, list));
    }
}
