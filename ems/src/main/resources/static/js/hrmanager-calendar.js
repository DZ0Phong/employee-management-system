/**
 * HR Manager Calendar - Department Multi-select & Company-wide Events
 */

// Departments data (loaded from API)
var departments = [];

/**
 * Fetch departments from API
 */
function loadDepartments() {
    fetch('/hrmanager/calendar/departments')
        .then(function(response) { return response.json(); })
        .then(function(data) {
            departments = data;
            initDepartmentCheckboxes();
        })
        .catch(function(error) {
            console.error('Failed to load departments:', error);
            // Fallback to sample data
            departments = [
                { id: 1, name: 'Engineering' },
                { id: 2, name: 'Marketing' },
                { id: 3, name: 'Sales' },
                { id: 4, name: 'HR' },
                { id: 5, name: 'Finance' }
            ];
            initDepartmentCheckboxes();
        });
}

/**
 * Initialize department checkboxes
 */
function initDepartmentCheckboxes() {
    var container = document.getElementById('departmentCheckboxes');
    if (!container) return;

    var html = '';
    departments.forEach(function(dept) {
        html += '<label class="flex items-center gap-2 p-2 hover:bg-white dark:hover:bg-slate-700 rounded cursor-pointer transition-colors">'
            + '<input type="checkbox" name="departments" value="' + dept.id + '" '
            + 'class="rounded border-slate-300 text-primary focus:ring-primary dept-checkbox" '
            + 'onchange="updateAssignedDepartments()" />'
            + '<span class="text-sm">' + dept.name + '</span>'
            + '</label>';
    });
    container.innerHTML = html;
    
    // Also populate department filter dropdown
    populateDepartmentFilter();
}

/**
 * Populate department filter dropdown
 */
function populateDepartmentFilter() {
    var select = document.getElementById('departmentFilter');
    if (!select) return;
    
    // Remove existing department options (keep All and Company-wide)
    while (select.options.length > 2) {
        select.remove(2);
    }
    
    // Add department options
    departments.forEach(function(dept) {
        var option = document.createElement('option');
        option.value = dept.id;
        option.textContent = dept.name;
        select.appendChild(option);
    });
}

/**
 * Toggle department fields based on company-wide checkbox
 */
function toggleDepartmentFields(isCompanyWide) {
    var deptFields = document.getElementById('departmentFields');
    if (!deptFields) return;

    if (isCompanyWide) {
        deptFields.style.display = 'none';
        // Clear all department selections
        var checkboxes = document.querySelectorAll('.dept-checkbox');
        checkboxes.forEach(function(cb) { cb.checked = false; });
        document.getElementById('assignedDepartments').value = '';
    } else {
        deptFields.style.display = 'block';
    }
}

/**
 * Update hidden field with selected department IDs as JSON array
 */
function updateAssignedDepartments() {
    var checkboxes = document.querySelectorAll('.dept-checkbox:checked');
    var selectedIds = [];
    checkboxes.forEach(function(cb) {
        selectedIds.push(cb.value);
    });
    
    // Store as JSON array: ["1","2","3"]
    var jsonValue = selectedIds.length > 0 ? JSON.stringify(selectedIds) : '';
    document.getElementById('assignedDepartments').value = jsonValue;
}

/**
 * Load department selections when editing an event
 */
function loadDepartmentSelections(assignedDepartmentsJson) {
    // Clear all checkboxes first
    var checkboxes = document.querySelectorAll('.dept-checkbox');
    checkboxes.forEach(function(cb) { cb.checked = false; });

    if (!assignedDepartmentsJson) return;

    try {
        var selectedIds = JSON.parse(assignedDepartmentsJson);
        if (Array.isArray(selectedIds)) {
            selectedIds.forEach(function(id) {
                var checkbox = document.querySelector('.dept-checkbox[value="' + id + '"]');
                if (checkbox) checkbox.checked = true;
            });
        }
    } catch (e) {
        console.error('Failed to parse assigned departments:', e);
    }
}

/**
 * Get department names from IDs
 */
function getDepartmentNames(assignedDepartmentsJson) {
    if (!assignedDepartmentsJson) return '';

    try {
        var selectedIds = JSON.parse(assignedDepartmentsJson);
        if (!Array.isArray(selectedIds) || selectedIds.length === 0) return '';

        var names = [];
        selectedIds.forEach(function(id) {
            var dept = departments.find(function(d) { return d.id == id; });
            if (dept) names.push(dept.name);
        });
        return names.join(', ');
    } catch (e) {
        return '';
    }
}

/**
 * Render department badge in event display
 */
function renderDepartmentBadge(event) {
    if (event.isCompanyWide) {
        return '<span class="inline-flex items-center gap-1 px-2 py-0.5 bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400 rounded text-[10px] font-bold">'
            + '<span class="material-symbols-outlined text-[14px]">corporate_fare</span>'
            + 'Company-wide'
            + '</span>';
    }

    var deptNames = getDepartmentNames(event.assignedDepartments);
    if (!deptNames) return '';

    var deptArray = deptNames.split(', ');
    if (deptArray.length === 1) {
        return '<span class="inline-flex items-center gap-1 px-2 py-0.5 bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 rounded text-[10px] font-bold">'
            + '<span class="material-symbols-outlined text-[14px]">folder</span>'
            + deptNames
            + '</span>';
    } else {
        return '<span class="inline-flex items-center gap-1 px-2 py-0.5 bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-400 rounded text-[10px] font-bold">'
            + '<span class="material-symbols-outlined text-[14px]">groups</span>'
            + deptArray.length + ' departments'
            + '</span>';
    }
}

/**
 * Enhanced openEditModalFromData to include department data
 */
var originalOpenEditModalFromData = window.openEditModalFromData;
window.openEditModalFromData = function(ev) {
    if (originalOpenEditModalFromData) {
        originalOpenEditModalFromData(ev);
    }

    // Load company-wide status
    var isCompanyWide = ev.isCompanyWide || false;
    document.getElementById('eventCompanyWide').checked = isCompanyWide;
    toggleDepartmentFields(isCompanyWide);

    // Load department selections
    if (!isCompanyWide && ev.assignedDepartments) {
        loadDepartmentSelections(ev.assignedDepartments);
    }
};

/**
 * Enhanced renderDayEvents to show department badges
 */
var originalRenderDayEvents = window.renderDayEvents;
window.renderDayEvents = function(events) {
    var container = document.getElementById('dayDetailEventsList');

    if (events.length === 0) {
        container.innerHTML = '<div class="text-center py-8 text-slate-400">'
            + '<span class="material-symbols-outlined text-4xl mb-2">event_busy</span>'
            + '<p class="text-sm">No events on this day</p>'
            + '</div>';
        if (window.hydrateAppIcons) window.hydrateAppIcons(container);
        return;
    }

    var html = '';
    events.forEach(function (ev) {
        var colorClass = window.getEventBgClass ? window.getEventBgClass(ev.color) : 'bg-blue-50 text-blue-600';
        var timeStr = ev.isAllDay ? 'All Day' : (ev.startTime || '') + (ev.endTime ? ' - ' + ev.endTime : '');
        var typeLabel = ev.type || 'OTHER';
        var deptBadge = renderDepartmentBadge(ev);

        html += '<div class="bg-slate-50 dark:bg-slate-800 rounded-xl p-4 hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors">'
            + '<div class="flex items-start justify-between gap-3">'
            + '<div class="flex-1">'
            + '<div class="flex items-center gap-2 mb-1">'
            + '<div class="w-3 h-3 rounded-full ' + colorClass.split(' ')[0] + '"></div>'
            + '<h4 class="font-bold text-sm">' + (ev.title || 'Untitled') + '</h4>'
            + '</div>'
            + '<div class="flex items-center gap-2 flex-wrap mb-1">'
            + '<p class="text-xs text-slate-500">' + timeStr + ' • ' + typeLabel + '</p>'
            + deptBadge
            + '</div>';

        if (ev.description) {
            html += '<p class="text-xs text-slate-600 dark:text-slate-400 mt-2">' + ev.description + '</p>';
        }

        html += '</div>'
            + '<div class="flex items-center gap-1 shrink-0">'
            + '<button onclick="editEventFromDayDetail(' + ev.id + ')" '
            + 'class="p-1.5 hover:bg-slate-200 dark:hover:bg-slate-600 rounded-lg transition-colors" title="Edit">'
            + '<span class="material-symbols-outlined text-[18px] text-slate-600 dark:text-slate-400">edit</span>'
            + '</button>'
            + '<button onclick="deleteEventFromDayDetail(' + ev.id + ', \'' + (ev.title || 'Untitled').replace(/'/g, "\\'") + '\')" '
            + 'class="p-1.5 hover:bg-rose-100 dark:hover:bg-rose-900/30 rounded-lg transition-colors" title="Delete">'
            + '<span class="material-symbols-outlined text-[18px] text-rose-500">delete</span>'
            + '</button>'
            + '</div>'
            + '</div>'
            + '</div>';
    });

    container.innerHTML = html;
    if (window.hydrateAppIcons) window.hydrateAppIcons(container);
};

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    loadDepartments();
});

// ══════════════════════════════════════════════════════════════════════════
// DEPARTMENT FILTER FUNCTIONALITY
// ══════════════════════════════════════════════════════════════════════════

var currentFilter = 'all';
var originalEvents = []; // Store original events

/**
 * Filter calendar by department
 */
function filterByDepartment(filterValue) {
    currentFilter = filterValue;
    
    // Store original events if not already stored
    if (originalEvents.length === 0 && window.calendarEvents) {
        originalEvents = window.calendarEvents.slice();
    }
    
    var filteredEvents = [];
    
    if (filterValue === 'all') {
        // Show all events
        filteredEvents = originalEvents.slice();
    } else if (filterValue === 'company-wide') {
        // Show only company-wide events
        filteredEvents = originalEvents.filter(function(e) {
            return e.isCompanyWide === true;
        });
    } else {
        // Show events for specific department
        var deptId = filterValue;
        filteredEvents = originalEvents.filter(function(e) {
            // Show if: company-wide OR primary dept matches OR in assigned departments
            if (e.isCompanyWide) return true;
            if (e.departmentId && e.departmentId == deptId) return true;
            if (e.assignedDepartments && e.assignedDepartments.includes('"' + deptId + '"')) return true;
            return false;
        });
    }
    
    // Update global calendarEvents and rebuild grid
    if (window.calendarEvents !== undefined) {
        window.calendarEvents = filteredEvents;
    }
    
    // Rebuild calendar grid if function exists
    if (typeof window.buildMonthGrid === 'function') {
        window.buildMonthGrid();
    }
    
    // Update filter info
    updateFilterInfo(filterValue, filteredEvents.length);
}

/**
 * Update filter info display
 */
function updateFilterInfo(filterValue, count) {
    var filterName = 'All Departments';
    
    if (filterValue === 'company-wide') {
        filterName = 'Company-wide Events';
    } else if (filterValue !== 'all') {
        var dept = departments.find(function(d) { return d.id == filterValue; });
        if (dept) filterName = dept.name;
    }
    
    console.log('Filter: ' + filterName + ' (' + count + ' events)');
}

/**
 * Clear filter and show all events
 */
function clearFilter() {
    document.getElementById('departmentFilter').value = 'all';
    filterByDepartment('all');
}
