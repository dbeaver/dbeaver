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

package org.jkiss.dbeaver.model.fs;

import org.jkiss.utils.CommonUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract Virtual file system
 */
public abstract class AbstractVirtualFileSystem implements DBFVirtualFileSystem {
    @Override
    public String[] getURISegments(URI uri) {
        List<String> segments = new ArrayList<>();
        if (useUriHostNameAsSegment(uri)) {
            String host = uri.getHost();
            if (!CommonUtils.isEmpty(host)) {
                segments.add(host);
            }
            String authority = uri.getAuthority();
            if (!CommonUtils.isEmpty(authority) && !authority.equals(host)) {
                segments.add(authority);
            }
        }
        String path = uri.getPath();
        if (!CommonUtils.isEmpty(path)) {
            for (String item : path.split("/")) {
                if (!item.isEmpty()) {
                    segments.add(item);
                }
            }
        }
        return segments.toArray(new String[0]);
    }

    protected boolean useUriHostNameAsSegment(URI uri) {
        return true;
    }

}
