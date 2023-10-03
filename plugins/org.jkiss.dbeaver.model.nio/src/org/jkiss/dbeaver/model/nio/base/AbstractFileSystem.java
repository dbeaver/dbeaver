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
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;

public abstract class AbstractFileSystem<
    P extends AbstractPath<P, FS, FP>,
    FS extends AbstractFileSystem<P, FS, FP>,
    FP extends AbstractFileSystemProvider<P, FS, FP>> extends FileSystem {

    protected final AbstractFileSystemProvider<P, FS, FP> provider;

    public AbstractFileSystem(@NotNull AbstractFileSystemProvider<P, FS, FP> provider) {
        this.provider = provider;
    }

    @Override
    public AbstractFileSystemProvider<P, FS, FP> provider() {
        return provider;
    }

    @Override
    public String getSeparator() {
        return "/";
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isOpen() {
        return provider.isOpen(this);
    }

    @Override
    public void close() throws IOException {
        provider.removeFileSystem(this);
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException();
    }

    @NotNull
    public abstract Path getRoot();
}
