package com.zerotrust;

import com.zerotrust.controller.DocumentController;
import com.zerotrust.dto.DocumentResponseDTO;
import com.zerotrust.security.JwtFilter;
import com.zerotrust.security.SecurityConfiguration;
import com.zerotrust.security.ZeroTrustFilter;
import com.zerotrust.service.DocumentService;
import com.zerotrust.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DocumentController.class)
@AutoConfigureMockMvc
@Import(SecurityConfiguration.class)
public class ZeroTrustFilterTests {

    private static final String TOKEN = "test-token";
    private static final String USERNAME = "mihai_user";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private JwtService jwtService;

    @TestConfiguration
    static class FilterTestConfiguration {
        @Bean
        JwtFilter jwtFilter(JwtService jwtService, UserDetailsService userDetailsService) {
            return new JwtFilter(jwtService, userDetailsService);
        }

        @Bean
        ZeroTrustFilter zeroTrustFilter(JwtService jwtService, StringRedisTemplate redisTemplate) {
            return new ZeroTrustFilter(redisTemplate);
        }
    }

    private void stubJwtAuthentication() {
        UserDetails userDetails = User.withUsername(USERNAME)
                .password("password")
                .roles("USER")
                .build();

        Mockito.when(jwtService.extractUsername(TOKEN)).thenReturn(USERNAME);
        Mockito.when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
        Mockito.when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(true);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should return 403 Forbidden when Biometric Trust Score is missing")
    void shouldRejectMissingBiometrics() throws Exception {

        stubJwtAuthentication();
        Mockito.when(valueOperations.get("trust_score:" + USERNAME)).thenReturn(null);

        mockMvc.perform(get("/api/documents/1")
                        .header("Authorization", "Bearer " + TOKEN)
                        .accept(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when Biometric Trust Score is below threshold")
    void shouldRejectCompromisedScore() throws Exception {

        stubJwtAuthentication();
        Mockito.when(valueOperations.get("trust_score:" + USERNAME)).thenReturn("60");

        mockMvc.perform(get("/api/documents/1")
                        .header("Authorization", "Bearer " + TOKEN)
                        .accept(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow request when Trust Score is excellent (Genuine User)")
    void shouldAllowGenuineUser() throws Exception {

        stubJwtAuthentication();
        Mockito.when(valueOperations.get("trust_score:" + USERNAME)).thenReturn("95");

        DocumentResponseDTO dto = new DocumentResponseDTO();
        dto.setId(1L);
        dto.setContent("sensitive-data");
        documentServiceStubForSuccess(dto);

        mockMvc.perform(get("/api/documents/1")
                        .header("Authorization", "Bearer " + TOKEN)
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private void documentServiceStubForSuccess(DocumentResponseDTO dto) {
        Mockito.when(documentService.getById(1L)).thenReturn(dto);
    }
}
