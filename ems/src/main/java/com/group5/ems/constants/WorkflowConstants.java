package com.group5.ems.constants;

public class WorkflowConstants {
    
    // Workflow Steps
    public static final String STEP_WAITING_DM = "WAITING_DM";
    public static final String STEP_WAITING_HR = "WAITING_HR";
    public static final String STEP_WAITING_HRM = "WAITING_HRM";
    public static final String STEP_COMPLETED = "COMPLETED";
    public static final String STEP_REJECTED = "REJECTED";
    public static final String STEP_CANCELLED = "CANCELLED";
    
    // Request Status
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    
    // User Roles
    public static final String ROLE_DEPT_MANAGER = "DEPT_MANAGER";
    public static final String ROLE_HR = "HR";
    public static final String ROLE_HR_MANAGER = "HR_MANAGER";
    
    private WorkflowConstants() {
        // Prevent instantiation
    }
}
