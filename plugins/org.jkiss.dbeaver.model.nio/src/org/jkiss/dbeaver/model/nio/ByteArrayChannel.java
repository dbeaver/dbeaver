/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.jkiss.dbeaver.model.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// copy of jdk.nio.zipfs.ByteArrayChannel
public class ByteArrayChannel implements SeekableByteChannel {

    private final ReadWriteLock rwlock = new ReentrantReadWriteLock();
    private byte buf[];

    /*
     * The current position of this channel.
     */
    private int pos;

    /*
     * The index that is one greater than the last valid byte in the channel.
     */
    private int last;

    private boolean closed;
    private boolean readonly;

    /*
     * Creates a {@code ByteArrayChannel} with size {@code sz}.
     */
    public ByteArrayChannel(int sz, boolean readonly) {
        this.buf = new byte[sz];
        this.pos = this.last = 0;
        this.readonly = readonly;
    }

    /*
     * Creates a ByteArrayChannel with its 'pos' at 0 and its 'last' at buf's end.
     * Note: no defensive copy of the 'buf', used directly.
     */
    public ByteArrayChannel(byte[] buf, boolean readonly) {
        this.buf = buf;
        this.pos = 0;
        this.last = buf.length;
        this.readonly = readonly;
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public long position() throws IOException {
        beginRead();
        try {
            ensureOpen();
            return pos;
        } finally {
            endRead();
        }
    }

    @Override
    public SeekableByteChannel position(long pos) throws IOException {
        beginWrite();
        try {
            ensureOpen();
            if (pos < 0 || pos >= Integer.MAX_VALUE)
                throw new IllegalArgumentException("Illegal position " + pos);
            this.pos = Math.min((int) pos, last);
            return this;
        } finally {
            endWrite();
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        beginWrite();
        try {
            ensureOpen();
            if (pos == last)
                return -1;
            int n = Math.min(dst.remaining(), last - pos);
            dst.put(buf, pos, n);
            pos += n;
            return n;
        } finally {
            endWrite();
        }
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        if (readonly)
            throw new NonWritableChannelException();
        ensureOpen();
        throw new UnsupportedOperationException();
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (readonly)
            throw new NonWritableChannelException();
        beginWrite();
        try {
            ensureOpen();
            int n = src.remaining();
            ensureCapacity(pos + n);
            src.get(buf, pos, n);
            pos += n;
            if (pos > last) {
                last = pos;
            }
            return n;
        } finally {
            endWrite();
        }
    }

    @Override
    public long size() throws IOException {
        beginRead();
        try {
            ensureOpen();
            return last;
        } finally {
            endRead();
        }
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;
        beginWrite();
        try {
            closed = true;
            buf = null;
            pos = 0;
            last = 0;
        } finally {
            endWrite();
        }
    }

    /**
     * Creates a newly allocated byte array. Its size is the current
     * size of this channel and the valid contents of the buffer
     * have been copied into it.
     *
     * @return the current contents of this channel, as a byte array.
     */
    public byte[] toByteArray() {
        beginRead();
        try {
            // avoid copy if last == bytes.length?
            return Arrays.copyOf(buf, last);
        } finally {
            endRead();
        }
    }

    private void ensureOpen() throws IOException {
        if (closed)
            throw new ClosedChannelException();
    }

    final void beginWrite() {
        rwlock.writeLock().lock();
    }

    final void endWrite() {
        rwlock.writeLock().unlock();
    }

    private final void beginRead() {
        rwlock.readLock().lock();
    }

    private final void endRead() {
        rwlock.readLock().unlock();
    }

    private void ensureCapacity(int minCapacity) {
        // overflow-conscious code
        if (minCapacity - buf.length > 0) {
            grow(minCapacity);
        }
    }

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * Increases the capacity to ensure that it can hold at least the
     * number of elements specified by the minimum capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     */
    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = buf.length;
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        buf = Arrays.copyOf(buf, newCapacity);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError("Required length exceeds implementation limit");
        return (minCapacity > MAX_ARRAY_SIZE) ?
            Integer.MAX_VALUE :
            MAX_ARRAY_SIZE;
    }
}