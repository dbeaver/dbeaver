/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;

import java.util.HashMap;
import java.util.Map;


public class TreeNodeChildrenLoading extends TreeNodeSpecial {

    private static final Map<Object, Object> loadingFiles = new HashMap<>();
    private static final Map<Object, Object> placeHolders = new HashMap<>();

    public static final Object LOADING_FAMILY = new Object();

    private static Image[] IMG_LOADING = new Image[] {
        DBeaverIcons.getImage(UIIcon.LOADING1),
        DBeaverIcons.getImage(UIIcon.LOADING2),
        DBeaverIcons.getImage(UIIcon.LOADING3),
        DBeaverIcons.getImage(UIIcon.LOADING4),
        DBeaverIcons.getImage(UIIcon.LOADING5),
        DBeaverIcons.getImage(UIIcon.LOADING6),
        DBeaverIcons.getImage(UIIcon.LOADING7)
    };

    private static String loadingText = UINavigatorMessages.ui_navigator_loading_text_loading;
    private static String text1 = loadingText + ".."; //$NON-NLS-1$;
    private static String text2 = loadingText + ".."; //$NON-NLS-1$;
    private static String text3 = loadingText + "..."; //$NON-NLS-1$;

    private int viewCount = 0;

    public static synchronized TreeNodeChildrenLoading createLoadingPlaceHolder(DBNNode parent) {
        TreeNodeChildrenLoading node = null;
        if (!placeHolders.containsKey(parent)) {
            placeHolders.put(parent, node = new TreeNodeChildrenLoading(parent));
        }
        return node;
    }

    protected TreeNodeChildrenLoading(DBNNode parent) {
        super(parent);
    }

    @Override
    public String getText(Object element) {
        return text3;
/*
        switch (viewCount % 4) {
            case 0:
                return loadingText;
            case 1:
                return text1;
            case 2:
                return text2;
            case 3:
            default:
                return text3;
        }
*/
    }

    @Override
    public Image getImage(Object element) {
        int imgIndex = (++viewCount % IMG_LOADING.length);
        return IMG_LOADING[imgIndex];
    }

    public void dispose(Object parent) {
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