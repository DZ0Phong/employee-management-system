/**
 * Leave Approval / Request Management Enhancements
 * - Auto-expand detail when requestId in URL
 * - View Full Detail redirect
 * - Email notification for CRITICAL items
 */

(function () {
    'use strict';

    // ══════════════════════════════════════════════════════════════════════════
    // AUTO-EXPAND DETAIL WHEN requestId IN URL
    // ══════════════════════════════════════════════════════════════════════════

    function autoExpandFromURL() {
        const urlParams = new URLSearchParams(window.location.search);
        const requestId = urlParams.get('requestId');

        if (!requestId) {
            console.log('No requestId in URL');
            return;
        }

        console.log('Auto-expanding request:', requestId);

        // Find the row with this requestId
        const row = document.querySelector(`tr[data-id="${requestId}"]`);

        if (!row) {
            console.warn('Row not found for requestId:', requestId);
            console.log('Available rows:', document.querySelectorAll('tr[data-id]').length);
            // Try to find by id attribute instead
            const rowById = document.getElementById(`row-${requestId}`);
            if (rowById) {
                console.log('Found row by ID instead');
                expandRow(rowById, requestId);
            }
            return;
        }

        console.log('Found row for request:', requestId);
        expandRow(row, requestId);
    }

    function expandRow(row, requestId) {
        try {
            // Scroll to the row
            setTimeout(() => {
                row.scrollIntoView({ behavior: 'smooth', block: 'center' });
                console.log('Scrolled to row');

                // Highlight the row briefly
                row.classList.add('bg-primary/10');
                setTimeout(() => {
                    row.classList.remove('bg-primary/10');
                }, 2000);

                // Auto-expand the detail - check if toggleDetail function exists
                setTimeout(() => {
                    if (typeof toggleDetail === 'function') {
                        console.log('Calling toggleDetail for:', requestId);
                        toggleDetail(parseInt(requestId));
                    } else {
                        console.warn('toggleDetail function not found, trying alternative method');
                        // Alternative: trigger click on the review button
                        const reviewBtn = document.getElementById(`btn-${requestId}`);
                        if (reviewBtn) {
                            console.log('Clicking review button');
                            reviewBtn.click();
                        } else {
                            console.error('Review button not found:', `btn-${requestId}`);
                        }
                    }
                }, 500);
            }, 300);
        } catch (error) {
            console.error('Error expanding row:', error);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EMAIL NOTIFICATION FOR CRITICAL ITEMS
    // ══════════════════════════════════════════════════════════════════════════

    function checkCriticalItems() {
        // Find all CRITICAL priority items that are PENDING
        const criticalRows = document.querySelectorAll('tr[data-priority="critical"][data-status="pending"]');

        if (criticalRows.length > 0) {
            // Show notification badge
            showCriticalNotification(criticalRows.length);

            // Send email notification (if not already sent today)
            sendCriticalEmailNotification(criticalRows.length);
        }
    }

    function showCriticalNotification(count) {
        // Update notification bell badge
        const notificationBell = document.querySelector('.notification-bell-badge');
        if (notificationBell) {
            notificationBell.textContent = count;
            notificationBell.classList.remove('hidden');
        }

        // Show toast notification
        if (typeof showToast === 'function') {
            showToast(`${count} CRITICAL request(s) need immediate attention!`, 'warning');
        }
    }

    function sendCriticalEmailNotification(count) {
        // Check if we already sent notification today
        const lastSent = localStorage.getItem('criticalEmailSentDate');
        const today = new Date().toDateString();

        if (lastSent === today) {
            console.log('Critical email already sent today');
            return;
        }

        // Send AJAX request to backend to trigger email
        fetch('/hrmanager/leave-approval/notify-critical', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': getCsrfToken()
            },
            body: JSON.stringify({ count: count })
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    localStorage.setItem('criticalEmailSentDate', today);
                    console.log('Critical email notification sent');
                }
            })
            .catch(error => {
                console.error('Failed to send critical email:', error);
            });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // VIEW FULL DETAIL - ADD BUTTONS TO ACTIONS COLUMN
    // ══════════════════════════════════════════════════════════════════════════

    function addFullDetailButtons() {
        // DISABLED: Button removed for cleaner UI
        // Auto-expand functionality still works via URL parameter
        console.log('Full Detail buttons disabled for cleaner UI');
        return;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // QUICK STATS - REAL-TIME UPDATE
    // ══════════════════════════════════════════════════════════════════════════

    function updateQuickStats() {
        // Count from visible rows
        const allRows = document.querySelectorAll('.data-row');
        const pendingRows = document.querySelectorAll('tr[data-status="pending"]');
        const approvedRows = document.querySelectorAll('tr[data-status="approved"]');
        const rejectedRows = document.querySelectorAll('tr[data-status="rejected"]');

        // Update count badges
        updateCountBadge('cnt-all', allRows.length);
        updateCountBadge('cnt-pending', pendingRows.length);
        updateCountBadge('pending-count-badge', pendingRows.length);
    }

    function updateCountBadge(id, count) {
        const badge = document.getElementById(id);
        if (badge) {
            badge.textContent = count;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ══════════════════════════════════════════════════════════════════════════

    function getCsrfToken() {
        const meta = document.querySelector('meta[name="_csrf"]');
        return meta ? meta.getAttribute('content') : '';
    }

    // ══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ══════════════════════════════════════════════════════════════════════════

    function init() {
        console.log('Leave Approval Enhancements: Starting initialization...');

        // Wait for both DOM and window to be fully loaded
        if (document.readyState === 'loading') {
            console.log('DOM still loading, waiting...');
            document.addEventListener('DOMContentLoaded', init);
            return;
        }

        // Additional wait to ensure all scripts are loaded
        setTimeout(() => {
            try {
                console.log('Leave Approval Enhancements: Running initialization...');

                // Auto-expand from URL
                autoExpandFromURL();

                // Add Full Detail buttons
                addFullDetailButtons();

                // Check for critical items
                checkCriticalItems();

                // Update quick stats
                updateQuickStats();

                // Re-run after table updates (e.g., after filtering)
                // Use debounce to prevent infinite loops
                let mutationTimeout;
                const observer = new MutationObserver(() => {
                    // Debounce: only run after mutations stop for 100ms
                    clearTimeout(mutationTimeout);
                    mutationTimeout = setTimeout(() => {
                        console.log('Table mutated, updating buttons and stats');
                        addFullDetailButtons();
                        updateQuickStats();
                    }, 100);
                });

                const tableBody = document.getElementById('table-body');
                if (tableBody) {
                    // Only observe childList changes, not subtree
                    observer.observe(tableBody, {
                        childList: true,
                        subtree: false // Don't observe deep changes to prevent infinite loop
                    });
                    console.log('MutationObserver attached to table-body');
                } else {
                    console.warn('table-body element not found');
                }

                console.log('Leave Approval Enhancements: Ready ✓');
            } catch (error) {
                console.error('Leave Approval Enhancements: Initialization error', error);
            }
        }, 1000); // Wait 1 second for everything to load
    }

    // Start initialization when window is fully loaded
    if (document.readyState === 'complete') {
        init();
    } else {
        window.addEventListener('load', init);
    }

})();
