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
package org.jkiss.dbeaver.model.nio;

import org.jkiss.code.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public abstract class NIOPath implements Path {
    @Override
    public int compareTo(@NotNull Path other) {
        return toString().compareTo(other.toString());
    }

    @NotNull
    @Override
    public String toString() {
        return toUri().toString();
    }

    @Override
    public boolean endsWith(@NotNull Path other) {
        return toString().equals(other.toString());
    }

    @Override
    public Path getName(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register(@NotNull WatchService watcher, @NotNull WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws
        IOException {
        throw new UnsupportedOperationException();
    }

}
