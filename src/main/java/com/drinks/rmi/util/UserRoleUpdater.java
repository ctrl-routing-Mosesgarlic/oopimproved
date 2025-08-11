package com.drinks.rmi.util;

import com.drinks.rmi.dto.UserDTO;
import com.drinks.rmi.interfaces.AuthService;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRoleUpdater {
    private static final Logger logger = LoggerFactory.getLogger(UserRoleUpdater.class);
    
    public static Task<Integer> createUpdateTask(AuthService authService, UserDTO currentUser, 
                                              String fromRole, String toRole) {
        return new Task<>() {
            @Override
            protected Integer call() throws Exception {
                try {
                    updateMessage("Updating user roles from " + fromRole + " to " + toRole);
                    int count = authService.updateUserRoles(fromRole, toRole, currentUser);
                    updateMessage("Updated " + count + " user records");
                    return count;
                } catch (Exception e) {
                    logger.error("Failed to update user roles", e);
                    updateMessage("Error: " + e.getMessage());
                    throw e;
                }
            }
        };
    }
}
