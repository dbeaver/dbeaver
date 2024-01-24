/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.notifications.sounds;

import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.notifications.NotificationSound;
import org.jkiss.dbeaver.ui.notifications.NotificationSoundProvider;

public class BeepSoundProvider implements NotificationSoundProvider {
    public static final BeepSoundProvider INSTANCE = new BeepSoundProvider();

    @NotNull
    @Override
    public NotificationSound create() {
        return BeepSound.INSTANCE;
    }

    private static class BeepSound implements NotificationSound {
        private static final BeepSound INSTANCE = new BeepSound();

        @Override
        public void play(float volume) {
            final Display display = UIUtils.getDisplay();
            if (display != null) {
                UIUtils.syncExec(display::beep);
            }
        }

        @Override
        public void close() {
            // nothing to release
        }
    }
}
