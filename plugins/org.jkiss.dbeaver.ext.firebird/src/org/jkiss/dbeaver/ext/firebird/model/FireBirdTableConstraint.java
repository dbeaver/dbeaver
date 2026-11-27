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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.model.meta.IPropertyValueValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableCheckConstraint;

/**
 * Firebird table constraint: primary key, unique key or CHECK constraint.
 * All types share this class, because a constraint created in the UI starts as primary key
 * and only becomes a CHECK constraint when the user changes its type.
 */
public class FireBirdTableConstraint extends GenericUniqueKey implements DBSTableCheckConstraint {

    private static final String CHECK_KEYWORD = "CHECK";

    @Nullable
    private String checkConstraintDefinition;

    public FireBirdTableConstraint(
        @NotNull GenericTableBase table,
        @NotNull String name,
        @NotNull DBSEntityConstraintType constraintType,
        @Nullable String checkConstraintDefinition,
        boolean persisted
    ) {
        super(table, name, null, constraintType, persisted);
        this.checkConstraintDefinition = checkConstraintDefinition;
    }

    /**
     * Condition of a CHECK constraint, without the CHECK keyword and the enclosing parentheses.
     */
    @Nullable
    @Override
    @Property(viewable = true, editable = true, length = PropertyLength.MULTILINE, order = 4,
        visibleIf = CheckConstraintValidator.class)
    public String getCheckConstraintDefinition() {
        return checkConstraintDefinition;
    }

    @Override
    public void setCheckConstraintDefinition(@Nullable String expression) {
        this.checkConstraintDefinition = expression;
    }

    /**
     * Extracts the condition from the source Firebird stores for a CHECK constraint, e.g. {@code CHECK (PRICE >= 0)}.
     * The source keeps the case of the CHECK keyword as written by the user.
     */
    @Nullable
    public static String extractCheckCondition(@Nullable String source) {
        if (source == null) {
            return null;
        }
        String condition = source.trim();
        if (condition.regionMatches(true, 0, CHECK_KEYWORD, 0, CHECK_KEYWORD.length())) {
            condition = condition.substring(CHECK_KEYWORD.length()).trim();
        }
        if (condition.startsWith("(") && findClosingParenthesis(condition) == condition.length() - 1) {
            condition = condition.substring(1, condition.length() - 1).trim();
        }
        return condition;
    }

    /**
     * Returns the position of the parenthesis closing the one at position 0, skipping string literals and quoted names,
     * or -1 if it is not closed.
     */
    private static int findClosingParenthesis(@NotNull String text) {
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                // A doubled quote inside a literal is an escaped quote and toggles twice
                if (c == quote) {
                    quote = 0;
                }
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Shows the CHECK condition for CHECK constraints only.
     */
    public static class CheckConstraintValidator implements IPropertyValueValidator<FireBirdTableConstraint, Object> {

        @Override
        public boolean isValidValue(@NotNull FireBirdTableConstraint constraint, @Nullable Object value) {
            return constraint.getConstraintType() == DBSEntityConstraintType.CHECK;
        }
    }
}
