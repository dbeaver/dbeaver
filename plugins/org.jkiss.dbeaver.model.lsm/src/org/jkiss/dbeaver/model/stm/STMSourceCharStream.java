/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.stm;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

final class STMSourceCharStream implements CharStream {
    private final CharStream delegate;
    @Nullable
    private final int[] utf16Offsets;

    STMSourceCharStream(@NotNull CharStream delegate) {
        this.delegate = delegate;
        this.utf16Offsets = createUtf16Offsets(delegate);
    }

    @NotNull
    Interval toUtf16Interval(int start, int stop) {
        if (start < 0 || utf16Offsets == null) {
            return Interval.of(start, stop);
        }
        int utf16Start = toUtf16Offset(start);
        if (stop < start) {
            return Interval.of(utf16Start, utf16Start - 1);
        }
        return Interval.of(utf16Start, toUtf16Offset(stop + 1) - 1);
    }

    @Override
    public String getText(Interval interval) {
        return delegate.getText(interval);
    }

    @Override
    public void consume() {
        delegate.consume();
    }

    @Override
    public int LA(int i) {
        return delegate.LA(i);
    }

    @Override
    public int mark() {
        return delegate.mark();
    }

    @Override
    public void release(int marker) {
        delegate.release(marker);
    }

    @Override
    public int index() {
        return delegate.index();
    }

    @Override
    public void seek(int index) {
        delegate.seek(index);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public String getSourceName() {
        return delegate.getSourceName();
    }

    private int toUtf16Offset(int codePointOffset) {
        return utf16Offsets[Math.min(codePointOffset, utf16Offsets.length - 1)];
    }

    @Nullable
    private static int[] createUtf16Offsets(@NotNull CharStream stream) {
        if (stream.size() == 0) {
            return null;
        }
        String text = stream.getText(Interval.of(0, stream.size() - 1));
        if (text.length() == stream.size()) {
            return null;
        }
        int[] offsets = new int[stream.size() + 1];
        int codePointOffset = 0;
        int utf16Offset = 0;
        while (utf16Offset < text.length()) {
            offsets[codePointOffset++] = utf16Offset;
            utf16Offset += Character.charCount(text.codePointAt(utf16Offset));
        }
        offsets[codePointOffset] = utf16Offset;
        return offsets;
    }
}
