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
package org.jkiss.dbeaver.model.lsm.mapping.internal;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.w3c.dom.DOMException;
import org.w3c.dom.Text;

public interface XTreeTextBase extends TerminalNode, Text, XTreeNodeBase {

    @Override
    default String getData() throws DOMException {
        return this.getSymbol().getText();
    }

    @Override
    default void setData(String data) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default int getLength() {
        return this.getSymbol().getText().length();
    }

    @Override
    default String substringData(int offset, int count) throws DOMException {
        return this.getSymbol().getText().substring(offset, count);
    }

    @Override
    default void appendData(String arg) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default void insertData(int offset, String arg) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default void deleteData(int offset, int count) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default void replaceData(int offset, int count, String arg) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default Text splitText(int offset) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean isElementContentWhitespace() {
        return this.getSymbol().getText().replaceAll("[ \r\n\t]", "").length() == 0;
    }

    @Override
    default String getWholeText() {
        throw new UnsupportedOperationException();
    }

    @Override
    default Text replaceWholeText(String content) throws DOMException {
        throw new UnsupportedOperationException();
    }
}
