package eu.mm.software.photofinder.integration;

import eu.mm.software.photofinder.photoparams.application.command.FileApplicationService;
import eu.mm.software.photofinder.photoparams.interfaces.rest.DescribeFormDto;
import eu.mm.software.photofinder.photoparams.interfaces.rest.DescribeFormValidator;
import eu.mm.software.photofinder.photoparams.interfaces.rest.FileEditController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the real "describe photos from local disk" flow (there is no binary photo-upload
 * endpoint in this application) exposed by {@link FileEditController} at
 * {@code POST /rest/api/v1/file/describe}.
 */
@ExtendWith(MockitoExtension.class)
class FileDescribeIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private FileApplicationService fileApplicationService;

    @BeforeEach
    void setUp() {
        FileEditController controller = new FileEditController(fileApplicationService, new DescribeFormValidator());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void describePhotos_withValidRequest_isAcceptedAndDelegatesToService() throws Exception {
        String body = """
                {"path":"/photos/2026","extensions":["jpg","png"],"provider":"OPENAI"}
                """;

        mockMvc.perform(post("/rest/api/v1/file/describe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());

        ArgumentCaptor<DescribeFormDto> captor = ArgumentCaptor.forClass(DescribeFormDto.class);
        verify(fileApplicationService).describePhotos(captor.capture());
        DescribeFormDto sent = captor.getValue();
        assertThat(sent.getPath()).isEqualTo("/photos/2026");
        assertThat(sent.getProvider()).isEqualTo("OPENAI");
        assertThat(sent.getExtensions()).containsExactlyInAnyOrder("jpg", "png");
    }

    @Test
    void describePhotos_withUnknownProvider_isRejectedAndServiceIsNeverCalled() throws Exception {
        String body = """
                {"path":"/photos/2026","extensions":["jpg"],"provider":"NOT_A_PROVIDER"}
                """;

        mockMvc.perform(post("/rest/api/v1/file/describe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Provider not active"));

        verifyNoInteractions(fileApplicationService);
    }

    @Test
    void describePhotos_withUnsupportedExtension_isRejected() throws Exception {
        String body = """
                {"path":"/photos/2026","extensions":["gif"],"provider":"OPENAI"}
                """;

        mockMvc.perform(post("/rest/api/v1/file/describe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(fileApplicationService);
    }
}
