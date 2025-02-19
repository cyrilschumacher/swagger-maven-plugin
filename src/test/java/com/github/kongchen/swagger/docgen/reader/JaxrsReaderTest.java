package com.github.kongchen.swagger.docgen.reader;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.swagger.models.ComposedModel;
import io.swagger.models.Model;
import io.swagger.models.properties.Property;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.util.Json;
import org.apache.maven.plugin.logging.Log;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.jaxrs.ext.SwaggerExtension;
import io.swagger.jaxrs.ext.SwaggerExtensions;
import io.swagger.models.ArrayModel;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.parameters.RefParameter;
import net.javacrumbs.jsonunit.JsonAssert;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class JaxrsReaderTest {
    @Mock
    private Log log;

    private JaxrsReader reader;

    List<SwaggerExtension> extensions = SwaggerExtensions.getExtensions();

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
        reader = new JaxrsReader(new Swagger(), log);
    }

    @AfterMethod
    public void resetExtenstions() {
        SwaggerExtensions.setExtensions(extensions);
    }

    @Test
    public void ignoreClassIfNoApiAnnotation() {
        Swagger result = reader.read(NotAnnotatedApi.class);

        assertEmptySwaggerResponse(result);
    }

    @Test
    public void ignoreApiIfHiddenAttributeIsTrue() {
        Swagger result = reader.read(HiddenApi.class);

        assertEmptySwaggerResponse(result);
    }

    @Test
    public void includeApiIfHiddenParameterIsTrueAndApiHiddenAttributeIsTrue() {
        Swagger result = reader.read(HiddenApi.class, "", null, true, new String[0], new String[0], new HashMap<String, Tag>(), new ArrayList<Parameter>());

        assertNotNull(result, "No Swagger object created");
        assertFalse(result.getTags().isEmpty(), "Should contain api tags");
        assertFalse(result.getPaths().isEmpty(), "Should contain operation paths");
    }

    @Test
    public void discoverApiOperation() {
        Tag expectedTag = new Tag();
        expectedTag.name("atag");
        Swagger result = reader.read(AnApi.class);

        assertSwaggerResponseContents(expectedTag, result);
    }

    @Test
    public void createNewSwaggerInstanceIfNoneProvided() {
        JaxrsReader nullReader = new JaxrsReader(null, log);
        Tag expectedTag = new Tag();
        expectedTag.name("atag");
        Swagger result = nullReader.read(AnApi.class);

        assertSwaggerResponseContents(expectedTag, result);
    }

    @Test
    public void handleOctetStreamAndByteArray() {
        Swagger result = reader.read(AnApiWithOctetStream.class);
        io.swagger.models.Path path = result.getPaths().get("/apath/add");
        assertNotNull(path, "Expecting to find a path ..");
        assertNotNull(path.getPost(), ".. with post opertion ..");
        assertNotNull(path.getPost().getConsumes().contains("application/octet-stream"), ".. and with octect-stream consumer.");
        assertTrue(path.getPost().getParameters().get(0) instanceof BodyParameter, "The parameter is a body parameter ..");
        assertFalse(((BodyParameter) path.getPost().getParameters().get(0)).getSchema() instanceof ArrayModel, " .. and the schema is NOT an ArrayModel");
    }

    private void assertEmptySwaggerResponse(Swagger result) {
        assertNotNull(result, "No Swagger object created");
        assertNull(result.getTags(), "Should not have any tags");
        assertNull(result.getPaths(), "Should not have any paths");
    }

    private void assertSwaggerResponseContents(Tag expectedTag, Swagger result) {
        assertNotNull(result, "No Swagger object created");
        assertFalse(result.getTags().isEmpty(), "Should contain api tags");
        assertTrue(result.getTags().contains(expectedTag), "Expected tag missing");
        assertFalse(result.getPaths().isEmpty(), "Should contain operation paths");
        assertTrue(result.getPaths().containsKey("/apath"), "Path missing from paths map");
        io.swagger.models.Path path = result.getPaths().get("/apath");
        assertFalse(path.getOperations().isEmpty(), "Should be a get operation");
    }

    @Test
    public void createCommonParameters() throws Exception {
        reader = new JaxrsReader(new Swagger(), Mockito.mock(Log.class));
        Swagger result = reader.read(CommonParametersApi.class);
        Parameter headerParam = result.getParameter("headerParam");
        assertTrue(headerParam instanceof HeaderParameter);
        Parameter queryParam = result.getParameter("queryParam");
        assertTrue(queryParam instanceof QueryParameter);

        result = reader.read(ReferenceCommonParametersApi.class);
        Operation get = result.getPath("/apath").getGet();
        List<Parameter> parameters = get.getParameters();
        for (Parameter parameter : parameters) {
            assertTrue(parameter instanceof RefParameter);
        }

        ObjectMapper mapper = Json.mapper();
        ObjectWriter jsonWriter = mapper.writer(new DefaultPrettyPrinter());
        String json = jsonWriter.writeValueAsString(result);
        JsonNode expectJson = mapper.readTree(this.getClass().getResourceAsStream("/expectedOutput/swagger-common-parameters.json"));
        JsonAssert.assertJsonEquals(expectJson, json);
    }

    @Test
    public void ignoreCommonParameters() {
        reader = new JaxrsReader(new Swagger(), Mockito.mock(Log.class));
        Swagger result = reader.read(CommonParametersApiWithPathAnnotation.class);
        assertNull(result.getParameter("headerParam"));
        assertNull(result.getParameter("queryParam"));

        reader = new JaxrsReader(new Swagger(), Mockito.mock(Log.class));
        result = reader.read(CommonParametersApiWithMethod.class);
        assertNull(result.getParameter("headerParam"));
        assertNull(result.getParameter("queryParam"));
    }

    @Test
    public void detectDuplicateCommonParameter() {
        Swagger swagger = new Swagger();
        reader = new JaxrsReader(swagger, Mockito.mock(Log.class));
        reader.read(CommonParametersApi.class);
        Exception exception = null;
        try {
            reader.read(CommonParametersApi.class);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    @Test
    public void handleResponseWithInheritance() {
        Swagger result = reader.read(AnApiWithInheritance.class);
        Map<String, Model> models = result.getDefinitions();

        Map<String, Property> properties = getProperties(models, "SomeResponseWithAbstractInheritance");
        assertNotNull(properties);
        assertTrue(properties.containsKey("classProperty"));
        assertFalse(properties.containsKey("inheritedProperty"));
        assertFalse(properties.containsKey("type"));

        properties = models.get("SomeResponseBaseClass").getProperties();
        assertNotNull(properties);
        assertTrue(properties.containsKey("inheritedProperty"));
        assertTrue(properties.containsKey("type"));

        properties = getProperties(models, "SomeResponseWithInterfaceInheritance");
        assertNotNull(properties);
        assertTrue(properties.containsKey("classProperty"));
        assertFalse(properties.containsKey("inheritedProperty"));
        assertFalse(properties.containsKey("type"));

        properties = getProperties(models,"SomeResponseInterface");
        assertNotNull(properties);
        assertTrue(properties.containsKey("inheritedProperty"));
        assertTrue(properties.containsKey("type"));

        properties = getProperties(models,"SomeResponse");
        assertNotNull(properties);
        assertTrue(properties.containsKey("classProperty"));
        assertTrue(properties.containsKey("type"));

        properties = getProperties(models,"SomeOtherResponse");
        assertNotNull(properties);
        assertTrue(properties.containsKey("classProperty"));
        assertTrue(properties.containsKey("type"));
    }

    private Map<String, Property> getProperties(Map<String, Model> models, String className) {
        assertTrue(models.containsKey(className));

        Model model = models.get(className);
        if (model instanceof ComposedModel) {
            ComposedModel composedResponse = (ComposedModel) model;
            return composedResponse.getChild().getProperties();
        } else {
            return model.getProperties();
        }
    }

    public void discoverSubResource() {
        Swagger result = reader.read(SomeResource.class);
        assertSwaggerPath(result.getPath("/resource/explicit/name").getGet(), result, "/resource/implicit/name");
    }

    private void assertSwaggerPath(Operation expectedOperation, Swagger result, String expectedPath) {
        assertNotNull(result, "No Swagger object created");
        assertFalse(result.getPaths().isEmpty(), "Should contain operation paths");
        assertTrue(result.getPaths().containsKey(expectedPath), "Expected path missing");
        io.swagger.models.Path path = result.getPaths().get(expectedPath);
        assertFalse(path.getOperations().isEmpty(), "Should be a get operation");
        assertEquals(expectedOperation, path.getGet(), "Should contain operation");
    }

    @Api(tags = "atag")
    @Path("/apath")
    static class AnApi {
        @ApiOperation(value = "Get a model.")
        @GET
        public Response getOperation() {
            return Response.ok().build();
        }
    }

    @Api(hidden = true, tags = "atag")
    @Path("/hidden/path")
    static class HiddenApi {
        @ApiOperation(value = "Get a model.")
        @GET
        public Response getOperation() {
            return Response.ok().build();
        }
    }

    @Path("/apath")
    static class NotAnnotatedApi {
    }

    @Api
    static class CommonParametersApi {
        @HeaderParam("headerParam")
        public String headerParam;

        @QueryParam("queryParam")
        public String queryParam;
    }

    @Api
    static class CommonParametersApiWithMethod {
        @HeaderParam("headerParam")
        public String headerParam;

        @QueryParam("queryParam")
        public String queryParam;

        @GET
        public Response getOperation() {
            return Response.ok().build();
        }
    }

    @Api
    @Path("/apath")
    static class CommonParametersApiWithPathAnnotation {
        @HeaderParam("headerParam")
        public String headerParam;

        @QueryParam("queryParam")
        public String queryParam;
    }

    @Api
    @Path("/apath")
    static class ReferenceCommonParametersApi {
        @GET
        public Response getOperation(
            @HeaderParam("headerParam") String headerParam,
            @QueryParam("queryParam") String queryParam) {
            return Response.ok().build();
        }
    }

    @Api(value = "v1")
    @Path("/apath")
    static class AnApiWithOctetStream {
        @POST
        @Path("/add")
        @ApiOperation(value = "Add content")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        public void addOperation(
                @ApiParam(value = "content", required = true, type = "string", format = "byte")
                    final byte[] content) {
        }
    }

    @Path("/resource")
    @Api(tags = "Resource")
    static class SomeResource {
        @Path("explicit")
        public SomeSubResource getSomething() {
            // no implementation needed. Method is only for the test cases, so that the return type is captured
            return new SomeSubResource();
        }
        @Path("implicit")
        @ApiOperation(value="", response = SomeSubResource.class)
        public Object getSomeSub() {
            // no implementation needed. Method is only for the test cases, so that the return type is overridden by @ApiOperation.response
            return new SomeSubResource();
        }
    }

    static class SomeSubResource {
        @Path("name")
        @GET
        public String getName() {
            // no implementation needed. Method is only for the test cases
            return toString();
        }
    }

    @Api
    @Path("/apath")
    static class AnApiWithInheritance {
        @GET
        public SomeResponseWithAbstractInheritance getOperation() {
            return null;
        }

        @GET
        public SomeResponseBaseClass getOperation2() { return null; }

        @GET
        public SomeResponseWithInterfaceInheritance getOperation3() { return null; }

        @GET
        public SomeResponseInterface getOperation4() {
            return null;
        }

        @GET
        public List<SomeResponse> getOperation5() { return null; }

        @GET
        public SomeOtherResponse[] getOperation6() { return null; }
    }

    @JsonTypeInfo(use=Id.NAME, property="type")
    static class SomeResponseWithAbstractInheritance extends SomeResponseBaseClass {
        public String getClassProperty(){
            return null;
        }
    }

    @JsonTypeInfo(use=Id.NAME, property="type")
    @JsonSubTypes({
            @JsonSubTypes.Type(SomeResponseWithAbstractInheritance.class)
    })
    static abstract class SomeResponseBaseClass {
        public String getInheritedProperty(){
            return null;
        }
    }

    @JsonTypeInfo(use=Id.NAME, property="type")
    static class SomeResponseWithInterfaceInheritance implements SomeResponseInterface {
        public String getClassProperty(){
            return null;
        }
        public String getInheritedProperty(){
            return null;
        }
    }

    @JsonTypeInfo(use=Id.NAME, property="type")
    @JsonSubTypes({
            @JsonSubTypes.Type(SomeResponseWithInterfaceInheritance.class)
    })
    interface SomeResponseInterface {
        String getInheritedProperty();
    }

    @JsonTypeInfo(use=Id.NAME, property="type")
    static class SomeResponse {
        public String getClassProperty() { return null; }
    }

    @JsonTypeInfo(use=Id.NAME, property="type")
    static class SomeOtherResponse {
        public String getClassProperty() { return null; }
    }
}
