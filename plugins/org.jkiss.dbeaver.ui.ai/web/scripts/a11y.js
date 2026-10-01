const A11Y_INTERACTIVE_SELECTOR =
    'button:not([disabled]), a[href], a.interactive-link, summary, input, select, textarea, [role="button"]';
const A11Y_DISMISS_SELECTOR = '.message-clean-btn, .attachment-close-icon';
const A11Y_ANNOUNCE_MAX_LENGTH = 4000;

let a11yStrings = {roles: {}};
let a11yAnnouncementsSuspended = false;
let a11yCurrentItem = null;

function initA11y(strings) {
    a11yStrings = strings || {};
    a11yStrings.roles = a11yStrings.roles || {};

    if (a11yStrings.transcriptLabel) {
        chat.setAttribute('aria-label', a11yStrings.transcriptLabel);
    }
    chat.tabIndex = -1;
}
function isTranscriptItem(element) {
    return element instanceof HTMLElement
        && element.parentElement === chat
        && element.matches('div:not(.status-bar)');
}

function getNavigableItems() {
    return Array.from(chat.querySelectorAll(':scope > div:not(.status-bar)'));
}

function getEnclosingItem(element) {
    while (element && element !== chat) {
        if (isTranscriptItem(element)) {
            return element;
        }
        element = element.parentElement;
    }
    return null;
}

function setCurrentItem(element) {
    if (a11yCurrentItem === element) {
        return;
    }
    if (a11yCurrentItem && a11yCurrentItem.isConnected) {
        a11yCurrentItem.tabIndex = -1;
        a11yCurrentItem.setAttribute('aria-selected', 'false');
    }
    a11yCurrentItem = element;
    if (element) {
        element.tabIndex = 0;
        element.setAttribute('aria-selected', 'true');
    }
}

function getCurrentItem() {
    if (a11yCurrentItem && a11yCurrentItem.isConnected) {
        return a11yCurrentItem;
    }
    const items = getNavigableItems();
    return items.length ? items[items.length - 1] : null;
}

function makeItemFocusable(element, role) {
    if (!element || element.hasAttribute('data-a11y-item')) {
        return;
    }
    element.setAttribute('data-a11y-item', 'true');
    element.setAttribute('role', 'option');
    element.setAttribute('aria-selected', 'false');
    element.tabIndex = -1;
    if (role) {
        element.dataset.a11yRole = role;
    }
    updateItemAccessibleName(element);
    if (!chat.contains(document.activeElement)) {
        setCurrentItem(element);
    }
}

function updateItemAccessibleName(item) {
    const roleLabel = a11yStrings.roles[item.dataset.a11yRole] || '';
    const text = getMessageText(item);
    if (roleLabel || text) {
        item.setAttribute('aria-label', roleLabel && text ? roleLabel + ': ' + text : roleLabel || text);
    }
}

function focusChat() {
    if (chat.contains(document.activeElement)) {
        return;
    }
    const target = getCurrentItem() || chat;
    focusItem(target);
}

function focusItem(item) {
    if (item !== chat) {
        if (!item.hasAttribute('tabindex')) {
            item.tabIndex = -1;
        }
        setCurrentItem(item);
        updateItemAccessibleName(item);
    }
    item.classList.add('a11y-focus');
    item.focus();
    if (typeof item.scrollIntoView === 'function') {
        item.scrollIntoView({block: 'nearest'});
    }
}

function getInteractiveDescendants(item) {
    return Array.from(item.querySelectorAll(A11Y_INTERACTIVE_SELECTOR))
        .filter(el => (el.offsetWidth || el.offsetHeight || el.getClientRects().length)
            && getComputedStyle(el).visibility !== 'hidden');
}

let a11yLastInputWasMouse = false;

chat.addEventListener('mousedown', event => {
    if (event.detail === 0) {
        return;
    }
    a11yLastInputWasMouse = true;
    chat.classList.remove('a11y-focus');
    for (const el of chat.querySelectorAll('.a11y-focus')) {
        el.classList.remove('a11y-focus');
    }
}, true);

document.addEventListener('keydown', () => {
    a11yLastInputWasMouse = false;
}, true);

window.addEventListener('blur', () => {
    a11yLastInputWasMouse = false;
});

chat.addEventListener('focusin', event => {
    const target = event.target;
    if (!a11yLastInputWasMouse
        && target instanceof HTMLElement
        && (isTranscriptItem(target) || target === chat)
    ) {
        target.classList.add('a11y-focus');
    }
});

chat.addEventListener('focusout', event => {
    if (event.target instanceof HTMLElement) {
        event.target.classList.remove('a11y-focus');
    }
});

chat.addEventListener('keydown', function(event) {
    const target = event.target;
    const isItem = isTranscriptItem(target);
    const isChat = target === chat;

    if ((event.key === 'Enter' || event.key === ' ')
        && !isItem && !isChat
        && target instanceof HTMLElement
        && target.getAttribute('role') === 'button'
    ) {
        event.preventDefault();
        target.click();
        return;
    }

    if (event.key === 'Escape') {
        event.preventDefault();
        if (isItem || isChat) {
            if (typeof window.traverseOut === 'function') {
                window.traverseOut(true);
            }
        } else {
            const item = getEnclosingItem(target);
            if (item) {
                focusItem(item);
            }
        }
        return;
    }

    if (event.key === 'Tab') {
        if (event.ctrlKey || event.altKey || event.metaKey) {
            return;
        }
        if (isItem || isChat) {
            if (typeof window.traverseOut === 'function') {
                event.preventDefault();
                window.traverseOut(!event.shiftKey);
            }
            return;
        }
        const item = getEnclosingItem(target);
        if (item) {
            const controls = getInteractiveDescendants(item);
            const index = controls.indexOf(target);
            if (index !== -1) {
                event.preventDefault();
                const nextIndex = index + (event.shiftKey ? -1 : 1);
                if (nextIndex < 0 || nextIndex >= controls.length) {
                    focusItem(item);
                } else {
                    controls[nextIndex].focus();
                }
            }
        }
        return;
    }

    if (!isItem && !isChat) {
        if ((event.key === 'ArrowLeft' || event.key === 'ArrowRight')
            && target instanceof HTMLElement
            && !isTextEntryControl(target)
        ) {
            const item = getEnclosingItem(target);
            if (item) {
                const controls = getInteractiveDescendants(item);
                const index = controls.indexOf(target);
                const next = index === -1 ? null : controls[index + (event.key === 'ArrowRight' ? 1 : -1)];
                if (next) {
                    event.preventDefault();
                    next.focus();
                }
            }
        }
        return;
    }

    switch (event.key) {
        case 'ArrowDown':
            moveItemFocus(event, 1);
            break;
        case 'ArrowUp':
            moveItemFocus(event, -1);
            break;
        case 'Home':
            focusItemAt(event, 0);
            break;
        case 'End':
            focusItemAt(event, -1);
            break;
        case 'Enter':
        case 'F2':
            if (isItem) {
                const controls = getInteractiveDescendants(target);
                if (controls.length) {
                    event.preventDefault();
                    const primary = controls.find(el => !el.matches(A11Y_DISMISS_SELECTOR)) || controls[0];
                    primary.focus();
                }
            }
            break;
    }
});

function isTextEntryControl(element) {
    return element.isContentEditable
        || element instanceof HTMLTextAreaElement
        || element instanceof HTMLSelectElement
        || (element instanceof HTMLInputElement
            && !['button', 'checkbox', 'radio', 'submit', 'reset'].includes(element.type));
}

function moveItemFocus(event, delta) {
    event.preventDefault();
    const current = getEnclosingItem(event.target);
    let item;
    if (current) {
        item = delta > 0 ? current.nextElementSibling : current.previousElementSibling;
        while (item && !isTranscriptItem(item)) {
            item = delta > 0 ? item.nextElementSibling : item.previousElementSibling;
        }
    } else {
        const items = getNavigableItems();
        item = delta > 0 ? items[0] : items[items.length - 1];
    }
    if (item) {
        focusItem(item);
    }
}

function focusItemAt(event, index) {
    const items = getNavigableItems();
    if (!items.length) {
        return;
    }
    event.preventDefault();
    focusItem(items[(index + items.length) % items.length]);
}

new MutationObserver(mutations => {
    for (const mutation of mutations) {
        for (const node of mutation.removedNodes) {
            if (node === a11yCurrentItem) {
                const items = getNavigableItems();
                setCurrentItem(items.length ? items[items.length - 1] : null);
            }
        }
    }
}).observe(chat, {childList: true});
let a11yPendingAnnouncements = [];
let a11yAnnounceTimer = null;

function announce(text) {
    if (a11yAnnouncementsSuspended || !text) {
        return;
    }
    const announcer = document.getElementById('a11y-announcer');
    if (!announcer) {
        return;
    }
    a11yPendingAnnouncements.push(String(text).substring(0, A11Y_ANNOUNCE_MAX_LENGTH));
    if (a11yAnnounceTimer) {
        return;
    }
    announcer.textContent = '';
    a11yAnnounceTimer = setTimeout(() => {
        a11yAnnounceTimer = null;
        announcer.textContent = a11yPendingAnnouncements.join('. ').substring(0, A11Y_ANNOUNCE_MAX_LENGTH);
        a11yPendingAnnouncements = [];
    }, 30);
}

function announceMessage(role, element) {
    if (a11yAnnouncementsSuspended || !element) {
        return;
    }
    const roleLabel = a11yStrings.roles[role];
    const text = getMessageText(element);
    if (!text) {
        return;
    }
    announce(roleLabel ? roleLabel + ': ' + text : text);
}

const A11Y_TEXT_EXCLUDED_CLASSES = ['message-meta', 'sr-only', 'message-icons-container', 'tooltip'];

function getMessageText(element) {
    // Collect text nodes up to the announcement cap instead of cloning
    // the whole (potentially huge) message subtree
    const walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT, {
        acceptNode(node) {
            for (let el = node.parentElement; el && el !== element; el = el.parentElement) {
                if (A11Y_TEXT_EXCLUDED_CLASSES.some(cls => el.classList.contains(cls))) {
                    return NodeFilter.FILTER_REJECT;
                }
            }
            return NodeFilter.FILTER_ACCEPT;
        }
    });
    let text = '';
    let node;
    while (text.length < A11Y_ANNOUNCE_MAX_LENGTH && (node = walker.nextNode())) {
        text += node.nodeValue + ' ';
    }
    return text.replace(/\s+/g, ' ').trim();
}

function announceWaiting() {
    announce(a11yStrings.waitingForResponse);
}

function setAnnouncementsSuspended(suspended) {
    a11yAnnouncementsSuspended = Boolean(suspended);
}
