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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionChunk;

import java.util.concurrent.Flow;

class LogSubscriber implements Flow.Subscriber<DAICompletionChunk> {
    private final Log log;
    private final Flow.Subscriber<? super DAICompletionChunk> subscriber;

    public LogSubscriber(Log log, Flow.Subscriber<? super DAICompletionChunk> subscriber) {
        this.log = log;
        this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(DAICompletionChunk item) {
        log.debug("Received completion chunk: " + item);

        subscriber.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        log.error("Error in completion stream", throwable);

        subscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        log.debug("Completion stream completed");

        subscriber.onComplete();
    }
}
