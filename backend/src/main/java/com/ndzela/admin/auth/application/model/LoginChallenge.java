package com.ndzela.admin.auth.application.model;

import com.ndzela.admin.auth.application.model.LoginStage;

public record LoginChallenge(String token, String email, LoginStage stage) {}
