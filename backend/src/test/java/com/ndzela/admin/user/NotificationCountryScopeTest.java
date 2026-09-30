package com.ndzela.admin.user;

import com.ndzela.admin.security.LoginAttemptService;
import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Alerts about one country's staff reach that country and the global admins only. */
class NotificationCountryScopeTest extends AbstractIntegrationTest {

    @Autowired private LoginAttemptService loginAttempts;

    private User gabonAdmin;
    private User maliAdmin;
    private User gabonViewer;
    private User root;
    private User otherRoot;

    @BeforeEach
    void people() {
        gabonAdmin = staff("ga.admin@ndzela.test", "GA", "ADMIN");
        maliAdmin = staff("ml.admin@ndzela.test", "ML", "ADMIN");
        gabonViewer = staff("ga.viewer@ndzela.test", "GA", "VIEWER");
        root = staff("root@ndzela.test", null, "SUPER_ADMIN");
        otherRoot = staff("root2@ndzela.test", null, "SUPER_ADMIN");
    }

    private int notificationsOf(User user) {
        return jdbc.queryForObject("select count(*) from notifications where user_id = ?", Integer.class, user.getId());
    }

    private void failFiveTimes(User user) {
        for (int i = 0; i < 5; i++) {
            loginAttempts.loginFailed(user.getEmail());
        }
    }

    @Test
    void lockingACountryUserAlertsThatCountryAndTheGlobalAdmins() {
        failFiveTimes(gabonViewer);

        assertEquals(1, notificationsOf(gabonAdmin));
        assertEquals(1, notificationsOf(root));
        assertEquals(1, notificationsOf(otherRoot));
        assertEquals(0, notificationsOf(maliAdmin));
    }

    @Test
    void lockingAGlobalUserAlertsOnlyGlobalAdmins() {
        failFiveTimes(root);

        assertEquals(1, notificationsOf(otherRoot));
        assertEquals(0, notificationsOf(gabonAdmin));
        assertEquals(0, notificationsOf(maliAdmin));
    }
}
