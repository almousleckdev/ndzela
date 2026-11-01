package com.ndzela.admin.auth;

import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Forgot-password is OTP-code based for staff, matching the driver app: expiring, single-use, no account enumeration.
class PasswordResetTest extends AbstractIntegrationTest {
    // Each test uses its own email: the OTP resend cooldown is keyed by email and outlives resetData()'s truncation.
    private static final java.util.concurrent.atomic.AtomicInteger SEQUENCE = new java.util.concurrent.atomic.AtomicInteger();
    private final String email = "ga.reset" + SEQUENCE.incrementAndGet() + "@ndzela.test";

    @Autowired
    private com.ndzela.admin.auth.application.RefreshTokenService refreshTokens;

    private ResultActions forgot(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\"}"));
    }

    private ResultActions reset(String email, String code, String newPassword) throws Exception {
        return mvc.perform(post("/api/v1/auth/forgot-password/reset").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"code\":\"" + code + "\",\"newPassword\":\"" + newPassword + "\"}"));
    }

    private String code(String email) {
        return redis.opsForValue().get("auth:otp:staff-reset:" + email);
    }

    @Test
    void aForgottenPasswordIsResetWithTheCodeSentByEmailAndOldSessionsEnd() throws Exception {
        User user = staff(email, "GA", "ADMIN");
        String oldRefresh = refreshTokens.issue(email, null);
        String oldAccessToken = bearer(user);
        String oldHash = jdbc.queryForObject("select password from users where email = ?", String.class, email);

        forgot(email).andExpect(status().isOk());
        String code = code(email);
        assertNotNull(code);

        reset(email, code, "Nouveau123!").andExpect(status().isOk());

        assertThrows(com.ndzela.admin.common.exception.SessionExpiredException.class, () -> refreshTokens.rotate(oldRefresh));
        assertNotEquals(oldHash, jdbc.queryForObject("select password from users where email = ?", String.class, email));
        // A reset must also end any access token that was already live, not just the refresh chain.
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/users")
                        .header("Authorization", oldAccessToken))
                .andExpect(status().isUnauthorized());
        reset(email, code, "Autre12345!").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void theAnswerNeverRevealsWhetherTheAccountExists() throws Exception {
        staff(email, "GA", "ADMIN");
        forgot(email).andExpect(status().isOk());
        forgot("nobody@ndzela.test").andExpect(status().isOk());

        reset("nobody@ndzela.test", "123456", "Nouveau123!").andExpect(status().isUnprocessableEntity());
        assertNull(code("nobody@ndzela.test"));
    }

    @Test
    void aWrongCodeIsRefusedAndTooManyTriesKillTheCode() throws Exception {
        staff(email, "GA", "ADMIN");
        forgot(email).andExpect(status().isOk());
        String code = code(email);
        String wrong = code.equals("000000") ? "000001" : "000000";
        for (int i = 0; i < 3; i++) {
            reset(email, wrong, "Nouveau123!").andExpect(status().isUnprocessableEntity());
        }
        reset(email, code, "Nouveau123!").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void asksInQuickSuccessionSendOneCodeOnly() throws Exception {
        staff(email, "GA", "ADMIN");
        forgot(email).andExpect(status().isOk());
        String first = code(email);
        forgot(email).andExpect(status().isOk());
        assertEquals(first, code(email));
    }
}
