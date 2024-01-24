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
package org.jkiss.dbeaver.ui.notifications;

public interface NotificationSound extends AutoCloseable {
    /**
     * Plays the notification sound with the given volume and blocks the thread until finished.
     *
     * @param volume notification volume in a range of {@code 0.0} to {@code 1.0}, inclusive.
     */
    void play(float volume);

    /**
     * Releases any system resources allocated by this notification sound.
     */
    @Override
    void close();
}
