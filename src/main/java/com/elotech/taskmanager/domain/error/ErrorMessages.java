package com.elotech.taskmanager.domain.error;

public final class ErrorMessages {

    private ErrorMessages() {
    }

    // Codes
    public static final String PROJECT_NOT_FOUND_CODE = "error.project.not-found";
    public static final String PROJECT_ADMIN_REQUIRED_CODE = "error.project.admin-required";
    public static final String PROJECT_NO_FIELDS_CODE = "error.project.no-fields";
    public static final String PROJECT_NAME_BLANK_CODE = "error.project.name-blank";

    public static final String PROJECT_MEMBER_EMPTY_LIST_CODE = "error.project-member.empty-list";
    public static final String PROJECT_MEMBER_DUPLICATE_USER_CODE = "error.project-member.duplicate-user";
    public static final String PROJECT_MEMBER_ALREADY_EXISTS_CODE = "error.project-member.already-exists";

    public static final String USER_NOT_FOUND_CODE = "error.user.not-found";

    public static final String TASK_NOT_FOUND_CODE = "error.task.not-found";
    public static final String TASK_NO_FIELDS_CODE = "error.task.no-fields";
    public static final String TASK_TITLE_BLANK_CODE = "error.task.title-blank";
    public static final String TASK_ASSIGNEE_NOT_MEMBER_CODE = "error.task.assignee-not-member";
    public static final String TASK_DONE_TO_TODO_CODE = "error.task.done-to-todo";
    public static final String TASK_CRITICAL_DONE_ADMIN_REQUIRED_CODE = "error.task.critical-done-admin-required";
    public static final String TASK_WIP_LIMIT_EXCEEDED_CODE = "error.task.wip-limit-exceeded";
    public static final String NOTIFICATION_NOT_FOUND_CODE = "error.notification.not-found";

    public static final String PAGINATION_PAGE_INVALID_CODE = "error.pagination.page-invalid";
    public static final String PAGINATION_SIZE_INVALID_CODE = "error.pagination.size-invalid";

    public static final String AUTH_EMAIL_IN_USE_CODE = "error.auth.email-in-use";
    public static final String AUTH_INVALID_CREDENTIALS_CODE = "error.auth.invalid-credentials";
    public static final String AUTH_REFRESH_EXPIRED_CODE = "error.auth.refresh-expired";
    public static final String AUTH_REFRESH_INVALID_CODE = "error.auth.refresh-invalid";
    public static final String AUTH_REFRESH_REVOKED_CODE = "error.auth.refresh-revoked";
    public static final String AUTH_UNAUTHENTICATED_CODE = "error.auth.unauthenticated";
    public static final String AUTH_FORBIDDEN_CODE = "error.auth.forbidden";
    public static final String AUTH_UNAUTHORIZED_CODE = "error.auth.unauthorized";
    public static final String AUTH_TOKEN_EXPIRED_CODE = "error.auth.token-expired";
    public static final String AUTH_TOKEN_INVALID_CODE = "error.auth.token-invalid";

    public static final String RESOURCE_NOT_FOUND_CODE = "error.resource.not-found";
    public static final String METHOD_NOT_ALLOWED_CODE = "error.method-not-allowed";
    public static final String REQUEST_PARAMETER_INVALID_CODE = "error.request.parameter-invalid";
    public static final String REQUEST_BODY_INVALID_CODE = "error.request.body-invalid";
    public static final String VALIDATION_FAILED_CODE = "error.validation.failed";
    public static final String INTERNAL_CODE = "error.internal";

    // Messages
    public static final String PROJECT_NOT_FOUND_MESSAGE = "Project not found";
    public static final String PROJECT_ADMIN_REQUIRED_MESSAGE = "Only project admins can perform this action";
    public static final String PROJECT_NO_FIELDS_MESSAGE = "At least one field must be provided";
    public static final String PROJECT_NAME_BLANK_MESSAGE = "Project name cannot be blank";

    public static final String PROJECT_MEMBER_EMPTY_LIST_MESSAGE = "At least one user must be provided";
    public static final String PROJECT_MEMBER_DUPLICATE_USER_MESSAGE = "Request contains duplicated users";
    public static final String PROJECT_MEMBER_ALREADY_EXISTS_MESSAGE = "User is already a project member";

    public static final String USER_NOT_FOUND_MESSAGE = "User not found";

    public static final String TASK_NOT_FOUND_MESSAGE = "Task not found";
    public static final String TASK_NO_FIELDS_MESSAGE = "At least one field must be provided";
    public static final String TASK_TITLE_BLANK_MESSAGE = "Task title cannot be blank";
    public static final String TASK_ASSIGNEE_NOT_MEMBER_MESSAGE = "Assignee must be a project member";
    public static final String TASK_DONE_TO_TODO_MESSAGE = "Done tasks cannot return to todo";
    public static final String TASK_CRITICAL_DONE_ADMIN_REQUIRED_MESSAGE = "Only project admins can complete critical tasks";
    public static final String TASK_WIP_LIMIT_EXCEEDED_MESSAGE = "Assignee has reached the in-progress task limit for this project";
    public static final String NOTIFICATION_NOT_FOUND_MESSAGE = "Notification not found";

    public static final String PAGINATION_PAGE_INVALID_MESSAGE = "Page must be greater than or equal to zero";
    public static final String PAGINATION_SIZE_INVALID_MESSAGE = "Size must be greater than zero";

    public static final String AUTH_EMAIL_IN_USE_MESSAGE = "Email already in use: ";
    public static final String AUTH_INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";
    public static final String AUTH_REFRESH_EXPIRED_MESSAGE = "Refresh token expired";
    public static final String AUTH_INVALID_REFRESH_TOKEN_MESSAGE = "Invalid refresh token";
    public static final String AUTH_UNAUTHENTICATED_MESSAGE = "User is not authenticated";
    public static final String AUTH_USER_NOT_FOUND_MESSAGE = "Authenticated user not found";
    public static final String AUTH_FORBIDDEN_MESSAGE = "You do not have permission to access this resource";
    public static final String AUTH_TOKEN_EXPIRED_MESSAGE = "Token expired";
    public static final String AUTH_TOKEN_INVALID_MESSAGE = "Invalid token";

    public static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found: ";
    public static final String METHOD_NOT_ALLOWED_MESSAGE = "HTTP method not allowed: ";
    public static final String REQUEST_PARAMETER_INVALID_MESSAGE = "Invalid value for parameter: ";
    public static final String REQUEST_BODY_INVALID_MESSAGE = "Request body is invalid or malformed";
    public static final String VALIDATION_FAILED_MESSAGE = "One or more fields are invalid";
    public static final String INTERNAL_MESSAGE = "An unexpected error occurred";
}
