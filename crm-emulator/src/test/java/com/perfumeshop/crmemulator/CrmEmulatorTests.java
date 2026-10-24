package com.perfumeshop.crmemulator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CrmEmulatorTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmulatorStore store;

    @BeforeEach
    void reset() {
        store.clear();
    }

    @Test
    void healthIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    void bitrix24WebhookReturnsResultId() throws Exception {
        mockMvc.perform(post("/rest/1/test/crm.contact.add.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fields\":{\"NAME\":\"alice\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", is(1)));

        mockMvc.perform(post("/rest/1/test/crm.deal.add.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fields\":{\"TITLE\":\"Order #9\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", is(2)));

        mockMvc.perform(post("/rest/1/test/crm.timeline.comment.add.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fields\":{\"COMMENT\":\"Favorite: Rose\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", is(3)));
    }

    @Test
    void amoCrmRequiresBearerAndReturnsEmbeddedIds() throws Exception {
        mockMvc.perform(post("/api/v4/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"bob\"}]"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v4/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token")
                        .content("[{\"name\":\"bob\"}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.contacts[0].id", is(1)));

        mockMvc.perform(post("/api/v4/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token")
                        .content("[{\"name\":\"Order #5\",\"price\":1200}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.leads[0].id", is(2)));

        mockMvc.perform(post("/api/v4/contacts/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token")
                        .content("[{\"note_type\":\"common\",\"params\":{\"text\":\"Favorite: Oud\"}}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.notes[0].id", is(3)));
    }

    @Test
    void inspectApiListsAndClearsEvents() throws Exception {
        mockMvc.perform(post("/rest/1/test/crm.contact.add.json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fields\":{\"NAME\":\"carol\"}}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/emulator/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].provider", is("BITRIX24")))
                .andExpect(jsonPath("$[0].method", is("crm.contact.add")));

        mockMvc.perform(get("/emulator/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCount", is(1)));

        mockMvc.perform(delete("/emulator/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cleared", is(true)));

        mockMvc.perform(get("/emulator/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
