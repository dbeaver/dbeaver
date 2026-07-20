function createCleanIcon(messageId) {
    const clean = document.createElement('img');
    clean.className = 'message-clean-icon';
    clean.src = cleanIcon;
    clean.setAttribute('aria-label', cleanTooltip);

    clean.addEventListener('mouseenter', (event) => {
        showCustomTooltip(event, cleanTooltip);
    });

    clean.addEventListener('mouseleave', () => {
        hideCustomTooltip();
    });

    clean.addEventListener('click', (event) => {
        event.stopPropagation();
        clearToHere(messageId);
        hideCustomTooltip();
    });


    return clean;
}

function showCustomTooltip(event, text) {
    const tooltip = document.createElement('div');
    tooltip.className = 'tooltip show';
    tooltip.textContent = text;
    tooltip.id = 'custom-clean-tooltip';

    document.body.appendChild(tooltip);

    const rect = tooltip.getBoundingClientRect();
    const viewportHeight = window.innerHeight;

    let x = event.clientX - rect.width/2;
    let y = event.clientY + 15;

    if (x + rect.width > window.innerWidth) {
        x = window.innerWidth - rect.width - 5;
    }

    if (x < 5) {
        x = 5;
    }

    if (y + rect.height > viewportHeight) {
        y = event.clientY - rect.height - 10;
    }

    tooltip.style.left = x + 'px';
    tooltip.style.top = y + 'px';

}

function hideCustomTooltip() {
    const tooltip = document.getElementById('custom-clean-tooltip');
    if (tooltip) {
        tooltip.remove();
    }
}
