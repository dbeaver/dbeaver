package org.jkiss.dbeaver.model.lsm.interfaces;

import java.io.IOException;
import java.io.Reader;

import org.antlr.v4.runtime.CharStream;
import org.jkiss.dbeaver.model.lsm.impl.LSMSourceImpl;

public interface LSMSource extends LSMObject<LSMSource> {

    CharStream getStream();

    public static LSMSource fromReader(Reader reader) throws IOException {
        return new LSMSourceImpl(reader);
    }
}
