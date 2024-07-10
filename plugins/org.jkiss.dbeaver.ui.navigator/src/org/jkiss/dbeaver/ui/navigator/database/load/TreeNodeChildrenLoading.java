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
package org.jkiss.dbeaver.ui.navigator.database.load;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;

import java.util.HashMap;
import java.util.Map;


public class TreeNodeChildrenLoading extends TreeNodeSpecial {

    private static final Map<Object, Object> loadingFiles = new HashMap<>();
    private static final Map<DBNNode, TreeNodeChildrenLoading> placeHolders = new HashMap<>();

    private static final String loadingText = UINavigatorMessages.ui_navigator_loading_text_loading;

    private int viewCount = 0;

    public static synchronized TreeNodeChildrenLoading createLoadingPlaceHolder(DBNNode parent) {
        TreeNodeChildrenLoading node = placeHolders.get(parent);
        if (node == null) {
            node = new TreeNodeChildrenLoading(parent);
            placeHolders.put(parent, node);
        }
        return node;
    }

    protected TreeNodeChildrenLoading(DBNNode parent) {
        super(parent);
    }

    @Override
    public String getText(Object element) {
        viewCount++;
        int dotCount = (viewCount % 10);
        return loadingText + ".".repeat(dotCount);
    }

    @Override
    public Image getImage(Object element) {
        //int imgIndex = (++viewCount % IMG_LOADING.length);
        //return IMG_LOADING[imgIndex];
        return null;
    }

    public void dispose(DBNNode parent) {
        super.dispose();

        loadingFiles.remove(parent);
        placeHolders.remove(parent);
    }

    public static synchronized boolean canBeginLoading(Object parent) {
        if (!loadingFiles.containsKey(parent)) {
            loadingFiles.put(parent, null);
            return true;
        }
        return false;
    }

    public static boolean isLoading() {
        synchronized (loadingFiles) {
            return !loadingFiles.isEmpty();
        }
    }

}