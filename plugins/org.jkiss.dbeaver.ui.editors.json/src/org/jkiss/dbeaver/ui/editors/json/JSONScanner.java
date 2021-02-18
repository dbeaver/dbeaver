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
package org.jkiss.dbeaver.ui.editors.json;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.graphics.Color;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * JSONScanner
 */
public class JSONScanner extends RuleBasedScanner {

    public JSONScanner() {
        super();
        initScanner();
    }

    public void reinitScanner() {
        initScanner();
    }

    private void initScanner() {
        ColorRegistry colorRegistry = UIUtils.getColorRegistry();
        Color colorKey = colorRegistry.get(SQLConstants.CONFIG_COLOR_KEYWORD);
        Color colorString = colorRegistry.get(SQLConstants.CONFIG_COLOR_STRING);
        Color colorValue = colorRegistry.get(SQLConstants.CONFIG_COLOR_NUMBER);

        IToken string = new Token(new TextAttribute(colorString));
        IToken value = new Token(new TextAttribute(colorValue));
        IToken defaultText = new Token(new TextAttribute(colorKey));
        //IToken nullValue = new Token(new TextAttribute(colorKey));

        List<IRule> rules = new LinkedList<>();

        rules.add(new NumberRule(value));
//        rules.add(new MultiLineRule(":\"", "\"", value, '\\'));  //$NON-NLS-1$//$NON-NLS-2$
//        rules.add(new MultiLineRule(": \"", "\"", value, '\\'));  //$NON-NLS-1$//$NON-NLS-2$
        rules.add(new MultiLineRule("\"", "\"", string, '\\')); //$NON-NLS-2$ //$NON-NLS-1$
        WordRule wordRule = new WordRule(new WordDetector(), defaultText);
        wordRule.addWord("null", value);
        wordRule.addWord("true", value);
        wordRule.addWord("false", value);
        rules.add(wordRule);
        rules.add(new WhitespaceRule(new WhitespaceDetector()));

        setRules(rules.toArray(new IRule[0]));
    }

    public static class WhitespaceDetector implements IWhitespaceDetector {

        @Override
        public boolean isWhitespace(char character) {
            return Character.isWhitespace(character);
        }

    }

    public static class WordDetector implements IWordDetector {

        @Override
        public boolean isWordPart(char character) {
            return Character.isJavaIdentifierPart(character);
        }

        @Override
        public boolean isWordStart(char character) {
            return Character.isJavaIdentifierPart(character);
        }

    }
}
