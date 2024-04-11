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
package org.jkiss.dbeaver.utils;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Represents node of single-linked-list without any unwanted computational cost.
 * Takes 0 resources to represent empty list with NULL value.
 * @param <T>
 */
public class ListNode<T> implements Iterable<T> {
    public final ListNode<T> next;
    public final T data;

    private ListNode(@Nullable ListNode<T> next, T data) {
        this.next = next;
        this.data = data;
    }
    
    public static <T> boolean hasAny(@Nullable ListNode<T> list) {
        return list != null;
    }

    public static <T> boolean hasOne(@Nullable ListNode<T> list) {
        return list != null && list.next == null;
    }

    public static <T> boolean hasMany(@Nullable ListNode<T> list) {
        return list != null && list.next != null;
    }
    
    @NotNull
    public static <T> ListNode<T> of(@Nullable T data) {
        return new ListNode<T>(null, data);
    }

    @NotNull
    public static <T> ListNode<T> of(@NotNull T data1, @NotNull T data2) {
        return new ListNode<T>(new ListNode<T>(null, data1), data2);
    }

    @NotNull
    public static <T> ListNode<T> push(@Nullable ListNode<T> node, @NotNull T data) {
        return new ListNode<T>(node, data);
    }

    /**
     * Join of two linked lists
     *
     * @return - result of join
     */
    @NotNull
    public static <T> ListNode<T> join(@Nullable ListNode<T> nodes, @NotNull ListNode<T> joinList) {
        Iterator<T> itr = joinList.iterator();
        while (itr.hasNext()) {
            nodes = ListNode.push(nodes, itr.next());
        }
        return nodes;
    }

    @NotNull
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private ListNode<T> node = ListNode.this;

            @Override
            public boolean hasNext() {
                return node != null;
            }

            @Override
            @NotNull
            public T next() {
                if (node != null) {
                    T result = node.data;
                    node = node.next;
                    return result;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
}
