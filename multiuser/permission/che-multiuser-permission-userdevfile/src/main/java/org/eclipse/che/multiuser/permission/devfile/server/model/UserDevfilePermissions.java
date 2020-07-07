package org.eclipse.che.multiuser.permission.devfile.server.model;

import java.util.List;

public interface UserDevfilePermissions {
    /** Returns user id */
    String getUserId();

    /** Returns user devfile id */
    String getUserDevfileId();

    /** Returns list of user devfile actions which can be performed by current user */
    List<String> getActions();
}
