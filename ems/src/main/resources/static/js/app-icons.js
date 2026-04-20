(function () {
    const SPRITE_PATH = '/icons/app-symbols.svg';

    function buildIcon(name, classes, filled) {
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('class', `app-icon ${classes}`.trim());
        if (filled) {
            svg.setAttribute('data-filled', 'true');
        }
        svg.setAttribute('viewBox', '0 0 24 24');
        svg.setAttribute('aria-hidden', 'true');

        const use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
        use.setAttributeNS('http://www.w3.org/1999/xlink', 'href', `${SPRITE_PATH}#${name}`);
        svg.appendChild(use);
        return svg;
    }

    function hydrateIconSpan(span) {
        if (!span || span.dataset.iconHydrated === 'true') {
            return;
        }

        const iconName = (span.dataset.iconName || span.textContent || '').trim();
        if (!iconName) {
            return;
        }

        span.dataset.iconName = iconName;
        span.dataset.iconHydrated = 'true';

        const inheritedClasses = Array.from(span.classList)
            .filter(className => className !== 'material-symbols-outlined' && className !== 'fill-icon')
            .join(' ');

        const filled = span.classList.contains('fill-icon');
        if (filled) {
            span.setAttribute('data-filled', 'true');
        }
        span.textContent = '';
        span.appendChild(buildIcon(iconName, inheritedClasses, filled));
    }

    function hydrate(root) {
        const scope = root || document;
        scope.querySelectorAll('.material-symbols-outlined').forEach(hydrateIconSpan);
    }

    function setIcon(target, iconName) {
        const span = typeof target === 'string' ? document.querySelector(target) : target;
        if (!span || !iconName) {
            return;
        }

        span.dataset.iconHydrated = 'false';
        span.dataset.iconName = iconName;
        span.textContent = iconName;
        hydrateIconSpan(span);
    }

    window.hydrateAppIcons = hydrate;
    window.setAppIcon = setIcon;

    function watchMutations() {
        if (typeof MutationObserver === 'undefined' || !document.body) {
            return;
        }

        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                mutation.addedNodes.forEach((node) => {
                    if (!(node instanceof Element)) {
                        return;
                    }

                    if (node.matches('.material-symbols-outlined')) {
                        hydrateIconSpan(node);
                    }

                    hydrate(node);
                });
            });
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            hydrate(document);
            watchMutations();
        });
    } else {
        hydrate(document);
        watchMutations();
    }
})();
