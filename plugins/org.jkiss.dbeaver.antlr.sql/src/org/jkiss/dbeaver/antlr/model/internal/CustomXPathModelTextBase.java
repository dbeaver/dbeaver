package org.jkiss.dbeaver.antlr.model.internal;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.w3c.dom.DOMException;
import org.w3c.dom.Text;

public interface CustomXPathModelTextBase extends TerminalNode, Text, CustomXPathModelNodeBase {

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
