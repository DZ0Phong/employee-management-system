/**
 * HR MODULE COMMON SCRIPTS
 */

document.addEventListener('DOMContentLoaded', () => {
    // Shared navigation or initialization logic if needed
});

/**
 * Switch tabs in any panel/modal
 * @param {string} tabName - The name of the tab to switch to
 * @param {string} prefix - The ID prefix for tab and content (e.g., 'tab-', 'ltab-')
 * @param {string} contentPrefix - The ID prefix for content panel (e.g., 'content-', 'lpanel-')
 */
function switchGenericTab(tabName, prefix, contentPrefix) {
    document.querySelectorAll('.' + prefix + 'tab').forEach(t => { 
        t.classList.remove('active'); 
        if (t.classList.contains('text-slate-400')) {
            // Specifically for the employee modal
        } else if (prefix === 'l') {
             // Specifically for leave tabs
        }
    });
    
    document.querySelectorAll('.' + contentPrefix + 'panel, .tab-content').forEach(c => {
        if (c.id.startsWith(contentPrefix)) {
            c.classList.remove('active');
        }
    });

    const activeTab = document.getElementById(prefix + 'tab-' + tabName);
    if (activeTab) {
        activeTab.classList.add('active');
    }
    
    const activePanel = document.getElementById(contentPrefix + '-' + tabName);
    if (activePanel) {
        activePanel.classList.add('active');
    }
}

/**
 * Specific for Employee Modal (used in employees.html)
 */
function switchTab(tab) {
    document.querySelectorAll('.modal-tab').forEach(t => { 
        t.classList.remove('active'); 
        t.classList.add('text-slate-400'); 
    });
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
    
    const activeTab = document.getElementById('tab-' + tab);
    if (activeTab) {
        activeTab.classList.add('active');
        activeTab.classList.remove('text-slate-400');
    }
    
    const activePanel = document.getElementById('content-' + tab);
    if (activePanel) {
        activePanel.classList.add('active');
    }
}

/**
 * Specific for Leave Tabs (used in leave.html)
 */
function switchLeaveTab(tab) {
    document.querySelectorAll('.leave-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.leave-panel').forEach(p => p.classList.remove('active'));
    
    const activeTab = document.getElementById('ltab-' + tab);
    if (activeTab) {
        activeTab.classList.add('active');
    }
    
    const activePanel = document.getElementById('lpanel-' + tab);
    if (activePanel) {
        activePanel.classList.add('active');
    }
}
