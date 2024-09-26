/*
 * Copyright (c) 2016-2020 Contributors to the Eclipse Foundation
 *
 *  See the NOTICE file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.eclipse.microprofile.jwt.tck.container.jaxrs;

import static org.eclipse.microprofile.jwt.tck.TCKConstants.TEST_GROUP_CDI;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.tck.TCKConstants;
import org.eclipse.microprofile.jwt.tck.util.MpJwtTestVersion;
import org.eclipse.microprofile.jwt.tck.util.TokenUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Tests of injection of JsonWebToken claims using {@linkplain org.eclipse.microprofile.jwt.ClaimValue} interface
 * wrappers.
 */
public class ClaimValueInjectionTest extends Arquillian {

    /**
     * The test generated JWT token string
     */
    private static String token;
    // Time claims in the token
    private static Long iatClaim;
    private static Long authTimeClaim;

    /**
     * The base URL for the container under test
     */
    @ArquillianResource
    private URL baseURL;

    /**
     * Create a CDI aware base web application archive
     *
     * @return the base base web application archive
     * @throws IOException
     *             - on resource failure
     */
    @Deployment(testable = true)
    public static WebArchive createDeployment() throws IOException {
        URL config = ClaimValueInjectionTest.class
                .getResource("/META-INF/microprofile-config-publickey-location.properties");
        URL publicKey = ClaimValueInjectionTest.class.getResource("/publicKey.pem");
        WebArchive webArchive = ShrinkWrap
                .create(WebArchive.class, "ClaimValueInjectionTest.war")
                .addAsManifestResource(new StringAsset(MpJwtTestVersion.MPJWT_V_1_0.name()),
                        MpJwtTestVersion.MANIFEST_NAME)
                .addAsResource(publicKey, "/publicKey.pem")
                .addClass(ClaimValueInjectionEndpoint.class)
                .addClass(TCKApplication.class)
                .addAsWebInfResource("beans.xml", "beans.xml")
                .addAsManifestResource(config, "microprofile-config.properties");
        System.out.printf("WebArchive: %s\n", webArchive.toString(true));
        return webArchive;
    }

    @BeforeClass(alwaysRun = true)
    public static void generateToken() throws Exception {
        HashMap<String, Long> timeClaims = new HashMap<>();
        token = TokenUtils.generateTokenString("/Token1.json", null, timeClaims);
        iatClaim = timeClaims.get(Claims.iat.name());
        authTimeClaim = timeClaims.get(Claims.auth_time.name());
    }

    @RunAsClient
    @Test(groups = TEST_GROUP_CDI, description = "Verify that the injected token issuer claim is as expected")
    public void verifyIssuerClaim() throws Exception {
        Reporter.log("Begin verifyIssuerClaim");
        String uri = baseURL.toExternalForm() + "endp/verifyInjectedIssuer";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                .target(uri)
                .queryParam(Claims.iss.name(), TCKConstants.TEST_ISSUER)
                .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }
    @RunAsClient
    @Test(groups = TEST_GROUP_CDI, description = "Verify that the injected token issuer claim using @Claim(standard) is as expected")
    public void verifyIssuerStandardClaim() throws Exception {
        Reporter.log("Begin verifyIssuerClaim");
        String uri = baseURL.toExternalForm() + "endp/verifyInjectedIssuerStandard";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                .target(uri)
                .queryParam(Claims.iss.name(), TCKConstants.TEST_ISSUER)
                .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @RunAsClient
    @Test(groups = TEST_GROUP_CDI, description = "Verify that the injected raw token claim is as expected")
    public void verifyInjectedRawToken() throws Exception {
        Reporter.log("Begin verifyInjectedRawToken\n");
        String uri = baseURL.toExternalForm() + "endp/verifyInjectedRawToken";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                .target(uri)
                .queryParam(Claims.raw_token.name(), token)
                .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }
    @RunAsClient
    @Test(groups = TEST_GROUP_CDI, description = "Verify that the injected raw token claim using @Claim(standard) is as expected")
    public void verifyInjectedRawTokenStandard() throws Exception {
        Reporter.log("Begin verifyInjectedRawTokenStandard\n");
        String uri = baseURL.toExternalForm() + "endp/verifyInjectedRawTokenStandard";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                .target(uri)
                .queryParam(Claims.raw_token.name(), token)
                .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @RunAsClient
    @Test(groups = TEST_GROUP_CDI, description = "Verify that the injected jti claim is as expected")
    public void verifyInjectedJTI() throws Exception {
        Reporter.log("Begin verifyInjectedJTI\n");
        String uri = baseURL.toExternalForm() + "endp/verifyInjectedJTI";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                .target(uri)
                .queryParam(Claims.jti.name(), "a-123")
                .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }
    @RunAsClient
    @Test(groups = TEST_GROUP_CDI, description = "Verify that the injected jti claim using @Claim(standard) is as expected")
    public void verifyInjectedJTIStandard() throws Exception {
        Reporter.log("Begin verifyInjectedJTIStandard\n");
        String uri = baseURL.toExternalForm() + "endp/verifyInjectedJTIStandard";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                .target(uri)
                .queryParam(Claims.jti.name(), "a-123")
                .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @RunAsClient
    @Test(groups = TEST_GROUP_CDI, description = "Verify that the injected aud claim is as expected")
    public void verifyInjectedAudience() throws Exception {
        Reporter.log("Begin verifyInjectedAudience\n");
        String uri = baseURL.toExternalForm() + "endp/verifyInjectedAudience";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                .target(uri)
                .queryParam(Claims.aud.name(), "s6BhdRkqt3")
                .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        System.out.println(reply);
        Reporter.log(reply.toString());
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }
    @RunAsClient
    @Test(groups = TEST_GROUP_CDI, description = "Verify that the injected aud claim using @Claim(standard) is as expected")
    public void verifyInjectedAudienceStandard() throws Exception {
        Reporter.log("Begin verifyInjectedAudienceStandard\n");
        String uri = baseURL.toExternalForm() + "endp/verifyInjectedAudienceStandard";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                .target(uri)
                .queryParam(Claims.aud.name(), "s6BhdRkqt3")
                .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        System.out.println(reply);
        Reporter.log(reply.toString());
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @RunAsClient
    @Test(groups = TEST_GROUP_CDI, description = "Verify that the injected iat claim is as expected")
    public void verifyInjectedIssuedAt() throws Exception {
        Reporter.log("Begin verifyInjectedIssuedAt\n");
        String uri = baseURL.toExternalForm() + "endp/verifyInjectedIssuedAt";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                .target(uri)
                .queryParam(Claims.iat.name(), iatClaim)
                .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }
    @RunAsClient
    @Test(groups = TEST_GROUP_CDI, description = "Verify that the injected iat claim using @Claim(standard) is as expected")
    public void verifyInjectedIssuedAtStandard() throws Exception {
        Reporter.log("Begin verifyInjectedIssuedAtStandard\n");
        String uri = baseURL.toExternalForm() + "endp/verifyInjectedIssuedAtStandard";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                .target(uri)
                .queryParam(Claims.iat.name(), iatClaim)
                .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @RunAsClient
    @Test(groups = TEST_GROUP_CDI, description = "Verify that the injected sub claim is as expected")
    public void verifyInjectedOptionalSubject() throws Exception {
        Reporter.log("Begin verifyInjectedOptionalSubject\n");
        String uri = baseURL.toExternalForm() + "endp/verifyInjectedOptionalSubject";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                .target(uri)
                .queryParam(Claims.sub.name(), "24400320")
                .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }
    @RunAsClient
    @Test(groups = TEST_GROUP_CDI, description = "Verify that the injected sub claim using @Claim(standard) is as expected")
    public void verifyInjectedSubjectStandard() throws Exception {
        Reporter.log("Begin verifyInjectedSubjectStandard\n");
        String uri = baseURL.toExternalForm() + "endp/verifyInjectedSubjectStandard";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                .target(uri)
                .queryParam(Claims.sub.name(), "24400320")
                .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @RunAsClient
    @Test(groups = TEST_GROUP_CDI, description = "Verify that the injected raw token claim is as expected")
    public void verifyInjectedOptionalAuthTime() throws Exception {
        Reporter.log("Begin verifyInjectedOptionalAuthTime\n");
        String uri = baseURL.toExternalForm() + "endp/verifyInjectedOptionalAuthTime";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                .target(uri)
                .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }
    @RunAsClient
    @Test(groups = TEST_GROUP_CDI, description = "Verify that the injected raw token claim using @Claim(standard) is as expected")
    public void verifyInjectedAuthTimeStandard() throws Exception {
        Reporter.log("Begin verifyInjectedAuthTimeStandard\n");
        String uri = baseURL.toExternalForm() + "endp/verifyInjectedAuthTimeStandard";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                .target(uri)
                .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @RunAsClient
    @Test(groups = TEST_GROUP_CDI, description = "Verify that the injected custom claim is missing as expected")
    public void verifyInjectedOptionalCustomMissing() throws Exception {
        Reporter.log("Begin verifyInjectedOptionalCustomMissing\n");
        String uri = baseURL.toExternalForm() + "endp/verifyInjectedOptionalCustomMissing";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                .target(uri);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }
    @RunAsClient
    @Test(groups = TEST_GROUP_CDI, description = "Verify that the injected customString claim is as expected")
    public void verifyInjectedCustomString() throws Exception {
        Reporter.log("Begin verifyInjectedCustomString\n");
        String uri = baseURL.toExternalForm() + "endp/verifyInjectedCustomString";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                .target(uri)
                .queryParam("value", "customStringValue")
                .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }
    @RunAsClient
    @Test(groups = TEST_GROUP_CDI, description = "Verify that the injected customInteger claim is as expected")
    public void verifyInjectedCustomInteger() throws Exception {
        Reporter.log("Begin verifyInjectedCustomInteger\n");
        String uri = baseURL.toExternalForm() + "endp/verifyInjectedCustomInteger";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                .target(uri)
                .queryParam("value", 123456789)
                .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Reporter.log(reply.toString());
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }
    @RunAsClient
    @Test(groups = TEST_GROUP_CDI, description = "Verify that the injected customDouble claim is as expected")
    public void verifyInjectedCustomDouble() throws Exception {
        Reporter.log("Begin verifyInjectedCustomDouble\n");
        String uri = baseURL.toExternalForm() + "endp/verifyInjectedCustomDouble";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                .target(uri)
                .queryParam("value", 3.141592653589793)
                .queryParam(Claims.auth_time.name(), authTimeClaim);
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        System.out.println(reply);
        Reporter.log(reply.toString());
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }

    @RunAsClient
    @Test(groups = TEST_GROUP_CDI, description = "Verify that the injected customBoolean claim is as expected")
    public void verifyInjectedCustomBoolean() throws Exception {
        String uri = baseURL.toExternalForm() + "endp/verifyInjectedCustomBoolean";
        WebTarget echoEndpointTarget = ClientBuilder.newClient()
                .target(uri)
                .queryParam("value", "true");
        Response response = echoEndpointTarget.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        Assert.assertEquals(response.getStatus(), HttpURLConnection.HTTP_OK);
        String replyString = response.readEntity(String.class);
        JsonReader jsonReader = Json.createReader(new StringReader(replyString));
        JsonObject reply = jsonReader.readObject();
        Assert.assertTrue(reply.getBoolean("pass"), reply.getString("msg"));
    }
}
