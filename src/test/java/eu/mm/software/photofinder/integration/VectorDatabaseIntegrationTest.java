package eu.mm.software.photofinder.integration;

import eu.mm.software.photofinder.photosattribute.application.query.VectorQuery;
import eu.mm.software.photofinder.photosattribute.interfaces.rest.VectorQueryController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the real semantic-search endpoint {@link VectorQueryController} exposes at
 * {@code GET /rest/api/v1/photos/search}.
 */
@ExtendWith(MockitoExtension.class)
class VectorDatabaseIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private VectorQuery vectorQuery;

    @BeforeEach
    void setUp() {
        VectorQueryController controller = new VectorQueryController(vectorQuery);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void search_returnsMatchingPhotoIds_andTotalCountHeader() throws Exception {
        when(vectorQuery.search("zachod slonca", null, null, null, 25))
                .thenReturn(List.of("photo-123", "photo-456"));

        mockMvc.perform(get("/rest/api/v1/photos/search")
                        .param("prompt", "zachod slonca")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$", containsInAnyOrder("photo-123", "photo-456")));
    }

    @Test
    void search_withCustomLimitProviderAndModel_passesParamsThrough() throws Exception {
        when(vectorQuery.search("cat", "OPENAI", "gpt-4o", "/photos", 5))
                .thenReturn(List.of("photo-1"));

        mockMvc.perform(get("/rest/api/v1/photos/search")
                        .param("prompt", "cat")
                        .param("provider", "OPENAI")
                        .param("model", "gpt-4o")
                        .param("path", "/photos")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$[0]", is("photo-1")));
    }
}
