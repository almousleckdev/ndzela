package com.ndzela.admin.freecharging;

import com.ndzela.admin.freecharging.application.PartnerActor;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PartnerActorTest {
    private String named(String actor) {
        return PartnerActor.runAs(actor, () -> SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void lineAndParagraphSeparatorsAndFormatCharactersAreStripped() {
        assertEquals("zella:mireille", named("mire ille"));
        assertEquals("zella:mireille", named("mire ille"));
        assertEquals("zella:mireille", named("mire‮ille​"));
        assertEquals("zella:mireille", named("﻿mireille"));
    }

    @Test
    void anActorMadeOnlyOfInvisibleCharactersIsUnknown() {
        assertEquals("zella:unknown", named(" ​‮"));
        assertEquals("zella:unknown", named(null));
    }
}
