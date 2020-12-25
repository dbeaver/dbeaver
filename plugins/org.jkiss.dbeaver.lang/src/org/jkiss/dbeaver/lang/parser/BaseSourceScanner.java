/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.lang.parser;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.lang.SCMSourceScanner;
import org.jkiss.dbeaver.lang.SCMSourceText;
import org.jkiss.utils.ArrayUtils;

import java.util.Collection;

/**
 * BaseSourceScanner.
 */
public class BaseSourceScanner extends RuleBasedScanner implements SCMSourceScanner, SCMSourceText {

    public BaseSourceScanner(Document document, Collection<IRule> rules) {

        setRules(ArrayUtils.toArray(IRule.class, rules));

        setRange(document, 0, document.getLength());
    }

    @NotNull
    @Override
    public SCMSourceText getSource() {
        return this;
    }

    @Override
    public int getLength() {
        return fDocument.getLength();
    }

    @Override
    public char getChar(int offset) throws IndexOutOfBoundsException {
        try {
            return fDocument.getChar(offset);
        } catch (BadLocationException e) {
            throw new IndexOutOfBoundsException(e.getMessage());
        }
    }

    @NotNull
    @Override
    public String getSegment(int beginOffset, int endOffset) {
        try {
            return fDocument.get(beginOffset, endOffset - beginOffset);
        } catch (BadLocationException e) {
            throw new IndexOutOfBoundsException(e.getMessage());
        }
    }
}
