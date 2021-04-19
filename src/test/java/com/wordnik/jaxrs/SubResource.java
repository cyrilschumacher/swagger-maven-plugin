package com.wordnik.jaxrs;

import com.wordnik.sample.model.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import java.util.Arrays;
import java.util.List;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 *
 * @author pradeep.chaudhary
 */
@Produces(MediaType.APPLICATION_JSON)
@Api(hidden=true, authorizations = {@Authorization(value="api_key")}, tags = {"Resource-V1"})
public class SubResource {

    @GET
    @ApiOperation(value="List of users",notes="Get user list")     
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Successful operation", response = List.class)
    })
    public Response getUsers() {
        User john = new User();
        john.setFirstName("John");
        john.setEmail("john@testdomain.com");
        
        User max = new User();
        max.setFirstName("Max");
        max.setEmail("max@testdomain.com");        
        
        return Response.ok(Arrays.asList(john, max)).build();
    }

    @Path("/{username}")
    @GET
    @ApiOperation(value="Fetch a user by username")     
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Successful operation", response = User.class)
    })
    public Response getUserByName(@ApiParam(value = "Username of user that needs to be fetched", required = true) 
                                    @PathParam("username") String username) {
        User max = new User();
        max.setFirstName("Max");
        max.setEmail("max@testdomain.com");
        max.setUsername("max");
                
        return Response.ok(max).build();
    }

    @PUT
    @ApiOperation(value="Update User")     
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Successful operation")
    })
    public Response updateUser(@ApiParam(value = "User to be updated", required = true) User user) {        
        return Response.ok().build();
    }
    
    
}
