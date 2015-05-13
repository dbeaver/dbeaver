/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.utils;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Common utils
 */
public class ArrayUtils {


    public static boolean isEmpty(@Nullable Object[] arr)
    {
        return arr == null || arr.length == 0;
    }

    public static boolean isEmpty(@Nullable short[] array)
    {
        return array == null || array.length == 0;
    }

    public static boolean contains(@Nullable short[] array, short value)
    {
        if (array == null)
            return false;
        for (int i = 0, arrayLength = array.length; i < arrayLength; i++) {
            if (array[i] == value)
                return true;
        }
        return false;
    }

    public static boolean contains(@Nullable char[] array, char value)
    {
        if (array == null || array.length == 0)
            return false;
        for (int i = 0, arrayLength = array.length; i < arrayLength; i++) {
            if (array[i] == value)
                return true;
        }
        return false;
    }

    public static boolean isEmpty(@Nullable int[] array)
    {
        return array == null || array.length == 0;
    }

    public static boolean contains(@Nullable int[] array, int value)
    {
        if (array == null)
            return false;
        for (int v : array) {
            if (v == value)
                return true;
        }
        return false;
    }

    public static boolean isEmpty(@Nullable long[] array)
    {
        return array == null || array.length == 0;
    }

    public static boolean contains(@Nullable long[] array, long value)
    {
        if (array == null)
            return false;
        for (long v : array) {
            if (v == value)
                return true;
        }
        return false;
    }

    public static <OBJECT_TYPE> boolean contains(OBJECT_TYPE[] array, OBJECT_TYPE value)
    {
        if (isEmpty(array))
            return false;
        for (OBJECT_TYPE v : array) {
            if (CommonUtils.equalObjects(value, v))
                return true;
        }
        return false;
    }

    public static <OBJECT_TYPE> boolean contains(OBJECT_TYPE[] array, OBJECT_TYPE... values)
    {
        if (isEmpty(array))
            return false;
        for (OBJECT_TYPE v : array) {
            for (OBJECT_TYPE v2 : values) {
                if (CommonUtils.equalObjects(v, v2))
                    return true;
            }
        }
        return false;
    }

    @NotNull
    public static <T> T[] concatArrays(@NotNull T[] first, @NotNull T[] second)
    {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    @NotNull
    public static <T> Collection<T> safeArray(@Nullable T[] array)
    {
        if (array == null) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(array);
        }
    }

    public static <T> int indexOf(T[] array, T element) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == element) {
                return i;
            }
        }
        return -1;
    }

    public static <T> T[] deleteArea(Class<T> type, T[] elements, int from, int to) {
        int delCount = to - from + 1;
        T[] newArray = (T[]) Array.newInstance(type, elements.length - delCount);
        System.arraycopy(elements, 0, newArray, 0, from);
        if (to < elements.length - 1) {
            System.arraycopy(elements, to + 1, newArray, from, elements.length - from - delCount);
        }

        return newArray;
    }

    public static <T> T[] insertArea(Class<T> type, Object[] elements, int pos, Object[] add) {
        T[] newArray = (T[]) Array.newInstance(type, elements.length + add.length);
        System.arraycopy(elements, 0, newArray, 0, pos);
        System.arraycopy(add, 0, newArray, pos, add.length);
        System.arraycopy(elements, pos, newArray, pos + add.length, elements.length - pos);
        return newArray;
    }
}
