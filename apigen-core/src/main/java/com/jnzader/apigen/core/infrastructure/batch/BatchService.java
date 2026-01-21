package com.jnzader.apigen.core.infrastructure.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnzader.apigen.core.infrastructure.batch.BatchRequest.BatchOperation;
import com.jnzader.apigen.core.infrastructure.batch.BatchResponse.BatchSummary;
import com.jnzader.apigen.core.infrastructure.batch.BatchResponse.OperationResult;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Service for processing batch API operations.
 *
 * <p>Executes multiple API operations in a single HTTP request by internally dispatching each
 * operation through Spring's DispatcherServlet.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Sequential or parallel execution
 *   <li>Stop-on-error support
 *   <li>Execution time tracking
 *   <li>Detailed per-operation results
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * BatchRequest request = new BatchRequest(List.of(
 *     new BatchOperation("op1", "GET", "/api/users/1", null, null),
 *     new BatchOperation("op2", "POST", "/api/users", null, body)
 * ), false);
 *
 * BatchResponse response = batchService.processBatch(request, httpRequest);
 * }</pre>
 */
public class BatchService {

    private static final Logger log = LoggerFactory.getLogger(BatchService.class);

    // Header constants
    private static final String HEADER_LOCATION = "Location";
    private static final String HEADER_ETAG = "ETag";
    private static final String HEADER_LAST_MODIFIED = "Last-Modified";
    private static final String HEADER_CONTENT_LOCATION = "Content-Location";
    private static final String HEADER_CONTENT_TYPE = "content-type";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private final DispatcherServlet dispatcherServlet;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;

    public BatchService(DispatcherServlet dispatcherServlet, ObjectMapper objectMapper) {
        this.dispatcherServlet = dispatcherServlet;
        this.objectMapper = objectMapper;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Processes a batch request sequentially.
     *
     * @param batchRequest the batch request containing operations
     * @param originalRequest the original HTTP request for context
     * @return the batch response with results for each operation
     */
    public BatchResponse processBatch(
            BatchRequest batchRequest, HttpServletRequest originalRequest) {
        long startTime = System.currentTimeMillis();
        List<OperationResult> results = new ArrayList<>();

        log.debug(
                "Processing batch with {} operations, stopOnError={}",
                batchRequest.operations().size(),
                batchRequest.stopOnError());

        for (BatchOperation operation : batchRequest.operations()) {
            OperationResult result = executeOperation(operation, originalRequest);
            results.add(result);

            if (batchRequest.stopOnError() && !result.isSuccessful()) {
                log.debug(
                        "Stopping batch execution due to error in operation {}",
                        operation.id() != null ? operation.id() : "unknown");
                break;
            }
        }

        long executionTime = System.currentTimeMillis() - startTime;
        BatchSummary summary = BatchSummary.fromResults(results);

        log.info(
                "Batch completed: {} operations ({} successful, {} failed) in {}ms",
                summary.total(),
                summary.successful(),
                summary.failed(),
                executionTime);

        return new BatchResponse(results, summary, executionTime);
    }

    /**
     * Processes a batch request in parallel.
     *
     * <p>Note: When using parallel execution, stopOnError is ignored.
     *
     * @param batchRequest the batch request containing operations
     * @param originalRequest the original HTTP request for context
     * @return the batch response with results for each operation
     */
    public BatchResponse processBatchParallel(
            BatchRequest batchRequest, HttpServletRequest originalRequest) {
        long startTime = System.currentTimeMillis();

        log.debug(
                "Processing batch in parallel with {} operations",
                batchRequest.operations().size());

        List<CompletableFuture<OperationResult>> futures =
                batchRequest.operations().stream()
                        .map(
                                op ->
                                        CompletableFuture.supplyAsync(
                                                () -> executeOperation(op, originalRequest),
                                                executor))
                        .toList();

        List<OperationResult> results = futures.stream().map(CompletableFuture::join).toList();

        long executionTime = System.currentTimeMillis() - startTime;
        BatchSummary summary = BatchSummary.fromResults(results);

        log.info(
                "Parallel batch completed: {} operations ({} successful, {} failed) in {}ms",
                summary.total(),
                summary.successful(),
                summary.failed(),
                executionTime);

        return new BatchResponse(results, summary, executionTime);
    }

    /**
     * Executes a single batch operation.
     *
     * @param operation the operation to execute
     * @param originalRequest the original request for context (headers, session, etc.)
     * @return the operation result
     */
    private OperationResult executeOperation(
            BatchOperation operation, HttpServletRequest originalRequest) {
        String operationId = operation.id();

        try {
            InternalHttpRequest internalRequest = createInternalRequest(operation, originalRequest);
            InternalHttpResponse internalResponse = new InternalHttpResponse();

            log.trace(
                    "Executing operation {} {} {}",
                    operationId,
                    operation.method(),
                    operation.path());

            dispatcherServlet.service(internalRequest, internalResponse);

            return createSuccessResult(operationId, internalResponse);
        } catch (Exception e) {
            log.warn(
                    "Error executing operation {}: {} - {}",
                    operationId,
                    operation.method(),
                    operation.path(),
                    e);
            return OperationResult.failure(
                    operationId,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal error executing operation",
                    e.getMessage());
        }
    }

    /**
     * Creates an internal HTTP request for the batch operation.
     *
     * @param operation the batch operation
     * @param originalRequest the original request for context
     * @return the internal request
     */
    private InternalHttpRequest createInternalRequest(
            BatchOperation operation, HttpServletRequest originalRequest) throws IOException {
        byte[] bodyBytes = null;
        if (operation.body() != null) {
            bodyBytes = objectMapper.writeValueAsBytes(operation.body());
        }

        return new InternalHttpRequest(
                operation.method(),
                operation.path(),
                originalRequest,
                operation.headers(),
                bodyBytes);
    }

    /**
     * Creates a successful operation result from the internal response.
     *
     * @param operationId the operation ID
     * @param response the internal response
     * @return the operation result
     */
    private OperationResult createSuccessResult(String operationId, InternalHttpResponse response) {
        int status = response.getStatus();
        JsonNode body = parseResponseBody(response);
        Map<String, String> headers = extractResponseHeaders(response);

        if (status >= 400) {
            String errorMessage = extractErrorMessage(body);
            return OperationResult.failure(operationId, status, errorMessage);
        }

        return OperationResult.success(
                operationId, status, headers.isEmpty() ? null : headers, body);
    }

    /**
     * Parses the response body as JSON.
     *
     * @param response the internal response
     * @return the parsed JSON body, or null if empty or not JSON
     */
    private JsonNode parseResponseBody(InternalHttpResponse response) {
        try {
            String content = response.getContentAsString();
            if (content == null || content.isBlank()) {
                return null;
            }

            String contentType = response.getContentType();
            if (contentType != null && contentType.contains(CONTENT_TYPE_JSON)) {
                return objectMapper.readTree(content);
            }

            // Return as text node for non-JSON responses
            return objectMapper.getNodeFactory().textNode(content);
        } catch (Exception e) {
            log.trace("Could not parse response body as JSON", e);
            return null;
        }
    }

    /**
     * Extracts relevant headers from the response.
     *
     * @param response the internal response
     * @return map of header names to values
     */
    private Map<String, String> extractResponseHeaders(InternalHttpResponse response) {
        Map<String, String> headers = new HashMap<>();

        for (String headerName : response.getHeaderNames()) {
            // Only include relevant headers in the batch response
            if (HEADER_LOCATION.equalsIgnoreCase(headerName)
                    || HEADER_ETAG.equalsIgnoreCase(headerName)
                    || HEADER_LAST_MODIFIED.equalsIgnoreCase(headerName)
                    || HEADER_CONTENT_LOCATION.equalsIgnoreCase(headerName)) {
                headers.put(headerName, response.getHeader(headerName));
            }
        }

        return headers;
    }

    /**
     * Extracts an error message from the response body.
     *
     * @param body the response body
     * @return the error message
     */
    private String extractErrorMessage(JsonNode body) {
        if (body == null) {
            return "Unknown error";
        }

        // Try common error message fields
        if (body.has("message")) {
            return body.get("message").asText();
        }
        if (body.has("detail")) {
            return body.get("detail").asText();
        }
        if (body.has("error")) {
            return body.get("error").asText();
        }
        if (body.has("title")) {
            return body.get("title").asText();
        }

        return "Request failed";
    }

    // ========== Internal Request/Response implementations ==========

    /**
     * Internal HTTP request implementation for batch operations. Wraps the original request and
     * overrides method, path, headers, and body.
     */
    private static class InternalHttpRequest implements HttpServletRequest {
        private final String method;
        private final String path;
        private final HttpServletRequest original;
        private final Map<String, String> customHeaders;
        private final byte[] body;
        private final Map<String, List<String>> mergedHeaders;

        InternalHttpRequest(
                String method,
                String path,
                HttpServletRequest original,
                Map<String, String> customHeaders,
                byte[] body) {
            this.method = method;
            this.path = path;
            this.original = original;
            this.customHeaders = customHeaders != null ? customHeaders : Map.of();
            this.body = body;
            this.mergedHeaders = mergeHeaders();
        }

        private Map<String, List<String>> mergeHeaders() {
            Map<String, List<String>> headers = new HashMap<>();

            // Copy original headers
            Enumeration<String> headerNames = original.getHeaderNames();
            while (headerNames != null && headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                List<String> values = new ArrayList<>();
                Enumeration<String> valueEnum = original.getHeaders(name);
                while (valueEnum.hasMoreElements()) {
                    values.add(valueEnum.nextElement());
                }
                headers.put(name.toLowerCase(), values);
            }

            // Override with custom headers
            customHeaders.forEach((k, v) -> headers.put(k.toLowerCase(), List.of(v)));

            // Set content type for body
            if (body != null) {
                headers.computeIfAbsent(HEADER_CONTENT_TYPE, k -> List.of(CONTENT_TYPE_JSON));
            }

            return headers;
        }

        @Override
        public String getMethod() {
            return method;
        }

        @Override
        public String getRequestURI() {
            return path;
        }

        @Override
        @SuppressWarnings("java:S4144") // Intentionally same as getRequestURI for batch operations
        public String getServletPath() {
            return path;
        }

        @Override
        public String getPathInfo() {
            return null;
        }

        @Override
        public String getHeader(String name) {
            List<String> values = mergedHeaders.get(name.toLowerCase());
            return values != null && !values.isEmpty() ? values.getFirst() : null;
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            List<String> values = mergedHeaders.get(name.toLowerCase());
            return Collections.enumeration(values != null ? values : List.of());
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return Collections.enumeration(mergedHeaders.keySet());
        }

        @Override
        public int getContentLength() {
            return body != null ? body.length : 0;
        }

        @Override
        public long getContentLengthLong() {
            return body != null ? body.length : 0;
        }

        @Override
        public String getContentType() {
            return getHeader(HEADER_CONTENT_TYPE);
        }

        @Override
        public jakarta.servlet.ServletInputStream getInputStream() {
            if (body == null) {
                return new EmptyServletInputStream();
            }
            return new ByteArrayServletInputStream(body);
        }

        @Override
        public Principal getUserPrincipal() {
            return original.getUserPrincipal();
        }

        // Delegate all other methods to original request
        @Override
        public String getAuthType() {
            return original.getAuthType();
        }

        @Override
        public Cookie[] getCookies() {
            return original.getCookies();
        }

        @Override
        public long getDateHeader(String name) {
            return original.getDateHeader(name);
        }

        @Override
        public int getIntHeader(String name) {
            String value = getHeader(name);
            return value != null ? Integer.parseInt(value) : -1;
        }

        @Override
        public String getPathTranslated() {
            return null;
        }

        @Override
        public String getContextPath() {
            return original.getContextPath();
        }

        @Override
        public String getQueryString() {
            int queryIndex = path.indexOf('?');
            return queryIndex >= 0 ? path.substring(queryIndex + 1) : null;
        }

        @Override
        public String getRemoteUser() {
            return original.getRemoteUser();
        }

        @Override
        public boolean isUserInRole(String role) {
            return original.isUserInRole(role);
        }

        @Override
        @SuppressWarnings("java:S2254") // Not exposing session ID in internal batch operations
        public String getRequestedSessionId() {
            // Return null for security - batch operations don't need session ID exposure
            return null;
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = new StringBuffer();
            url.append(original.getScheme()).append("://");
            url.append(original.getServerName());
            if (original.getServerPort() != 80 && original.getServerPort() != 443) {
                url.append(":").append(original.getServerPort());
            }
            url.append(path);
            return url;
        }

        @Override
        public jakarta.servlet.http.HttpSession getSession(boolean create) {
            return original.getSession(create);
        }

        @Override
        public jakarta.servlet.http.HttpSession getSession() {
            return original.getSession();
        }

        @Override
        public String changeSessionId() {
            return original.changeSessionId();
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            return original.isRequestedSessionIdValid();
        }

        @Override
        public boolean isRequestedSessionIdFromCookie() {
            return original.isRequestedSessionIdFromCookie();
        }

        @Override
        public boolean isRequestedSessionIdFromURL() {
            return original.isRequestedSessionIdFromURL();
        }

        @Override
        public boolean authenticate(HttpServletResponse response)
                throws IOException, jakarta.servlet.ServletException {
            return original.authenticate(response);
        }

        @Override
        public void login(String username, String password) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void logout() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<jakarta.servlet.http.Part> getParts() {
            return List.of();
        }

        @Override
        public jakarta.servlet.http.Part getPart(String name) {
            return null;
        }

        @Override
        public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(
                Class<T> handlerClass) {
            throw new UnsupportedOperationException();
        }

        // ServletRequest methods
        @Override
        public Object getAttribute(String name) {
            return original.getAttribute(name);
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            return original.getAttributeNames();
        }

        @Override
        public String getCharacterEncoding() {
            return StandardCharsets.UTF_8.name();
        }

        @Override
        public void setCharacterEncoding(String env) {
            // No-op
        }

        @Override
        public java.io.BufferedReader getReader() {
            return new java.io.BufferedReader(
                    new java.io.InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public String getParameter(String name) {
            return original.getParameter(name);
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return original.getParameterNames();
        }

        @Override
        public String[] getParameterValues(String name) {
            return original.getParameterValues(name);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return original.getParameterMap();
        }

        @Override
        public String getProtocol() {
            return original.getProtocol();
        }

        @Override
        public String getScheme() {
            return original.getScheme();
        }

        @Override
        public String getServerName() {
            return original.getServerName();
        }

        @Override
        public int getServerPort() {
            return original.getServerPort();
        }

        @Override
        public String getRemoteAddr() {
            return original.getRemoteAddr();
        }

        @Override
        public String getRemoteHost() {
            return original.getRemoteHost();
        }

        @Override
        public void setAttribute(String name, Object o) {
            original.setAttribute(name, o);
        }

        @Override
        public void removeAttribute(String name) {
            original.removeAttribute(name);
        }

        @Override
        public Locale getLocale() {
            return original.getLocale();
        }

        @Override
        public Enumeration<Locale> getLocales() {
            return original.getLocales();
        }

        @Override
        public boolean isSecure() {
            return original.isSecure();
        }

        @Override
        public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path) {
            return original.getRequestDispatcher(path);
        }

        @Override
        public int getRemotePort() {
            return original.getRemotePort();
        }

        @Override
        public String getLocalName() {
            return original.getLocalName();
        }

        @Override
        public String getLocalAddr() {
            return original.getLocalAddr();
        }

        @Override
        public int getLocalPort() {
            return original.getLocalPort();
        }

        @Override
        public jakarta.servlet.ServletContext getServletContext() {
            return original.getServletContext();
        }

        @Override
        public jakarta.servlet.AsyncContext startAsync() {
            throw new UnsupportedOperationException("Async not supported in batch operations");
        }

        @Override
        public jakarta.servlet.AsyncContext startAsync(
                jakarta.servlet.ServletRequest servletRequest,
                jakarta.servlet.ServletResponse servletResponse) {
            throw new UnsupportedOperationException("Async not supported in batch operations");
        }

        @Override
        public boolean isAsyncStarted() {
            return false;
        }

        @Override
        public boolean isAsyncSupported() {
            return false;
        }

        @Override
        public jakarta.servlet.AsyncContext getAsyncContext() {
            throw new IllegalStateException("Async not started");
        }

        @Override
        public jakarta.servlet.DispatcherType getDispatcherType() {
            return jakarta.servlet.DispatcherType.REQUEST;
        }

        @Override
        public String getRequestId() {
            return original.getRequestId();
        }

        @Override
        public String getProtocolRequestId() {
            return original.getProtocolRequestId();
        }

        @Override
        public jakarta.servlet.ServletConnection getServletConnection() {
            return original.getServletConnection();
        }
    }

    /** Empty servlet input stream. */
    private static class EmptyServletInputStream extends jakarta.servlet.ServletInputStream {
        @Override
        public int read() {
            return -1;
        }

        @Override
        public boolean isFinished() {
            return true;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(jakarta.servlet.ReadListener readListener) {
            // No-op
        }
    }

    /** Byte array servlet input stream. */
    private static class ByteArrayServletInputStream extends jakarta.servlet.ServletInputStream {
        private final java.io.ByteArrayInputStream delegate;

        ByteArrayServletInputStream(byte[] data) {
            this.delegate = new java.io.ByteArrayInputStream(data);
        }

        @Override
        public int read() {
            return delegate.read();
        }

        @Override
        public boolean isFinished() {
            return delegate.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(jakarta.servlet.ReadListener readListener) {
            // No-op for synchronous operations
        }
    }

    /** Internal HTTP response implementation for capturing batch operation results. */
    private static class InternalHttpResponse implements HttpServletResponse {
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private final StringWriter stringWriter = new StringWriter();
        private final Map<String, List<String>> headers = new HashMap<>();
        private int status = 200;
        private String contentType;
        private String characterEncoding = StandardCharsets.UTF_8.name();
        private boolean committed = false;
        private PrintWriter writer;

        String getContentAsString() {
            if (writer != null) {
                writer.flush();
                return stringWriter.toString();
            }
            return outputStream.toString(StandardCharsets.UTF_8);
        }

        @Override
        public Collection<String> getHeaderNames() {
            return headers.keySet();
        }

        @Override
        public void addCookie(Cookie cookie) {
            // No-op for internal response
        }

        @Override
        public boolean containsHeader(String name) {
            return headers.containsKey(name.toLowerCase());
        }

        @Override
        public String encodeURL(String url) {
            return url;
        }

        @Override
        public String encodeRedirectURL(String url) {
            return url;
        }

        @Override
        public void sendError(int sc, String msg) {
            this.status = sc;
            this.committed = true;
        }

        @Override
        public void sendError(int sc) {
            this.status = sc;
            this.committed = true;
        }

        @Override
        public void sendRedirect(String location) {
            setStatus(302);
            setHeader(HEADER_LOCATION, location);
            this.committed = true;
        }

        @Override
        public void sendRedirect(String location, int sc, boolean clearBuffer) {
            setStatus(sc);
            setHeader(HEADER_LOCATION, location);
            if (clearBuffer) {
                resetBuffer();
            }
            this.committed = true;
        }

        @Override
        public void setDateHeader(String name, long date) {
            setHeader(name, String.valueOf(date));
        }

        @Override
        public void addDateHeader(String name, long date) {
            addHeader(name, String.valueOf(date));
        }

        @Override
        public void setHeader(String name, String value) {
            headers.put(name.toLowerCase(), new ArrayList<>(List.of(value)));
        }

        @Override
        public void addHeader(String name, String value) {
            headers.computeIfAbsent(name.toLowerCase(), k -> new ArrayList<>()).add(value);
        }

        @Override
        public void setIntHeader(String name, int value) {
            setHeader(name, String.valueOf(value));
        }

        @Override
        public void addIntHeader(String name, int value) {
            addHeader(name, String.valueOf(value));
        }

        @Override
        public void setStatus(int sc) {
            this.status = sc;
        }

        @Override
        public int getStatus() {
            return status;
        }

        @Override
        public String getHeader(String name) {
            List<String> values = headers.get(name.toLowerCase());
            return values != null && !values.isEmpty() ? values.getFirst() : null;
        }

        @Override
        public Collection<String> getHeaders(String name) {
            return headers.getOrDefault(name.toLowerCase(), List.of());
        }

        @Override
        public String getCharacterEncoding() {
            return characterEncoding;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return new ServletOutputStream() {
                @Override
                public void write(int b) {
                    outputStream.write(b);
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                    // No-op
                }
            };
        }

        @Override
        public PrintWriter getWriter() {
            if (writer == null) {
                writer = new PrintWriter(stringWriter);
            }
            return writer;
        }

        @Override
        public void setCharacterEncoding(String charset) {
            this.characterEncoding = charset;
        }

        @Override
        public void setContentLength(int len) {
            setHeader("Content-Length", String.valueOf(len));
        }

        @Override
        public void setContentLengthLong(long len) {
            setHeader("Content-Length", String.valueOf(len));
        }

        @Override
        public void setContentType(String type) {
            this.contentType = type;
            setHeader("Content-Type", type);
        }

        @Override
        public void setBufferSize(int size) {
            // No-op
        }

        @Override
        public int getBufferSize() {
            return outputStream.size();
        }

        @Override
        public void flushBuffer() {
            this.committed = true;
        }

        @Override
        public void resetBuffer() {
            outputStream.reset();
            stringWriter.getBuffer().setLength(0);
        }

        @Override
        public boolean isCommitted() {
            return committed;
        }

        @Override
        public void reset() {
            if (!committed) {
                status = 200;
                headers.clear();
                resetBuffer();
            }
        }

        @Override
        public void setLocale(Locale loc) {
            // No-op
        }

        @Override
        public Locale getLocale() {
            return Locale.getDefault();
        }
    }
}
