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

package org.jkiss.dbeaver.model.ai.completion;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AISettingsEventListener;
import org.jkiss.dbeaver.model.ai.TooManyRequestsException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.concurrent.Flow;

/**
 * Completion engine
 */
public interface DAICompletionEngine extends AISettingsEventListener {

    /**
     * Returns the context size for the completion engine.
     *
     * @param monitor progress monitor
     * @return the context window size
     */
    int getMaxContextSize(@NotNull DBRProgressMonitor monitor);

    /**
     * Requests completions from the completion engine.
     *
     * @param monitor the progress monitor
     * @param request the completion request
     * @return the completion response
     * @throws TooManyRequestsException if the request limit is exceeded and the request can be retried
     * @throws DBException if an error occurs
     */
    DAICompletionResponse requestCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionRequest request
    ) throws DBException;

    /**
     * Requests a stream of completion chunks from the completion engine.
     *
     * @param monitor the progress monitor
     * @param request the completion request
     * @return the stream of completion chunks
     * @throws TooManyRequestsException if the request limit is exceeded and the request can be retried
     * @throws DBException if an error occurs
     */
    default Flow.Publisher<DAICompletionChunk> requestCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionRequest request
    ) throws DBException {
        DAICompletionResponse completionResponse = requestCompletion(monitor, request);
        return subscriber -> {
            subscriber.onSubscribe(new Flow.Subscription() {
                private boolean isCompleted = false;

                @Override
                public void request(long n) {
                    if (n <= 0) {
                        subscriber.onError(new IllegalArgumentException("Invalid request size: " + n));
                    } else if (isCompleted) {
                        subscriber.onError(new IllegalStateException("Completion stream is already completed"));
                    } else {
                        subscriber.onNext(new DAICompletionChunk(completionResponse.text()));
                        subscriber.onComplete();
                        isCompleted = true;
                    }
                }

                @Override
                public void cancel() {
                    // No action needed
                }
            });
        };
    }

    /**
     * Checks if the completion engine has a valid configuration.
     *
     * @return true if the completion engine has a valid configuration
     */
    boolean hasValidConfiguration();

    boolean isLoggingEnabled();
}
