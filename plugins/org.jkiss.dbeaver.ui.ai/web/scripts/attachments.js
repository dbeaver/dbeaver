function parseAttachmentContent(content) {
    if (Array.isArray(content)) {
        return content.map(item => {
            if (item.path && !item.name) {
                item.name = item.path.split('/').pop();
            }
            return item;
        });
    }

    if (typeof content !== 'string' || !content.trim()) {
        return [];
    }

    const str = content.trim();

    const parsed = JSON.parse(str);
    if (Array.isArray(parsed)) {
        return parsed.map(item => {
            if (item.path && !item.name) {
                item.name = item.path.split('/').pop();
            }
            return item;
        });
    }

    const matches = str.match(/\{[^}]*\}/g);
    if (!matches) {
        return [];
    }

    return matches.map(block => {
        const inner = block.slice(1, -1).trim();
        if (!inner) return {};
        const pairs = inner.split(/,\s*(?=[\w-]+=)/);
        const obj = {};
        for (const pair of pairs) {
            const eq = pair.indexOf('=');
            if (eq === -1) continue;
            const key = pair.slice(0, eq).trim();
            let val = pair.slice(eq + 1).trim();
            if ((val.startsWith('"') && val.endsWith('"')) || (val.startsWith("'") && val.endsWith("'"))) {
                val = val.slice(1, -1);
            }
            if (/^\d+$/.test(val)) {
                obj[key] = Number(val);
            } else if (/^(true|false)$/i.test(val)) {
                obj[key] = val.toLowerCase() === 'true';
            } else {
                obj[key] = val;
            }
        }
        return obj;
    });
}


function createAttachmentCard(args) {
    let files = [];
    let items = '';
    try {
        files = parseAttachmentContent(args.content);

        if (!Array.isArray(files)) {
            return `<div class="attachment-card"><div class="attachment-error">%!#ai_chat_attachment_invalid_data</div></div>`;
        }

        items = files.map((f, index) => {
            const iconSrc = f.icon;
            const name = f.name || (f.path ? f.path.split('/').pop() : '%#ai_chat_attachment_unknown_file');
            return `
        <div class="attachment-item" role="button" tabindex="-1" onclick="window.openFileInExplorer(${args.id}, ${index});" style="cursor: pointer;" title="%!#ai_chat_attachment_show_in_explorer">
            <img src="${iconSrc}" class="attachment-icon" alt=""/>
            <div class="attachment-name" title="${escapeHtml(f.path)}">${escapeHtml(name)}</div>
        </div>`;
        }).join('');

    } catch (error) {
        return `<div class="attachment-card"><div class="attachment-error">${escapeHtml(formatChatString('%#ai_chat_attachment_create_error', error.message))}</div></div>`;
    }

    const canCreateConnection = args.canCreateConnection === true;
    const canImport = args.canImport === true;

    const isImportDataWasExecuted = args.isImportDataWasExecuted === true;
    const isCreateConnectionWasExecuted = args.isCreateConnectionWasExecuted === true;

    let actionsHtml = '';
    if (canCreateConnection || canImport) {
        actionsHtml = `
            <div id="attachment-actions-${args.id}" class="attachment-actions-response">
                ${getCreateButton(canCreateConnection, args.id)}
                ${getImportButton(canImport, args.id, files.length)}
            </div>
        `;
    } else {
        if (files.length === 1) {
            let listItems = '';
            try {
                const entries = window.getFileTypeHandlerExtensions();

                listItems = entries.map(e => {
                    return `<button class="attachment-btn" title="${escapeHtml(formatChatString('%#ai_chat_attachment_choose_type_tip', e))}"
                        onclick="window.onSetFileType(${args.id}, '${e}');">${e}</button>`;
                }).join('');
            } catch (e) {
                listItems = '';
            }

            const headerText = '%!#ai_chat_attachment_choose_type';

            actionsHtml = listItems ? `
                <div id="attachment-actions-${args.id}" class="attachment-actions-response">
                    <div style="font-size: 12px; opacity: 0.8; margin: 4px 0;">${headerText}</div>
                    <div class="handler-list" style="display:flex; gap:8px; flex-wrap: wrap;">
                        ${listItems}
                    </div>
                </div>
            ` : `
                <div class="attachment-actions-response">%!#ai_chat_attachment_no_handlers</div>`;
        } else {
            actionsHtml = `<div class="attachment-actions-response">%!#ai_chat_attachment_no_actions</div>`;
        }
    }

    if (isCreateConnectionWasExecuted) {
        actionsHtml = `
            <div id="attachment-actions-${args.id}" class="attachment-status">
                %!#ai_chat_attachment_open_completed
            </div>
        `;
    } else if (isImportDataWasExecuted) {
        actionsHtml = `
            <div id="attachment-actions-${args.id}" class="attachment-status">
                %!#ai_chat_attachment_import_completed
            </div>
        `;
    }
    const closeIconSrc = closeIcon;
    return `
        <div class="attachment-card">
            <img src="${closeIconSrc}"
                 class="attachment-close-icon"
                 alt=""
                 title="${escapeHtml(closeTooltip)}"
                 role="button"
                 tabindex="-1"
                 aria-label="${escapeHtml(closeTooltip)}"
                 onclick="deleteMessage(${args.id});" />
            <div class="attachment-files">
                ${items}
            </div>
            ${actionsHtml}
        </div>
    `;
}

function escapeHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function getCreateButton(canCreate, messageId) {
    if (!canCreate) return '';
    const label = '%!#ai_chat_attachment_open_as_table';
    return `<button class="attachment-btn" title="${label}" onclick="window.attachmentCreateConnection(${messageId}); showActionCompleted(${messageId}, 'open');">${label}</button>`;
}

function getImportButton(canImport, messageId, filesCount = 1) {
    if (!canImport) return '';

    const label = filesCount > 1 ? '%!#ai_chat_attachment_import_files' : '%!#ai_chat_attachment_import_file';

    return `<button class="attachment-btn" title="${label}" onclick="window.attachmentExportToTable(${messageId}); showActionCompleted(${messageId}, 'import');">${label}</button>`;
}

function onSetFileType(messageId, extensionId) {
    if (typeof window.setAttachmentFileType !== 'function') {
        return;
    }
    const result = window.setAttachmentFileType(messageId, extensionId);
    const canCreate = Array.isArray(result) && !!result[0];
    const canImport = Array.isArray(result) && !!result[1];
    renderAttachmentActions(messageId, canCreate, canImport);
}

function renderAttachmentActions(messageId, canCreate, canImport) {
    const container = document.getElementById(`attachment-actions-${messageId}`);
    if (!container) return;
    if (!canCreate && !canImport) {
        container.innerHTML = `<div>%!#ai_chat_attachment_no_type_actions</div>`;
        return;
    }

    const messageElement = document.getElementById(messageId);
    const filesCount = messageElement ? messageElement.querySelectorAll('.attachment-item').length : 1;

    container.innerHTML = `${getCreateButton(canCreate, messageId)} ${getImportButton(canImport, messageId, filesCount)}`;
}

function showActionCompleted(messageId, action) {
    const messageElement = document.getElementById(messageId);
    if (!messageElement) {
        return;
    }

    const attachmentCard = messageElement.querySelector('.attachment-card');
    if (!attachmentCard) {
        return;
    }

    const actionsContainer = attachmentCard.querySelector('.attachment-actions-response');
    if (actionsContainer) {
        actionsContainer.classList.add('hidden');
    }

    const existingStatus = attachmentCard.querySelector('.attachment-status');
    if (existingStatus) {
        existingStatus.remove();
    }

    const statusDiv = document.createElement('div');
    statusDiv.className = 'attachment-status';
    statusDiv.textContent = action === 'open' ? '%#ai_chat_attachment_open_completed' : '%#ai_chat_attachment_import_completed';

    attachmentCard.appendChild(statusDiv);
}
