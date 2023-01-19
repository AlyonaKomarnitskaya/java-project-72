package hexlet.code;


import hexlet.code.domain.Url;

import static org.assertj.core.api.Assertions.assertThat;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import io.javalin.Javalin;
import io.ebean.DB;
import io.ebean.Database;
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

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        existingUrl = new Url("https://github.com");
        existingUrl.save();
    }

    @AfterAll
    public static void afterAll() {
        app.stop();
    }

    @BeforeEach
    void beforeEach() {
        transaction = DB.beginTransaction();
    }

    @AfterEach
    void afterEach() {
        transaction.rollback();
    }

    @Test
    void testIndex() {
        HttpResponse<String> response = Unirest.get(baseUrl).asString();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void testCreateExistingUrl() {
        String inputName = "https://www.newyear.com";
        HttpResponse<String> responsePost1 = Unirest
                .post(baseUrl + "/urls")
                .field("url", inputName)
                .asEmpty();

        assertThat(responsePost1.getHeaders().getFirst("Location")).isEqualTo("/urls");

        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();
        String body = response.getBody();

        assertThat(body).contains(inputName);
        assertThat(body).contains("Страница уже существует");
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
        String inputName = "https://christmas.com";
        HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", inputName)
                .asEmpty();

        assertThat(responsePost.getStatus()).isEqualTo(302);
        assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();
        String body = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(body).contains(inputName);
        assertThat(body).contains("Статья успешно добавлена");

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
}

