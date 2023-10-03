/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.nio.base;

import org.jkiss.code.NotNull;

import java.io.IOException;
import java.nio.file.*;

public abstract class AbstractPath<
    P extends AbstractPath<P, FS, FP>,
    FS extends AbstractFileSystem<P, FS, FP>,
    FP extends AbstractFileSystemProvider<P, FS, FP>> implements Path {

    protected final AbstractFileSystem<P, FS, FP> filesystem;
    protected final AbstractFileSystemProvider<P, FS, FP> provider;
    protected final String path;
    protected volatile int[] offsets;
    protected String resolved;

    public AbstractPath(@NotNull AbstractFileSystem<P, FS, FP> filesystem, @NotNull String path) {
        this(filesystem, path, false);
    }

    public AbstractPath(@NotNull AbstractFileSystem<P, FS, FP> filesystem, @NotNull String path, boolean normalized) {
        this.filesystem = filesystem;
        this.provider = filesystem.provider;
        this.path = normalized ? path : normalize(path);
    }

    @NotNull
    @Override
    public AbstractFileSystem<P, FS, FP> getFileSystem() {
        return filesystem;
    }

    @Override
    public boolean isAbsolute() {
        return !path.isEmpty() && path.charAt(0) == '/';
    }

    @Override
    public Path getRoot() {
        if (isAbsolute()) {
            return filesystem.getRoot();
        } else {
            return null;
        }
    }

    @Override
    public Path getFileName() {
        int off = path.length();

        if (off == 0 || off == 1 && path.charAt(0) == '/') {
            return null;
        }

        initOffsets();

        final int len = offsets.length;

        if (len == 1) {
            return this;
        } else {
            return filesystem.provider.newPath(filesystem, path.substring(offsets[len - 1]));
        }
    }

    @Override
    public Path getParent() {
        int off = path.length();

        if (off == 0 || off == 1 && path.charAt(0) == '/') {
            return null;
        }

        initOffsets();

        final int len = offsets.length;

        if (len == 1) {
            return getRoot();
        } else {
            return filesystem.provider.newPath(filesystem, path.substring(0, offsets[len - 1]));
        }
    }

    @Override
    public int getNameCount() {
        initOffsets();
        return offsets.length;
    }

    @NotNull
    @Override
    public Path getName(int index) {
        initOffsets();

        if (index < 0 || index >= offsets.length) {
            throw new IllegalArgumentException();
        }

        final int begin = offsets[index];
        final int end;

        if (index == (offsets.length - 1)) {
            end = path.length();
        } else {
            end = offsets[index + 1] - 1;
        }

        return filesystem.provider.newPath(filesystem, path.substring(begin, end));
    }

    @NotNull
    @Override
    public Path subpath(int beginIndex, int endIndex) {
        initOffsets();

        if (beginIndex < 0 || beginIndex >= offsets.length || endIndex > offsets.length || beginIndex >= endIndex) {
            throw new IllegalArgumentException();
        }

        final int begin = offsets[beginIndex];
        final int end;

        if (endIndex == offsets.length) {
            end = path.length();
        } else {
            end = offsets[endIndex] - 1;
        }

        return filesystem.provider.newPath(filesystem, path.substring(begin, end));
    }

    @Override
    public boolean startsWith(@NotNull Path other) {
        if (!filesystem.provider.isValidPath(other)) {
            return false;
        }

        final P o = filesystem.provider.getPath(other);

        final int len = path.length();
        final int olen = o.path.length();

        if (o.isAbsolute() != isAbsolute() || olen > len) {
            return false;
        }

        for (int i = 0; i < olen; i++) {
            if (o.path.charAt(i) != path.charAt(i)) {
                return false;
            }
        }

        return o.path.length() == len
            || o.path.charAt(olen - 1) == '/'
            || path.charAt(olen) == '/';
    }

    @Override
    public boolean endsWith(@NotNull Path other) {
        if (!filesystem.provider.isValidPath(other)) {
            return false;
        }

        final P o = filesystem.provider.getPath(other);
        int olast = o.path.length() - 1;
        int last = path.length() - 1;

        if (olast > 0 && o.path.charAt(olast) == '/') {
            olast -= 1;
        }

        if (last > 0 && path.charAt(last) == '/') {
            last -= 1;
        }

        if (olast == -1) { // o.path.length == 0
            return last == -1;
        }

        if ((o.isAbsolute() && (!isAbsolute() || olast != last)) || last < olast) {
            return false;
        }

        for (; olast >= 0; olast--, last--) {
            if (path.charAt(olast) != path.charAt(last)) {
                return false;
            }
        }

        return o.path.charAt(olast + 1) == '/' || last == -1 || path.charAt(last) == '/';
    }

    @NotNull
    @Override
    public Path normalize() {
        final String resolved = getResolved();
        if (resolved.equals(path)) { // no change
            return this;
        }
        return provider.newPath(filesystem, resolved, true);
    }

    @NotNull
    @Override
    public Path resolve(@NotNull Path other) {
        final P o = filesystem.provider.getPath(other);

        if (o.path.isEmpty()) {
            return this;
        } else if (path.isEmpty() || o.isAbsolute()) {
            return o;
        } else if (path.charAt(path.length() - 1) == '/') {
            return filesystem.provider.newPath(filesystem, path + o.path, true);
        } else {
            return filesystem.provider.newPath(filesystem, path + '/' + o.path, true);
        }
    }

    @NotNull
    @Override
    public Path relativize(@NotNull Path other) {
        final P o = filesystem.provider.getPath(other);

        if (o.equals(this)) {
            return filesystem.provider.newPath(filesystem, "", true);
        }

        if (path.isEmpty()) {
            return o;
        }

        if (filesystem != o.filesystem || isAbsolute() != o.isAbsolute()) {
            throw new IllegalArgumentException();
        }

        if (path.length() == 1 && path.charAt(0) == '/') {
            return provider.newPath(filesystem, o.path.substring(1), true);
        }

        int mc = getNameCount();
        int oc = o.getNameCount();
        int n = Math.min(mc, oc);
        int i = 0;

        for (; i < n; i++) {
            if (!equalsNameAt(o, i)) {
                break;
            }
        }

        int dotdots = mc - i;
        int len = dotdots * 3 - 1;

        if (i < oc) {
            len += (o.path.length() - o.offsets[i] + 1);
        }

        final StringBuilder result = new StringBuilder(len);

        while (dotdots > 0) {
            result.append('.');
            result.append('.');

            if (result.length() < len) { // no tailing slash at the end
                result.append('/');
            }

            dotdots -= 1;
        }

        if (i < oc) {
            result.append(o.path, o.offsets[i], o.path.length());
        }

        return provider.newPath(filesystem, result.toString());
    }

    @NotNull
    @Override
    public Path toRealPath(@NotNull LinkOption... options) throws IOException {
        final String resolved = getResolvedPath();

        if (resolved.equals(path)) {
            return this;
        } else {
            final P realPath = provider.newPath(filesystem, resolved, true);
            realPath.resolved = resolved;
            return realPath;
        }
    }

    @NotNull
    @Override
    public AbstractPath<P, FS, FP> toAbsolutePath() {
        if (isAbsolute()) {
            return this;
        } else {
            return filesystem.provider.newPath(filesystem, '/' + path);
        }
    }

    @NotNull
    @Override
    public WatchKey register(@NotNull WatchService watcher, @NotNull WatchEvent.Kind<?>[] events, @NotNull WatchEvent.Modifier... modifiers) {
        throw new ProviderMismatchException();
    }

    @Override
    public int compareTo(@NotNull Path other) {
        final P o = filesystem.provider.getPath(other);
        return path.compareTo(o.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AbstractPath<?, ?, ?> path1 = (AbstractPath<?, ?, ?>) o;
        return filesystem.provider.isValidPath(path1) && compareTo(path1) == 0;
    }

    @NotNull
    @Override
    public String toString() {
        return path;
    }

    @NotNull
    private static String normalize(@NotNull String path) {
        final int len = path.length();

        if (len == 0) {
            // empty path - no op
            return path;
        }

        final StringBuilder sb = new StringBuilder(len);
        char lastCh = 0;

        for (int i = 0; i < len; i++) {
            char ch = path.charAt(i);

            if (ch == '\\') {
                // replace backslash with forward slash
                ch = '/';
            }

            if (ch == '/' && lastCh == '/') {
                // skip second slash
                continue;
            } else if (ch == '\u0000') {
                // NUL is not allowed
                throw new InvalidPathException(path, "Path: nul character not allowed");
            }

            sb.append(ch);
            lastCh = ch;
        }

        if (sb.length() > 1 && lastCh == '/') {
            // remove trailing slash
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    private void initOffsets() {
        if (offsets != null) {
            return;
        }

        final int len = path.length();

        // count names
        int count = 0;
        int index = 0;

        if (len == 0) {
            // empty path has one name
            count = 1;
        } else {
            while (index < len) {
                final char ch = path.charAt(index++);

                if (ch != '/') {
                    count++;

                    while (index < len && path.charAt(index) != '/') {
                        index++;
                    }
                }
            }
        }

        // populate offsets
        final int[] result = new int[count];
        count = 0;
        index = 0;

        while (index < len) {
            final char ch = path.charAt(index);

            if (ch == '/') {
                index++;
            } else {
                result[count++] = index++;

                while (index < len && path.charAt(index) != '/') {
                    index++;
                }
            }
        }

        synchronized (this) {
            if (offsets == null) {
                offsets = result;
            }
        }
    }

    private boolean equalsNameAt(@NotNull AbstractPath<P, FS, FP> other, int index) {
        int mbegin = offsets[index];
        int mlen;

        if (index == (offsets.length - 1)) {
            mlen = path.length() - mbegin;
        } else {
            mlen = offsets[index + 1] - mbegin - 1;
        }

        int obegin = other.offsets[index];
        int olen;

        if (index == (other.offsets.length - 1)) {
            olen = other.path.length() - obegin;
        } else {
            olen = other.offsets[index + 1] - obegin - 1;
        }

        if (mlen != olen) {
            return false;
        }

        for (int n = 0; n < mlen; n++) {
            if (path.charAt(mbegin + n) != other.path.charAt(obegin + n)) {
                return false;
            }
        }

        return true;
    }

    @NotNull
    private String getResolvedPath() {
        String r = resolved;

        if (r == null) {
            if (isAbsolute()) {
                r = getResolved();
            } else {
                r = toAbsolutePath().getResolvedPath();
            }

            resolved = r;
        }

        return resolved;
    }

    @NotNull
    private String getResolved() {
        final int len = path.length();

        for (int i = 0; i < len; i++) {
            if (path.charAt(i) == '.' && (len == i + 1 || path.charAt(i + 1) == '/')) {
                return resolve0();
            }
        }

        return path;
    }

    @NotNull
    private String resolve0() {
        final StringBuilder sb = new StringBuilder(path.length());
        final int nc = getNameCount();
        final int[] lastM = new int[nc];

        int lastMOff = -1;
        int m = 0;

        for (int i = 0; i < nc; i++) {
            int n = offsets[i];
            int len;

            if (i == offsets.length - 1) {
                len = path.length() - n;
            } else {
                len = offsets[i + 1] - n - 1;
            }

            if (len == 1 && path.charAt(n) == (byte) '.') {
                if (m == 0 && path.charAt(0) == '/') { // absolute path
                    sb.append('/');
                    m++;
                }

                continue;
            }

            if (len == 2 && path.charAt(n) == '.' && path.charAt(n + 1) == '.') {
                if (lastMOff >= 0) {
                    m = lastM[lastMOff--];  // retreat
                    continue;
                }

                if (path.charAt(0) == '/') {  // "/../xyz" skip
                    if (m == 0) {
                        sb.append('/');
                        m++;
                    }
                } else {               // "../xyz" -> "../xyz"
                    if (m != 0 && sb.charAt(m - 1) != '/') {
                        sb.append('/');
                        m++;
                    }

                    while (len-- > 0) {
                        sb.append(path.charAt(n++));
                        m++;
                    }
                }
                continue;
            }

            if (m == 0 && path.charAt(0) == '/' || m != 0 && sb.charAt(m - 1) != '/') { // absolute path or not the first name
                sb.append('/');
                m++;
            }

            lastM[++lastMOff] = m;

            while (len-- > 0) {
                sb.append(path.charAt(n++));
                m++;
            }
        }

        if (m > 1 && sb.charAt(m - 1) == '/') {
            m--;
        }

        sb.setLength(m);

        return sb.toString();
    }
}
