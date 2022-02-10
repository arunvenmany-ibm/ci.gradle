/**
 * OpenAPI Petstore
 * This is a sample server Petstore server. For this sample, you can use the api key `special-key` to test the authorization filters.
 *
 * The version of the OpenAPI document: 1.0.0
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openliberty.guides.inventory.api;

import java.io.File;
import io.openliberty.guides.inventory.model.ModelApiResponse;
import io.openliberty.guides.inventory.model.Pet;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * OpenAPI Petstore
 *
 * <p>This is a sample server Petstore server. For this sample, you can use the api key `special-key` to test the authorization filters.
 *
 */

@RegisterRestClient
@RegisterProvider(ApiExceptionMapper.class)
@Path("/")
public interface PetApi  {

    /**
     * Add a new pet to the store
     *
     */
    @POST
    @Path("/pet")
    @Consumes({ "application/json", "application/xml" })
    public void addPet(Pet body) throws ApiException, ProcessingException;

    /**
     * Deletes a pet
     *
     */
    @DELETE
    @Path("/pet/{petId}")
    public void deletePet(@PathParam("petId") Long petId, @HeaderParam("api_key")  String apiKey) throws ApiException, ProcessingException;

    /**
     * Finds Pets by status
     *
     * Multiple status values can be provided with comma separated strings
     *
     */
    @GET
    @Path("/pet/findByStatus")
    @Produces({ "application/xml", "application/json" })
    public List<Pet> findPetsByStatus(@QueryParam("status") List<String> status) throws ApiException, ProcessingException;

    /**
     * Finds Pets by tags
     *
     * Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing.
     *
     */
    @GET
    @Path("/pet/findByTags")
    @Produces({ "application/xml", "application/json" })
    public List<Pet> findPetsByTags(@QueryParam("tags") List<String> tags) throws ApiException, ProcessingException;

    /**
     * Find pet by ID
     *
     * Returns a single pet
     *
     */
    @GET
    @Path("/pet/{petId}")
    @Produces({ "application/xml", "application/json" })
    public Pet getPetById(@PathParam("petId") Long petId) throws ApiException, ProcessingException;

    /**
     * Update an existing pet
     *
     */
    @PUT
    @Path("/pet")
    @Consumes({ "application/json", "application/xml" })
    public void updatePet(Pet body) throws ApiException, ProcessingException;
}

