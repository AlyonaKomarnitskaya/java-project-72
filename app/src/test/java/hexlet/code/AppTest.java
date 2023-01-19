package hexlet.code;


import hexlet.code.domain.Url;

import static org.assertj.core.api.Assertions.assertThat;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import io.javalin.Javalin;
import io.ebean.DB;
import hexlet.code.domain.query.QUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.ebean.Transaction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AppTest {
    @Test
    void testInit() {
        assertThat(true).isEqualTo(true);
    }

    private static Javalin app;
    private static String baseUrl;
    private static Url existingUrl;
    private static Transaction transaction;
    private static final String SAMPLE_DIRECTORY = "src/test/resources";

    @BeforeAll
    public static void beforeAll() throws IOException {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        existingUrl = new Url("https://youtube.com");
        existingUrl.save();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        app.stop();
    }

    @BeforeEach
    final void beforeEach() {
        transaction = DB.beginTransaction();
    }

    @AfterEach
    final void afterEach() {
        transaction.rollback();
    }

    @Test
    void testIndex() {
        HttpResponse<String> response = Unirest.get(baseUrl).asString();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void testCreateExistingUrl() {
        HttpResponse<String> responsePost1 = Unirest
                .post(baseUrl + "/urls")
                .field("url", existingUrl.getName())
                .asEmpty();

        HttpResponse<String> response = Unirest
                .get(baseUrl)
                .asString();
        String body = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void testIncorrectShowId() {
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls/100")
                .asString();
        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    void testIndexUrls() {
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();
        String body = response.getBody();
        assertThat(body).contains(existingUrl.getName());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void testCreateUrl() {
        String inputName = "https://youtube.com";
        HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", inputName)
                .asEmpty();

        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);

        Url actualUrl = new QUrl()
                .name.equalTo(inputName)
                .findOne();

        assertThat(actualUrl).isNotNull();
        assertThat(actualUrl.getName()).isEqualTo(inputName);
    }
    @Test
    void testCreateInvalidUrl() {
        String inputUrl = "Qwerty";
        HttpResponse responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", inputUrl)
                .asEmpty();

        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();
        String body = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(body).doesNotContain(inputUrl);

        Url actualUrl = new QUrl()
                .name.equalTo(inputUrl)
                .findOne();

        assertThat(actualUrl).isNull();
    }

    @Test
    void testCheckUrl() throws Exception {
        String samplePage = Files.readString(Paths.get(SAMPLE_DIRECTORY, "sample.html"));

        MockWebServer mockServer = new MockWebServer();
        mockServer.enqueue(new MockResponse().setBody(samplePage));
        mockServer.start();
        String samplePageUrl = mockServer.url("/").toString();

        HttpResponse response = Unirest
                .post(baseUrl + "/urls/")
                .field("url", samplePageUrl)
                .asEmpty();

        Url url = new QUrl()
                .name.equalTo(samplePageUrl.substring(0, samplePageUrl.length() - 1))
                .findOne();

        HttpResponse response1 = Unirest
                .post(baseUrl + "/urls/" + url.getId() + "/checks")
                .asString();

        assertThat(response1.getStatus()).isEqualTo(302);
        assertThat(response1.getHeaders().getFirst("Location")).isEqualTo("/urls/" + url.getId());

        String body = Unirest
                .get(baseUrl + "/urls/" + url.getId())
                .asString()
                .getBody();

        assertThat(body).contains("200");
        assertThat(body).contains("Sample title");
        assertThat(body).contains("Sample description");
        assertThat(body).contains("Sample header");

    }
}

