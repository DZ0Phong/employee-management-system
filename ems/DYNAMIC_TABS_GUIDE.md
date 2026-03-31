# Dynamic Activity Tabs - Implementation Guide

## Overview
Activity tabs in HR Manager Dashboard are now dynamic - automatically generated from backend data.

## How to Add New Category

### 1. Update Service (HRManagerDashboardService.java)
Add new category in `getActivityCategories()` method:

```java
// New Category Example
long newCategoryCount = requestRepository.countByStatusAndRequestTypeCategory("PENDING", "NEW_TYPE");
categories.add(ActivityCategoryDTO.builder()
        .type("newtype")           // Unique identifier
        .label("New Category")     // Display name
        .icon("new_icon")          // Material icon
        .count(newCategoryCount)   // Count
        .color("red")              // Badge color
        .description("Description")
        .build());
```

### 2. Add Controller Endpoint (HrManagerController.java)
Add case in `getActivitiesByType()` method:

```java
case "newtype":
    activities = dashboardService.getNewTypeActivities(since);
    break;
```

### 3. Add Service Method
Implement the data fetching method:

```java
public List<RecentActivityDTO> getNewTypeActivities(LocalDateTime since) {
    // Fetch and return activities
}
```

### 4. Template (dashboard.html)
No changes needed! Tabs are auto-generated from `activityCategories`.

## Color Options
- blue, green, purple, orange, red, yellow, pink, indigo

## Icon Options
Use Material Symbols: https://fonts.google.com/icons
