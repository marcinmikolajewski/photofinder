package eu.mm.software.photofinder.photoparams.interfaces.rest;

import eu.mm.software.photofinder.photoparams.application.query.FileQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FileListControllerIT {

    private MockMvc mockMvc;

    @Mock
    private FileQuery fileQuery;

    @InjectMocks
    private FileListController controller;

    @Test
    void getAllFiles_returnsFilteredAndCountsHeader() throws Exception {
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        // given
        String path = "/some/path";
        Set<String> extensionsUpper = Set.of("JPG", "PNG");
        when(fileQuery.listAllFilesFromPath(path, extensionsUpper)).thenReturn(Set.of(
                "/some/path/a.JPG",
                "/some/path/b.PNG",
                "/some/path/c.GIF"
        ));

        // when & then
        mockMvc.perform(get("/rest/api/v1/file")
                        .param("path", path)
                        .param("extensions", "jpg", "png")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "3"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$", containsInAnyOrder(
                        "/some/path/a.JPG",
                        "/some/path/b.PNG",
                        "/some/path/c.GIF"
                )));
    }

    @Test
    void getAllFiles_onlyNew_filtersOutExisting() throws Exception {
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        String path = "/p";
        Set<String> extensionsUpper = Set.of("JPG");
        when(fileQuery.listAllFilesFromPath(path, extensionsUpper)).thenReturn(Set.of(
                "/p/new1.JPG",
                "/p/old1.JPG"
        ));
        when(fileQuery.findAllByLoggedUser()).thenReturn(Set.of(
                "/p/old1.JPG"
        ));

        mockMvc.perform(get("/rest/api/v1/file")
                        .param("path", path)
                        .param("extensions", "jpg")
                        .param("new", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]", is("/p/new1.JPG")));
    }
}
