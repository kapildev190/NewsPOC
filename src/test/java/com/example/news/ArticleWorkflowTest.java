package com.example.news;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ArticleWorkflowTest {
    @Autowired MockMvc mvc;
    static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Test
    void fullWorkflow() throws Exception {
        // admin creates users
        mvc.perform(post("/api/admin/users").with(httpBasic("admin", "admin123")).contentType(JSON)
            .content("{\"username\":\"jane\",\"password\":\"secret1\",\"fullName\":\"Jane\",\"role\":\"JOURNALIST\"}"))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/admin/users").with(httpBasic("admin", "admin123")).contentType(JSON)
            .content("{\"username\":\"ed\",\"password\":\"secret1\",\"fullName\":\"Ed\",\"role\":\"EDITOR\"}"))
            .andExpect(status().isCreated());

        // journalist writes + submits
        String body = mvc.perform(post("/api/journalist/articles").with(httpBasic("jane", "secret1")).contentType(JSON)
            .content("{\"title\":\"Big News\",\"content\":\"Something happened\",\"category\":\"world\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        mvc.perform(post("/api/journalist/articles/" + id + "/submit").with(httpBasic("jane", "secret1")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_REVIEW"));

        // not visible to public yet
        mvc.perform(get("/api/public/articles")).andExpect(jsonPath("$.totalElements").value(0));

        // journalist cannot approve
        mvc.perform(post("/api/editor/articles/" + id + "/approve").with(httpBasic("jane", "secret1")))
            .andExpect(status().isForbidden());

        // editor approves -> visible
        mvc.perform(post("/api/editor/articles/" + id + "/approve").with(httpBasic("ed", "secret1")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(get("/api/public/articles")).andExpect(jsonPath("$.totalElements").value(1));

        // approved article can no longer be edited
        mvc.perform(put("/api/journalist/articles/" + id).with(httpBasic("jane", "secret1")).contentType(JSON)
            .content("{\"title\":\"x\",\"content\":\"y\"}")).andExpect(status().isConflict());
    }
}
