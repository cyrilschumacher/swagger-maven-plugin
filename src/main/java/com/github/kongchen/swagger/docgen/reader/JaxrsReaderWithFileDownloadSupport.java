package com.github.kongchen.swagger.docgen.reader;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.converter.ModelConverters;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.FileProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import org.apache.maven.plugin.logging.Log;

import java.util.Map;

public class JaxrsReaderWithFileDownloadSupport extends JaxrsReader {

    public JaxrsReaderWithFileDownloadSupport(Swagger swagger, Log log) {
        super(swagger, log);
    }

    @Override
    protected void updateApiResponse(Operation operation, ApiResponses responseAnnotation) {
        for (ApiResponse apiResponse : responseAnnotation.value()) {
            Map<String, Property> responseHeaders = parseResponseHeaders(apiResponse.responseHeaders());
            Class<?> responseClass = apiResponse.response();
            Response response = new Response()
                    .description(apiResponse.message())
                    .headers(responseHeaders);

            // BEGIN custom patch
            if (responseClass.equals(java.io.File.class)) {
                response.schema(new FileProperty());
            }
            // END custom patch

            if (responseClass.equals(Void.class)) {
                voidResponseType(operation, apiResponse, response);
            } else {
                otherResponseTypes(operation, apiResponse, response);
            }

            if (apiResponse.code() == 0) {
                operation.defaultResponse(response);
            } else {
                operation.response(apiResponse.code(), response);
            }
        }
    }

    private void voidResponseType(Operation operation, ApiResponse apiResponse, Response response) {
        if (operation.getResponses() != null) {
            Response apiOperationResponse = operation.getResponses().get(String.valueOf(apiResponse.code()));
            if (apiOperationResponse != null) {
                response.schema(apiOperationResponse.getSchema());
            }
        }
    }

    private void otherResponseTypes(Operation operation, ApiResponse apiResponse, Response response) {
        Class<?> responseClass = apiResponse.response();
        Map<String, Model> models = ModelConverters.getInstance().read(responseClass);

        for (Map.Entry<String, Model> entry : models.entrySet()) {
            final Property schema = new RefProperty().asDefault(entry.getKey());
            if ("List".equals(apiResponse.responseContainer())) {
                response.schema(new ArrayProperty(schema));
            } else {
                response.schema(schema);
            }
            swagger.model(entry.getKey(), entry.getValue());
        }

        models = ModelConverters.getInstance().readAll(responseClass);

        for (Map.Entry<String, Model> entry : models.entrySet()) {
            swagger.model(entry.getKey(), entry.getValue());
        }

        if (response.getSchema() == null) {
            voidResponseType(operation, apiResponse, response);
        }
    }
}
