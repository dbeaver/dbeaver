/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

/**
 * Completion request
 */
public class DAICompletionResponse {

    private String resultPrompt;
    private String resultCompletion;
    private String resultMessage;

    public String getResultPrompt() {
        return resultPrompt;
    }

    public void setResultPrompt(String resultPrompt) {
        this.resultPrompt = resultPrompt;
    }

    public String getResultCompletion() {
        return resultCompletion;
    }

    public void setResultCompletion(String resultCompletion) {
        this.resultCompletion = resultCompletion;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }
}
