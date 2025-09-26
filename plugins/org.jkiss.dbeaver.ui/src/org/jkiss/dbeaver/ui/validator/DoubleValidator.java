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
import org.eclipse.swt.widgets.Text;

/**
 * VerifyListener for double (floating-point) values with optional range checks.
 * <p>
 * Example:
 * <pre>{@code
 * text.addVerifyListener(new DoubleValidator(0.0, 100.0, false));
 * }</pre>
 * This will allow numbers from 0.0 up to 100.0 (inclusive),
 * disallowing negative input.
 */
public class DoubleValidator implements VerifyListener {
    private final double minValue;
    private final double maxValue;
    private final boolean allowNegative;

    public DoubleValidator(double minValue, double maxValue, boolean allowNegative) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.allowNegative = allowNegative;
    }

    @Override
    public void verifyText(VerifyEvent e) {
        if (e.getSource() instanceof Text text) {
            String current = text.getText();

            String prospective =
                current.substring(0, e.start)
                    + e.text
                    + current.substring(e.end);

            verifyText(e, prospective);
        } else {
            verifyText(e, e.text);
        }
    }

    private void verifyText(VerifyEvent e, String text) {
        if (text.isEmpty() || ".".equals(text)) {
            return;
        }
        if (!allowNegative && text.startsWith("-")) {
            e.doit = false;
            return;
        }
        try {
            double value = Double.parseDouble(text);
            if (value < minValue || value > maxValue) {
                e.doit = false;
            }
        } catch (NumberFormatException ex) {
            e.doit = false;
        }
    }
}
