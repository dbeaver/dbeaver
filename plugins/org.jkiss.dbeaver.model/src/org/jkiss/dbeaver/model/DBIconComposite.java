/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.model;

/**
 * Image with overlays
 */
public class DBIconComposite implements DBPImage
{
    private final DBPImage main;
    private final boolean disabled;
    private DBPImage topLeft, topRight, bottomLeft, bottomRight;

    public DBIconComposite(DBPImage main, boolean disabled, DBPImage topLeft, DBPImage topRight, DBPImage bottomLeft, DBPImage bottomRight) {
        this.main = main;
        this.disabled = disabled;
        this.topLeft = topLeft;
        this.topRight = topRight;
        this.bottomLeft = bottomLeft;
        this.bottomRight = bottomRight;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean hasOverlays() {
        return topLeft != null || topRight != null || bottomLeft != null || bottomRight != null;
    }

    public DBPImage getTopLeft() {
        return topLeft;
    }

    public void setTopLeft(DBPImage topLeft) {
        this.topLeft = topLeft;
    }

    public DBPImage getTopRight() {
        return topRight;
    }

    public void setTopRight(DBPImage topRight) {
        this.topRight = topRight;
    }

    public DBPImage getBottomLeft() {
        return bottomLeft;
    }

    public void setBottomLeft(DBPImage bottomLeft) {
        this.bottomLeft = bottomLeft;
    }

    public DBPImage getBottomRight() {
        return bottomRight;
    }

    public void setBottomRight(DBPImage bottomRight) {
        this.bottomRight = bottomRight;
    }

    @Override
    public String getLocation() {
        return main.getLocation();
    }

}
