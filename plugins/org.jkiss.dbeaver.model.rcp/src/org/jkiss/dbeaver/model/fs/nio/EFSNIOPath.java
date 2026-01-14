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
package org.jkiss.dbeaver.model.fs.nio;

import org.eclipse.core.runtime.IPath;

import java.io.File;
import java.nio.file.Path;

/**
 * NIOResource
 */
public class EFSNIOPath implements IPath {

    private final Path nioPath;

    public EFSNIOPath(Path nioPath) {
        this.nioPath = nioPath;
    }

    @Override
    public IPath addFileExtension(String extension) {
        return this;
    }

    @Override
    public IPath addTrailingSeparator() {
        return this;
    }

    @Override
    public IPath append(String path) {
        return this;
    }

    @Override
    public IPath append(IPath path) {
        return this;
    }

    @Override
    public Object clone() {
        return new EFSNIOPath(nioPath);
    }

    @Override
    public String getDevice() {
        return null;
    }

    @Override
    public String getFileExtension() {
        return null;
    }

    @Override
    public boolean hasTrailingSeparator() {
        return false;
    }

    @Override
    public boolean isAbsolute() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isPrefixOf(IPath anotherPath) {
        return false;
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public boolean isUNC() {
        return false;
    }

    @Override
    public boolean isValidPath(String path) {
        return false;
    }

    @Override
    public boolean isValidSegment(String segment) {
        return false;
    }

    @Override
    public String lastSegment() {
        return null;
    }

    @Override
    public IPath makeAbsolute() {
        return this;
    }

    @Override
    public IPath makeRelative() {
        return this;
    }

    @Override
    public IPath makeRelativeTo(IPath base) {
        return this;
    }

    @Override
    public IPath makeUNC(boolean toUNC) {
        return this;
    }

    @Override
    public int matchingFirstSegments(IPath anotherPath) {
        return 0;
    }

    @Override
    public IPath removeFileExtension() {
        return this;
    }

    @Override
    public IPath removeFirstSegments(int count) {
        return this;
    }

    @Override
    public IPath removeLastSegments(int count) {
        return this;
    }

    @Override
    public IPath removeTrailingSeparator() {
        return this;
    }

    @Override
    public String segment(int index) {
        return null;
    }

    @Override
    public int segmentCount() {
        return 0;
    }

    @Override
    public String[] segments() {
        return new String[0];
    }

    @Override
    public IPath setDevice(String device) {
        return null;
    }

    @Override
    public File toFile() {
        return null;
    }

    @Override
    public String toOSString() {
        return null;
    }

    @Override
    public String toPortableString() {
        return null;
    }

    @Override
    public IPath uptoSegment(int count) {
        return null;
    }

    @Override
    public Path toPath() {
        return nioPath;
    }
}
