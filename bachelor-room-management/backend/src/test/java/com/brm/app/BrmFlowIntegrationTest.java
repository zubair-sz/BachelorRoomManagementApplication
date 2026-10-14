package com.brm.app;

import com.brm.app.config.AppClock;
import com.brm.app.service.ChoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Walks through a realistic flat-share: sign up, create/join a room, split rent, settle up,
 * delegate tasks, rotate a weekly chore, post to the board, and remove a roommate.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BrmFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ChoreService choreService;
    @Autowired
    private AppClock clock;

    private final ObjectMapper json = new ObjectMapper();

    private record Person(String token, String id) {}

    private Person register(String name) throws Exception {
        String email = name.toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        String body = send(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", name, "email", email, "password", "password123"))),
                null, 201);
        return new Person(JsonPath.read(body, "$.token"), JsonPath.read(body, "$.userId"));
    }

    private String send(RequestBuilder request, Person as, int expectedStatus) throws Exception {
        RequestBuilder authenticated = as == null ? request : servletContext -> {
            MockHttpServletRequest built = request.buildRequest(servletContext);
            built.addHeader("Authorization", "Bearer " + as.token());
            return built;
        };
        return mvc.perform(authenticated).andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }

    private String postJson(String url, Object body, Person as, int expectedStatus) throws Exception {
        return send(post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)),
                as, expectedStatus);
    }

    private double netOf(String balancesJson, Person person) {
        List<Number> values = JsonPath.read(balancesJson, "$[?(@.userId=='" + person.id() + "')].net");
        return values.get(0).doubleValue();
    }

    @Test
    void authRejectsMissingAndBadTokens() throws Exception {
        send(get("/api/rooms"), null, 401);
        mvc.perform(get("/api/rooms").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void flatShareEndToEnd() throws Exception {
        Person admin = register("Asha");
        Person member = register("Ravi");
        Person roomie = register("Kiran");
        Person outsider = register("Zed");
        LocalDate today = clock.today(); // read once so the test can't straddle midnight

        // --- rooms: create, join by code, roster ---
        String created = postJson("/api/rooms", Map.of("name", "Flat 4B"), admin, 201);
        String roomId = JsonPath.read(created, "$.id");
        String inviteCode = JsonPath.read(created, "$.inviteCode");
        String base = "/api/rooms/" + roomId;

        String joined = postJson("/api/rooms/join", Map.of("inviteCode", inviteCode), member, 200);
        assertThat((Boolean) JsonPath.read(joined, "$.admin")).isFalse();
        assertThat((Object) JsonPath.read(joined, "$.inviteCode")).isNull(); // only the admin sees the code
        postJson("/api/rooms/join", Map.of("inviteCode", inviteCode), member, 200); // joining twice is harmless
        assertThat((Integer) JsonPath.read(send(get(base + "/members"), member, 200), "$.length()")).isEqualTo(2);

        send(get(base + "/members"), outsider, 403);
        postJson("/api/rooms/join", Map.of("inviteCode", "nope"), outsider, 404);

        // --- expenses: only the admin logs them; equal split puts the extra cent on the first person ---
        Map<String, Object> rent = Map.of("description", "Rent", "amount", 100.01, "splitType", "EQUAL");
        postJson(base + "/expenses", rent, member, 403);
        postJson(base + "/expenses", rent, admin, 201);

        String balances = send(get(base + "/balances"), member, 200);
        assertThat(netOf(balances, admin)).isCloseTo(50.00, within(0.001));
        assertThat(netOf(balances, member)).isCloseTo(-50.00, within(0.001));

        // custom splits must add up
        Map<String, Object> badSplit = Map.of("description", "Groceries", "amount", 50, "splitType", "CUSTOM",
                "shares", List.of(Map.of("userId", admin.id(), "amount", 20), Map.of("userId", member.id(), "amount", 20)));
        postJson(base + "/expenses", badSplit, admin, 400);

        // --- payments: member logs, only recipient/admin confirms, balances move on confirmation ---
        String payment = postJson(base + "/payments", Map.of("amount", 50.00, "note", "UPI"), member, 201);
        String paymentId = JsonPath.read(payment, "$.id");
        assertThat((String) JsonPath.read(payment, "$.status")).isEqualTo("PENDING");
        assertThat(netOf(send(get(base + "/balances"), member, 200), member)).isCloseTo(-50.00, within(0.001));

        send(post(base + "/payments/" + paymentId + "/confirm"), member, 403);
        String confirmed = send(post(base + "/payments/" + paymentId + "/confirm"), admin, 200);
        assertThat((String) JsonPath.read(confirmed, "$.status")).isEqualTo("CONFIRMED");
        send(post(base + "/payments/" + paymentId + "/confirm"), admin, 409); // can't resolve twice

        balances = send(get(base + "/balances"), admin, 200);
        assertThat(netOf(balances, admin)).isCloseTo(0.0, within(0.001));
        assertThat(netOf(balances, member)).isCloseTo(0.0, within(0.001));

        // --- tasks: admin assigns, assignee checks off, bystanders can't ---
        postJson(base + "/members", Map.of("email", "nobody-here@example.com"), admin, 404);
        String roomieEmail = JsonPath.read(send(get("/api/users/me"), roomie, 200), "$.email");
        postJson(base + "/members", Map.of("email", roomieEmail), admin, 201);

        postJson(base + "/tasks", Map.of("title", "Buy bin bags", "assignedTo", member.id()), member, 403);
        String task = postJson(base + "/tasks",
                Map.of("title", "Buy bin bags", "assignedTo", member.id(), "dueDate", today.plusDays(2).toString()),
                admin, 201);
        String taskId = JsonPath.read(task, "$.id");

        assertThat((Integer) JsonPath.read(send(get(base + "/tasks?mine=true"), member, 200), "$.length()")).isEqualTo(1);
        send(patch(base + "/tasks/" + taskId + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}"), roomie, 403);
        String done = send(patch(base + "/tasks/" + taskId + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}"), member, 200);
        assertThat((String) JsonPath.read(done, "$.status")).isEqualTo("DONE");
        assertThat((Object) JsonPath.read(done, "$.completedAt")).isNotNull();

        // --- recurring chore: created on today's weekday, so this week's task exists straight away ---
        String todayDow = today.getDayOfWeek().name();
        String chore = postJson(base + "/chores",
                Map.of("title", "Clean bathroom", "dayOfWeek", todayDow, "rotate", true), admin, 201);
        assertThat((String) JsonPath.read(chore, "$.assignedToId")).isEqualTo(member.id()); // already rotated on

        List<String> firstWeek = JsonPath.read(send(get(base + "/tasks"), admin, 200),
                "$[?(@.recurring==true)].assignedToId");
        assertThat(firstWeek).containsExactly(admin.id());

        // the midnight job is idempotent...
        assertThat(choreService.generateDueTasks(today)).isZero();
        // ...and a week later it hands the chore to the next roommate
        assertThat(choreService.generateDueTasks(today.plusWeeks(1))).isEqualTo(1);
        List<String> bothWeeks = JsonPath.read(send(get(base + "/tasks"), admin, 200),
                "$[?(@.recurring==true)].assignedToId");
        assertThat(bothWeeks).containsExactlyInAnyOrder(admin.id(), member.id());

        // --- board: text post, photo upload, access control, rejected file types ---
        send(multipart(base + "/media").param("caption", "Anyone seen the remote?"), member, 201);
        MockMultipartFile photo = new MockMultipartFile("file", "fridge.png", "image/png", new byte[]{1, 2, 3});
        String post = send(multipart(base + "/media").file(photo).param("caption", "Fridge is broken"), member, 201);
        assertThat((String) JsonPath.read(post, "$.fileType")).isEqualTo("IMAGE");
        String fileUrl = JsonPath.read(post, "$.fileUrl");

        mvc.perform(get("/api" + fileUrl).header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        send(get("/api" + fileUrl), outsider, 403);

        MockMultipartFile script = new MockMultipartFile("file", "evil.svg", "image/svg+xml", "<svg/>".getBytes());
        send(multipart(base + "/media").file(script), member, 400);
        send(multipart(base + "/media"), member, 400); // nothing to post

        // --- removing people: blocked while they owe money, allowed once settled ---
        Map<String, Object> dinner = Map.of("description", "Dinner", "amount", 50, "splitType", "CUSTOM",
                "shares", List.of(Map.of("userId", admin.id(), "amount", 20), Map.of("userId", roomie.id(), "amount", 30)));
        postJson(base + "/expenses", dinner, admin, 201);
        assertThat(netOf(send(get(base + "/balances"), admin, 200), roomie)).isCloseTo(-30.0, within(0.001));
        send(delete(base + "/members/" + roomie.id()), admin, 409);
        send(delete(base + "/members/" + admin.id()), admin, 409); // the admin can't be removed

        send(delete(base + "/members/" + member.id()), member, 403); // only the admin removes people
        send(delete(base + "/members/" + member.id()), admin, 204);
        send(get(base + "/members"), member, 403);
    }
}
