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
package org.jkiss.dbeaver.ui.validator;

import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;

public class IntegerValidator implements VerifyListener {
    private final int minValue;
    private final int maxValue;

    public IntegerValidator(int minValue, int maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public void verifyText(VerifyEvent e) {
        String newText = e.text;
        if (newText.isEmpty()) {
            return; // Allow empty input
        }

        try {
            int value = Integer.parseInt(newText);
            if (value < minValue || value > maxValue) {
                e.doit = false; // Reject input outside of range
            }
        } catch (NumberFormatException ex) {
            e.doit = false; // Reject non-integer input
        }
    }
}
