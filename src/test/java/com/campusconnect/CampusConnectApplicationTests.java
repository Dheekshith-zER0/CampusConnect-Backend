package com.campusconnect;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

/**
 * End-to-end API tests against an in-memory H2 database (see application-test.properties).
 */
@SpringBootTest
@ActiveProfiles("test")
class CampusConnectApplicationTests {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	void healthAndMetaArePublic() throws Exception {
		mvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.database").value("UP"));

		mvc.perform(get("/api/meta"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.categories.length()").value(7))
				.andExpect(jsonPath("$.statuses[1].label").value("In Progress"));
	}

	@Test
	void protectedEndpointsRequireToken() throws Exception {
		mvc.perform(get("/api/complaints/my")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/complaints/my").header("Authorization", "Bearer nope"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void registrationIsValidated() throws Exception {
		mvc.perform(json(post("/api/auth/register"), """
				{"name":"","email":"not-an-email","password":"123","rollNumber":"R1","department":"CSE"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors", hasKey("name")))
				.andExpect(jsonPath("$.fieldErrors", hasKey("email")))
				.andExpect(jsonPath("$.fieldErrors", hasKey("password")));
	}

	@Test
	void corsAllowsConfiguredOriginsOnly() throws Exception {
		mvc.perform(options("/api/complaints")
				.header("Origin", "https://campusconnect-git-main.vercel.app")
				.header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "authorization,content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "https://campusconnect-git-main.vercel.app"));

		mvc.perform(options("/api/complaints")
				.header("Origin", "https://evil.example.com")
				.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden());
	}

	@Test
	void fullComplaintLifecycle() throws Exception {
		String student = register("asha@college.edu");
		String otherStudent = register("ravi@college.edu");

		// Duplicate email -> 409
		mvc.perform(json(post("/api/auth/register"), """
				{"name":"Asha","email":"ASHA@college.edu","password":"secret12","rollNumber":"21CS001","department":"CSE"}"""))
				.andExpect(status().isConflict());

		// Wrong password -> 401
		mvc.perform(json(post("/api/auth/login"), """
				{"email":"asha@college.edu","password":"wrong-password"}"""))
				.andExpect(status().isUnauthorized());

		// Invalid complaint -> 400
		mvc.perform(auth(json(post("/api/complaints"), """
				{"title":"Hi","description":"short"}"""), student))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors", hasKey("category")));

		// Create complaint -> 201
		String created = mvc.perform(auth(json(post("/api/complaints"), """
				{"title":"Wi-Fi down in Hostel B","description":"No internet on floors 2 and 3 since Monday.",
				 "category":"WIFI_INTERNET","location":"Hostel B"}"""), student))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("SUBMITTED"))
				.andExpect(jsonPath("$.categoryLabel").value("Wi-Fi and Internet"))
				.andExpect(jsonPath("$.student.email").value("asha@college.edu"))
				.andReturn().getResponse().getContentAsString();
		Integer id = JsonPath.read(created, "$.id");

		mvc.perform(auth(get("/api/complaints/my"), student))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));

		// Students cannot see other students' complaints or use admin endpoints
		mvc.perform(auth(get("/api/complaints/" + id), otherStudent)).andExpect(status().isForbidden());
		mvc.perform(auth(get("/api/admin/complaints"), student)).andExpect(status().isForbidden());

		// Owner can edit while still SUBMITTED
		mvc.perform(auth(json(put("/api/complaints/" + id), """
				{"title":"Wi-Fi down in Hostel B (floors 2-3)","description":"No internet on floors 2 and 3 since Monday.",
				 "category":"WIFI_INTERNET","location":"Hostel B"}"""), student))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Wi-Fi down in Hostel B (floors 2-3)"));

		// Admin manages the complaint
		String admin = login("admin@test.edu", "AdminPass1");
		mvc.perform(auth(get("/api/admin/complaints").param("status", "SUBMITTED").param("search", "hostel"), admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
		mvc.perform(auth(get("/api/admin/complaints").param("status", "BOGUS"), admin))
				.andExpect(status().isBadRequest());

		mvc.perform(auth(json(patch("/api/admin/complaints/" + id + "/status"), """
				{"status":"REJECTED"}"""), admin))
				.andExpect(status().isBadRequest());

		mvc.perform(auth(json(patch("/api/admin/complaints/" + id + "/status"), """
				{"status":"IN_PROGRESS","remarks":"Technician assigned"}"""), admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.adminRemarks").value("Technician assigned"));

		mvc.perform(auth(get("/api/admin/stats"), admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(1))
				.andExpect(jsonPath("$.inProgress").value(1));

		// Student tracks the status history
		mvc.perform(auth(get("/api/complaints/" + id), student))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.complaint.statusLabel").value("In Progress"))
				.andExpect(jsonPath("$.history.length()").value(2))
				.andExpect(jsonPath("$.history[1].remarks").value("Technician assigned"));

		// Once in progress, the student can no longer edit or withdraw it
		mvc.perform(auth(json(put("/api/complaints/" + id), """
				{"title":"Changed title","description":"Changed description text","category":"OTHER"}"""), student))
				.andExpect(status().isConflict());
		mvc.perform(auth(delete("/api/complaints/" + id), student)).andExpect(status().isConflict());

		// Admin can delete it
		mvc.perform(auth(delete("/api/complaints/" + id), admin)).andExpect(status().isNoContent());
		mvc.perform(auth(get("/api/complaints/" + id), admin)).andExpect(status().isNotFound());

		// Logout invalidates the token
		mvc.perform(auth(post("/api/auth/logout"), student)).andExpect(status().isNoContent());
		mvc.perform(auth(get("/api/auth/me"), student)).andExpect(status().isUnauthorized());
	}

	private String register(String email) throws Exception {
		String body = mvc.perform(json(post("/api/auth/register"), """
				{"name":"Test Student","email":"%s","password":"secret12","rollNumber":"21CS001","department":"CSE"}"""
				.formatted(email)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.user.role").value("STUDENT"))
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.token");
	}

	private String login(String email, String password) throws Exception {
		String body = mvc.perform(json(post("/api/auth/login"), """
				{"email":"%s","password":"%s"}""".formatted(email, password)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.token");
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
		return builder.contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
		return builder.header("Authorization", "Bearer " + token);
	}
}
