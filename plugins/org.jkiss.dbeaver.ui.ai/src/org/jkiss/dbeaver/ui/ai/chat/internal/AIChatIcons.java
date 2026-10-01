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
package org.jkiss.dbeaver.ui.ai.chat.internal;

import org.jkiss.dbeaver.model.DBIcon;

import java.util.List;

public class AIChatIcons {
    public static final DBIcon ROCKET = new DBIcon("ai-rocket", "rocket.svg"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon USER = new DBIcon("ai-user", "user.svg"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ASSISTANT = new DBIcon("ai-assistant", "robot.svg"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon SEND = new DBIcon("ai-chat-send", "ai-chat-send.svg"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon ATTACH = new DBIcon("ai-attach", "attach.svg"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon MIC_ON = new DBIcon("ai-mic-on", "mic_on.svg"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon MIC_OFF = new DBIcon("ai-mic-off", "mic_off.svg"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon RECORDING0 = new DBIcon("RECORDING0", "mic-animation/recording00.svg"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon RECORDING1 = new DBIcon("RECORDING0", "mic-animation/recording01.svg"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon RECORDING2 = new DBIcon("RECORDING0", "mic-animation/recording02.svg"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon RECORDING3 = new DBIcon("RECORDING0", "mic-animation/recording03.svg"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon RECORDING4 = new DBIcon("RECORDING0", "mic-animation/recording04.svg"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon RECORDING5 = new DBIcon("RECORDING0", "mic-animation/recording05.svg"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon RECORDING6 = new DBIcon("RECORDING0", "mic-animation/recording06.svg"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon RECORDING7 = new DBIcon("RECORDING0", "mic-animation/recording07.svg"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final List<DBIcon> RECORDING = List.of(RECORDING0, RECORDING1, RECORDING2, RECORDING3, RECORDING4, RECORDING5, RECORDING6, RECORDING7);

    static {
        DBIcon.loadIcons(AIChatIcons.class);
    }
}
