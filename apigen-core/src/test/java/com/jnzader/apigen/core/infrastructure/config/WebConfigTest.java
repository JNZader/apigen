package com.jnzader.apigen.core.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("WebConfig Tests")
@ExtendWith(MockitoExtension.class)
class WebConfigTest {

    @Mock
    private RequestIdInterceptor requestIdInterceptor;

    @Mock
    private InterceptorRegistry interceptorRegistry;

    @Mock
    private InterceptorRegistration interceptorRegistration;

    @Mock
    private ContentNegotiationConfigurer contentNegotiationConfigurer;

    private WebConfig webConfig;

    @BeforeEach
    void setUp() {
        webConfig = new WebConfig(requestIdInterceptor);
    }

    @Nested
    @DisplayName("addInterceptors")
    class AddInterceptorsTests {

        @Test
        @DisplayName("should register RequestIdInterceptor")
        void shouldRegisterRequestIdInterceptor() {
            when(interceptorRegistry.addInterceptor(requestIdInterceptor))
                    .thenReturn(interceptorRegistration);
            when(interceptorRegistration.addPathPatterns(any(String.class)))
                    .thenReturn(interceptorRegistration);

            webConfig.addInterceptors(interceptorRegistry);

            verify(interceptorRegistry).addInterceptor(requestIdInterceptor);
        }

        @Test
        @DisplayName("should configure interceptor for /api/** path pattern")
        void shouldConfigureInterceptorForApiPath() {
            when(interceptorRegistry.addInterceptor(requestIdInterceptor))
                    .thenReturn(interceptorRegistration);
            when(interceptorRegistration.addPathPatterns(any(String.class)))
                    .thenReturn(interceptorRegistration);

            webConfig.addInterceptors(interceptorRegistry);

            verify(interceptorRegistration).addPathPatterns("/api/**");
        }
    }

    @Nested
    @DisplayName("configureContentNegotiation")
    class ConfigureContentNegotiationTests {

        @Test
        @DisplayName("should disable parameter-based content type")
        void shouldDisableParameterBasedContentType() {
            when(contentNegotiationConfigurer.favorParameter(false))
                    .thenReturn(contentNegotiationConfigurer);
            when(contentNegotiationConfigurer.defaultContentType(any(MediaType.class)))
                    .thenReturn(contentNegotiationConfigurer);
            when(contentNegotiationConfigurer.mediaType(anyString(), any(MediaType.class)))
                    .thenReturn(contentNegotiationConfigurer);

            webConfig.configureContentNegotiation(contentNegotiationConfigurer);

            verify(contentNegotiationConfigurer).favorParameter(false);
        }

        @Test
        @DisplayName("should set default content type to JSON")
        void shouldSetDefaultContentTypeToJson() {
            when(contentNegotiationConfigurer.favorParameter(false))
                    .thenReturn(contentNegotiationConfigurer);
            when(contentNegotiationConfigurer.defaultContentType(any(MediaType.class)))
                    .thenReturn(contentNegotiationConfigurer);
            when(contentNegotiationConfigurer.mediaType(anyString(), any(MediaType.class)))
                    .thenReturn(contentNegotiationConfigurer);

            webConfig.configureContentNegotiation(contentNegotiationConfigurer);

            verify(contentNegotiationConfigurer).defaultContentType(MediaType.APPLICATION_JSON);
        }

        @Test
        @DisplayName("should configure JSON media type")
        void shouldConfigureJsonMediaType() {
            when(contentNegotiationConfigurer.favorParameter(false))
                    .thenReturn(contentNegotiationConfigurer);
            when(contentNegotiationConfigurer.defaultContentType(any(MediaType.class)))
                    .thenReturn(contentNegotiationConfigurer);
            when(contentNegotiationConfigurer.mediaType(anyString(), any(MediaType.class)))
                    .thenReturn(contentNegotiationConfigurer);

            webConfig.configureContentNegotiation(contentNegotiationConfigurer);

            verify(contentNegotiationConfigurer).mediaType("json", MediaType.APPLICATION_JSON);
        }

        @Test
        @DisplayName("should configure XML media type")
        void shouldConfigureXmlMediaType() {
            when(contentNegotiationConfigurer.favorParameter(false))
                    .thenReturn(contentNegotiationConfigurer);
            when(contentNegotiationConfigurer.defaultContentType(any(MediaType.class)))
                    .thenReturn(contentNegotiationConfigurer);
            when(contentNegotiationConfigurer.mediaType(anyString(), any(MediaType.class)))
                    .thenReturn(contentNegotiationConfigurer);

            webConfig.configureContentNegotiation(contentNegotiationConfigurer);

            verify(contentNegotiationConfigurer).mediaType("xml", MediaType.APPLICATION_XML);
        }
    }
}
