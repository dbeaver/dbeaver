/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.lsp;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.text.parser.TPRuleBasedScanner;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenAbstract;
import org.jkiss.dbeaver.model.text.parser.TPWordDetector;
import org.jkiss.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class LspSQLIndentifierDetector extends TPWordDetector {

    protected SQLDialect dialect;

    public LspSQLIndentifierDetector(SQLDialect dialect) {
        this.dialect = dialect;
    }

    @NotNull
    public List<Pair<TPToken, Region>> extractAllTokens(@NotNull IDocument document, @NotNull SQLRuleManager ruleManager) {
        final TPRuleBasedScanner scanner = new TPRuleBasedScanner();
        scanner.setRules(ruleManager.getAllRules());
        scanner.setRange(document, 0, document.getLength());

        List<Pair<TPToken, Region>> tokens = new ArrayList<>();

        TPToken token = scanner.nextToken();
        while (!token.isEOF()) {
            if (token instanceof TPTokenAbstract) {
                tokens.add(new Pair<>(token, new Region(scanner.getTokenOffset(), scanner.getTokenLength())));
            }
            token = scanner.nextToken();
        }

        return tokens;
    }
}
