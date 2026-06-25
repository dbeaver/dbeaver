function createContent(args) {
    const holder = document.createElement('p');

    const body = document.createElement('p');
    if (args.role === 'attachment') {
        body.innerHTML = createAttachmentCard(args);
    } else {
        const markdown = marked.parse(args.content);
        body.innerHTML = parseSqlBlocks(markdown, args.id);
    }

    holder.appendChild(body);

    if (args.role == 'assistant' && typeof args.meta === 'object') {
        const meta = document.createElement('p');
        meta.className = 'message-meta'
        meta.innerText = formatMessageMeta(args.meta);

        setElementMeta(meta, args.meta);
        updateMessageMeta(meta);

        holder.appendChild(meta);
    }

    return holder;
}

function addMessage(args) {
    hideCenterText();

    const existingMessage = document.getElementById(args.id);
    if (existingMessage) {
        if (existingMessage.classList.contains('streaming')) {
            if (typeof args.content === 'string') {
                streamingMessages.set(args.id, args.content);
            }
            finalizeStreamingMessage(args.id, args.meta);
            return;
        }
        return;
    }

    if (args.role === 'assistant' && currentStreamingMessageId) {
        const streamingMessage = document.getElementById(currentStreamingMessageId);
        if (streamingMessage && streamingMessage.classList.contains('streaming')) {
            const finalContent = typeof args.content === 'string'
                ? args.content
                : (streamingMessages.get(currentStreamingMessageId) || '');

            streamingMessage.id = args.id;
            streamingMessages.set(args.id, finalContent);
            streamingMessages.delete(currentStreamingMessageId);
            currentStreamingMessageId = args.id;
            finalizeStreamingMessage(args.id, args.meta);
            return;
        }

        streamingMessages.delete(currentStreamingMessageId);
        currentStreamingMessageId = null;
    }

    const div = document.createElement('div');
    div.id = args.id;
    div.className = 'message ' + getClassName(args.role);

    if (args.role === 'error' || args.role === 'warning') {
        const icon = document.createElement('img');
        icon.className = 'message-type-icon';
        icon.src = args.icon;
        icon.alt = args.role;

        div.appendChild(icon);
    }

    div.appendChild(createContent(args));

    const userMessages = document.querySelectorAll('.message.right, .message.attachment');
    if (args.role === 'user' && userMessages.length > 0) {
        const iconsContainer = document.createElement('div');
        iconsContainer.className = 'message-icons-container';
        iconsContainer.appendChild(createCleanIcon(args.id));

        div.addEventListener('mouseenter', () => {
            iconsContainer.style.opacity = '1';
            iconsContainer.style.visibility = 'visible';
        });

        div.addEventListener('mouseleave', () => {
            iconsContainer.style.opacity = '0';
            iconsContainer.style.visibility = 'hidden';
        });

        div.appendChild(iconsContainer);

        setTimeout(() => {
            const offset = calculateIconsContainerOffset(div, iconsContainer);
            if (offset > 0) {
                iconsContainer.style.transform = `translateY(-${offset}px) translateX(-5px)`;
            }
        }, 0);
    }

    appendChatNode(div);
}

function calculateIconsContainerOffset(messageDiv, iconsContainer) {
    const sqlActions = messageDiv.querySelectorAll('.sql-actions');

    if (sqlActions.length === 0) {
        return 0;
    }

    const lastSqlActions = sqlActions[sqlActions.length - 1];
    const sqlButtons = lastSqlActions.querySelectorAll('.sql-button');

    const iconsRect = iconsContainer.getBoundingClientRect();
    let maxOffset = 0;
    let hasCollision = false;

    sqlButtons.forEach(button => {
        const buttonRect = button.getBoundingClientRect();

        const horizontalOverlap = !(iconsRect.right < buttonRect.left || iconsRect.left > buttonRect.right);
        const verticalOverlap = !(iconsRect.bottom < buttonRect.top || iconsRect.top > buttonRect.bottom);

        if (horizontalOverlap && verticalOverlap) {
            hasCollision = true;
            const neededOffset = (iconsRect.bottom - buttonRect.top) + 10;
            maxOffset = Math.max(maxOffset, neededOffset);
        }
    });

    return hasCollision ? maxOffset : 0;
}

function removeMessage(args) {
    removeFunctionArtifacts(args.id);
    const el = document.getElementById(args.id);
    if (el) el.remove();
    streamingMessages.delete(args.id);
    if (currentStreamingMessageId === String(args.id)) {
        currentStreamingMessageId = null;
    }
    ensureBusyIndicator();
}

function removeFunctionArtifacts(messageId) {
    const id = String(messageId);
    const confirmationPrefix = 'confirm-' + id + '-';
    document.querySelectorAll('.message.confirmation, .message.confirmation-result').forEach(el => {
        if (el.dataset.messageId === id || el.id.startsWith(confirmationPrefix)) {
            el.remove();
        }
    });
}

