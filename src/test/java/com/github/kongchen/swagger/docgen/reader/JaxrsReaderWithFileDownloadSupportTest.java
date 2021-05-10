package com.github.kongchen.swagger.docgen.reader;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.models.Operation;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import org.apache.maven.plugin.logging.Log;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class JaxrsReaderWithFileDownloadSupportTest {

    private final Swagger swagger = mock(Swagger.class);
    private final Log log = mock(Log.class);
    private JaxrsReaderWithFileDownloadSupport jaxrsReaderWithFileDownloadSupport;

    @BeforeMethod
    public void init() {
        jaxrsReaderWithFileDownloadSupport = new JaxrsReaderWithFileDownloadSupport(swagger, log);
    }

    @Test
    @ApiResponses({@ApiResponse(code = 0, message = "test", response = java.io.File.class)})
    public void updateApiResponseFileClass() throws Exception {
        // Given
        Operation operation = new Operation();

        // When
        ApiResponses apiResponses = getApiResponses("updateApiResponseFileClass");
        jaxrsReaderWithFileDownloadSupport.updateApiResponse(operation, apiResponses);

        // Then
        assertNotNull(operation.getResponses().get("default"));
        assertNotNull(operation.getResponses().get("default").getSchema());
    }

    private ApiResponses getApiResponses(String method) throws NoSuchMethodException {
        return this.getClass().getMethod(method).getAnnotation(ApiResponses.class);
    }

    @Test
    @ApiResponses({@ApiResponse(code = 1, message = "test")})
    public void updateApiResponseVoidClass() throws Exception {
        // Given
        Operation operation = new Operation();
        Response response = mock(Response.class);
        Property schema = mock(Property.class);
        when(response.getSchema()).thenReturn(schema);
        operation.setResponses(new HashMap<>());
        operation.getResponses().put("1", response);

        // When
        ApiResponses apiResponses = getApiResponses("updateApiResponseVoidClass");
        jaxrsReaderWithFileDownloadSupport.updateApiResponse(operation, apiResponses);

        // Then
        assertNotNull(operation.getResponses().get("1"));
        assertEquals(schema, operation.getResponses().get("1").getSchema());
    }

    @Test
    @ApiResponses({@ApiResponse(code = 2, message = "test", response = String.class)})
    public void updateApiResponseOtherClass() throws Exception {
        // Given
        Operation operation = new Operation();
        Response response = mock(Response.class);
        Property schema = mock(Property.class);
        when(response.getSchema()).thenReturn(schema);
        operation.setResponses(new HashMap<>());
        operation.getResponses().put("2", response);

        // When
        ApiResponses apiResponses = getApiResponses("updateApiResponseOtherClass");
        jaxrsReaderWithFileDownloadSupport.updateApiResponse(operation, apiResponses);

        // Then
        assertNotNull(operation.getResponses().get("2"));
        assertEquals(schema, operation.getResponses().get("2").getSchema());
    }
}
