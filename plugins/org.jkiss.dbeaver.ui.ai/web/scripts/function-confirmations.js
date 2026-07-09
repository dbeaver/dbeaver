function addFunctionConfirmation(args) {
    hideCenterText();

    const cardId = 'confirm-' + args.messageId + '-' + args.functionIndex;
    const div = document.createElement('div');
    div.id = cardId;
    div.className = 'message confirmation';
    div.dataset.messageId = String(args.messageId);
    div.dataset.approvedText = args.approvedText;

    div._fnData = {
        description: args.description,
        arguments: args.arguments,
        functionName: args.functionName,
        paramsLabel: args.paramsLabel,
        resultLabel: args.resultLabel
    };

    const card = document.createElement('div');
    card.className = 'confirmation-card';

    if (args.arguments && Object.keys(args.arguments).length > 0) {
        const paramsBlock = document.createElement('details');
        paramsBlock.className = 'confirmation-params confirmation-params-inline';

        const paramsSummary = document.createElement('summary');
        paramsSummary.className = 'confirmation-header confirmation-params-summary confirmation-inline-summary';
        appendConfirmationTitle(paramsSummary, args);
        const expander = document.createElement('span');
        expander.className = 'confirmation-expander';
        expander.setAttribute('aria-hidden', 'true');
        expander.textContent = '\u203A';
        paramsSummary.appendChild(expander);
        paramsBlock.appendChild(paramsSummary);

        const paramsDiv = document.createElement('div');
        paramsDiv.className = 'confirmation-params-content fn-detail-section';
        const paramsValue = document.createElement('pre');
        paramsValue.className = 'fn-detail-value';
        paramsValue.textContent = formatArguments(args.arguments);
        paramsDiv.appendChild(paramsValue);
        paramsBlock.appendChild(paramsDiv);
        card.appendChild(paramsBlock);
    } else {
        const header = document.createElement('div');
        header.className = 'confirmation-header';
        appendConfirmationTitle(header, args);
        card.appendChild(header);
    }

    if (args.description) {
        const desc = document.createElement('div');
        desc.className = 'confirmation-description';
        desc.textContent = args.description;
        card.appendChild(desc);
    }

    const actions = document.createElement('div');
    actions.className = 'confirmation-actions';

    const allowGroup = document.createElement('div');
    allowGroup.className = 'confirmation-allow-group';

    const allowBtn = document.createElement('button');
    allowBtn.className = 'confirmation-btn confirmation-btn-allow confirmation-btn-allow-main';
    allowBtn.textContent = args.allowText;
    allowBtn.onclick = () => {
        replaceCardWithCollapsibleStatus(div, args.approvedText, 'confirmation-status-approved', div._fnData);
        window.confirmFunctionCalls(args.messageId, args.functionIndex);
    };

    const allowMenuBtn = document.createElement('button');
    allowMenuBtn.className = 'confirmation-btn confirmation-btn-allow confirmation-btn-allow-menu';
    allowMenuBtn.type = 'button';
    allowMenuBtn.setAttribute('aria-label', 'Allow options');
    allowMenuBtn.textContent = '\u25BE';
    allowMenuBtn.onclick = (event) => {
        event.preventDefault();
        event.stopPropagation();

        if (typeof window.showAIFunctionAllowMenu === 'function') {
            const rect = allowMenuBtn.getBoundingClientRect();
            window.showAIFunctionAllowMenu(
                args.functionName,
                Math.round(rect.left),
                Math.round(rect.bottom),
                args.messageId,
                args.functionIndex
            );
        }
    };

    const declineBtn = document.createElement('button');
    declineBtn.className = 'confirmation-btn confirmation-btn-decline';
    declineBtn.textContent = args.declineText;
    declineBtn.onclick = () => {
        replaceCardWithCollapsibleStatus(div, args.declinedText, 'confirmation-status-declined', {
            ...div._fnData,
            awaitResult: false,
            showResult: false
        });
        window.denyFunctionCalls(args.messageId, args.functionIndex);
    };

    allowGroup.appendChild(allowBtn);
    allowGroup.appendChild(allowMenuBtn);
    actions.appendChild(allowGroup);
    actions.appendChild(declineBtn);
    card.appendChild(actions);

    div.appendChild(card);
    appendChatNode(div);
}

function approveFunctionConfirmation(messageId, functionIndex) {
    const card = document.getElementById('confirm-' + messageId + '-' + functionIndex);
    if (!card) {
        return;
    }

    replaceCardWithCollapsibleStatus(
        card,
        card.dataset.approvedText || '',
        'confirmation-status-approved',
        card._fnData
    );
    window.confirmFunctionCalls(messageId, functionIndex);
}

function appendConfirmationTitle(container, args) {
    const title = document.createElement('span');
    title.className = 'confirmation-title';

    title.appendChild(document.createTextNode((args.titlePrefix || 'Allow') + ' '));

    const agent = document.createElement('span');
    agent.className = 'confirmation-title-emphasis';
    agent.textContent = args.agentName || '';
    title.appendChild(agent);

    title.appendChild(document.createTextNode(' ' + (args.titleMiddle || 'to run tool') + ' '));

    const tool = document.createElement('span');
    tool.className = 'confirmation-title-emphasis';
    tool.textContent = args.toolName || '';
    title.appendChild(tool);

    container.appendChild(title);
}

function formatArguments(args) {
    if (!args || typeof args !== 'object') return '';
    return Object.entries(args)
        .map(([key, value]) => key + ': ' + (typeof value === 'object' ? JSON.stringify(value) : String(value)))
        .join('\n');
}

function createCollapsibleStatus(text, statusClass, data) {
    const details = document.createElement('details');
    details.className = 'fn-collapsible';
    if (data && data.functionName) {
        details.setAttribute('data-function-name', data.functionName);
        if (data.awaitResult !== false) {
            details.classList.add('fn-awaiting-result');
        }
    }

    const summary = document.createElement('summary');
    summary.className = 'fn-summary ' + statusClass;

    const icon = createConfirmationStatusIcon(statusClass, text);
    summary.appendChild(icon);

    const label = document.createElement('span');
    label.className = 'fn-summary-label';
    label.textContent = text;
    summary.appendChild(label);

    details.appendChild(summary);

    const content = document.createElement('div');
    content.className = 'fn-details-content';

    if (data && data.description) {
        const descDiv = document.createElement('div');
        descDiv.className = 'fn-detail-description';
        descDiv.textContent = data.description;
        content.appendChild(descDiv);
    }

    const sections = document.createElement('div');
    sections.className = 'fn-detail-box';

    if (data && data.arguments && Object.keys(data.arguments).length > 0) {
        const paramsDiv = document.createElement('div');
        paramsDiv.className = 'fn-detail-section';
        const paramsLabel = document.createElement('div');
        paramsLabel.className = 'fn-detail-label';
        paramsLabel.textContent = data.paramsLabel || 'Parameters';
        paramsDiv.appendChild(paramsLabel);
        const paramsValue = document.createElement('pre');
        paramsValue.className = 'fn-detail-value';
        paramsValue.textContent = formatArguments(data.arguments);
        paramsDiv.appendChild(paramsValue);
        sections.appendChild(paramsDiv);
    }

    if (!data || data.showResult !== false) {
        const resultDiv = document.createElement('div');
        resultDiv.className = 'fn-detail-section fn-detail-result';
        resultDiv.style.display = 'none';
        sections.appendChild(resultDiv);
    }

    if (sections.childElementCount > 0) {
        content.appendChild(sections);
    }

    details.appendChild(content);
    return details;
}

function addAutoConfirmedStatus(args) {
    hideCenterText();

    const div = document.createElement('div');
    div.className = 'message confirmation-result';
    if (args.messageId !== undefined && args.messageId !== null) {
        div.dataset.messageId = String(args.messageId);
    }

    const statusClass = args.hasException ? 'confirmation-status-error' : Boolean(args.confirmed) ? 'confirmation-status-approved' : 'confirmation-status-declined';
    const collapsible = createCollapsibleStatus(args.text, statusClass, args);
    div.appendChild(collapsible);

    appendChatNode(div);
}

function replaceCardWithCollapsibleStatus(container, text, statusClass, data) {
    container.className = 'message confirmation-result';
    container.innerHTML = '';
    container._fnData = null;

    const collapsible = createCollapsibleStatus(text, statusClass, data);
    container.appendChild(collapsible);
}

function replaceCardWithStatus(container, text, statusClass) {
    container.className = 'message confirmation-result';
    container.innerHTML = '';
    container._fnData = null;

    const status = document.createElement('div');
    status.className = 'confirmation-status ' + statusClass;

    const icon = createConfirmationStatusIcon(statusClass, text);
    status.appendChild(icon);

    const label = document.createElement('span');
    label.className = 'confirmation-status-label';
    label.textContent = text;
    status.appendChild(label);

    container.appendChild(status);
}

function createConfirmationStatusIcon(statusClass, text) {
    const icon = document.createElement('span');
    const isApproved = statusClass.includes('approved');
    const isError = statusClass.includes('error');
    icon.className = `confirmation-status-icon ${
        isApproved
            ? 'confirmation-status-icon-approved'
            : isError
                ? 'confirmation-status-icon-error'
                : 'confirmation-status-icon-declined'
    }`;
    icon.textContent = isApproved ? '\u2713' : isError ? '\u26A0' : '\u00D7';
    icon.setAttribute('aria-label', text);
    return icon;
}

function updateCollapsibleStatus(collapsible, text, statusClass) {
    const summary = collapsible.querySelector('.fn-summary');
    if (!summary) {
        return;
    }

    summary.className = 'fn-summary ' + statusClass;

    const currentIcon = summary.querySelector('.confirmation-status-icon');
    const nextIcon = createConfirmationStatusIcon(statusClass, text);
    if (currentIcon) {
        currentIcon.replaceWith(nextIcon);
    } else {
        summary.prepend(nextIcon);
    }

    const label = summary.querySelector('.fn-summary-label');
    if (label && text) {
        label.textContent = text;
    }
}

function findFunctionStatusTarget(args) {
    const candidates = Array.from(document.querySelectorAll('.fn-collapsible')).filter(el =>
        el.getAttribute('data-function-name') === args.functionName
    );

    const awaitingCandidates = candidates.filter(el => el.classList.contains('fn-awaiting-result'));
    if (awaitingCandidates.length > 0) {
        return awaitingCandidates[awaitingCandidates.length - 1];
    }

    if (args.messageId !== undefined && args.messageId !== null) {
        const messageId = String(args.messageId);
        const messageScopedCandidates = candidates.filter(el => {
            const container = el.closest('.message.confirmation-result, .message.confirmation');
            return container && container.dataset.messageId === messageId;
        });
        if (messageScopedCandidates.length > 0) {
            return messageScopedCandidates[messageScopedCandidates.length - 1];
        }
    }

    return candidates.length > 0 ? candidates[candidates.length - 1] : null;
}

function updateFunctionResult(args) {
    const el = findFunctionStatusTarget(args);
    if (el) {
        el.classList.remove('fn-awaiting-result');

        if (args.hasException) {
            const currentLabel = el.querySelector('.fn-summary-label');
            const currentText = currentLabel ? currentLabel.textContent : '';
            updateCollapsibleStatus(el, args.text || currentText, 'confirmation-status-error');
        }

        const resultDiv = el.querySelector('.fn-detail-result');
        if (resultDiv && args.result) {
            if (!resultDiv.querySelector('.fn-detail-label')) {
                const resultLabel = document.createElement('div');
                resultLabel.className = 'fn-detail-label';
                resultLabel.textContent = args.resultLabel || 'Result';
                resultDiv.appendChild(resultLabel);
            }
            if (!resultDiv.querySelector('.fn-detail-value')) {
                const resultValue = document.createElement('pre');
                resultValue.className = 'fn-detail-value';
                resultDiv.appendChild(resultValue);
            }
            resultDiv.querySelector('.fn-detail-value').textContent = args.result;
            resultDiv.style.display = '';
        }
        return;
    }

    if (!args.hasException) {
        return;
    }

    const container = document.createElement('div');
    container.className = 'message confirmation-result';
    if (args.messageId !== undefined && args.messageId !== null) {
        container.dataset.messageId = String(args.messageId);
    }

    const collapsible = createCollapsibleStatus(args.text || args.functionName, 'confirmation-status-error', {
        ...args,
        awaitResult: false
    });
    container.appendChild(collapsible);
    appendChatNode(container);
    updateFunctionResult({
        ...args,
        hasException: true
    });
}
