package ar.com.grupoesfera.repartir.controllers;

import ar.com.grupoesfera.repartir.itest.fixtures.GruposFixture;
import ar.com.grupoesfera.repartir.exceptions.GrupoInvalidoException;
import ar.com.grupoesfera.repartir.model.Gasto;
import ar.com.grupoesfera.repartir.services.GruposService;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.javacrumbs.jsonunit.JsonMatchers.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integrationTest")
public class GruposControllerIntegrationTest {

    final GruposFixture GRUPOS = new GruposFixture();

    @MockBean
    GruposService gruposService;

    @LocalServerPort
    int randomServerPort;

    @Test
    void listarCuandoNoExistenGrupos() {

        when(gruposService.listarGrupos()).thenReturn(emptyList());

        given()
                .accept(ContentType.JSON)
                .port(randomServerPort)
                .contentType(ContentType.JSON)
            .when()
                .get("/api/grupos")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(equalTo("[]"));
    }

    @Test
    void listarCuandoExistenUnUnicoGrupo() {

        when(gruposService.listarGrupos())
                .thenReturn(asList(GRUPOS.ALMUERZO));

        given()
                .accept(ContentType.JSON)
                .port(randomServerPort)
                .contentType(ContentType.JSON)
            .when()
                .get("/api/grupos")
            .then()
                .statusCode(200)
                .body(jsonEquals("""
                        [
                            {
                                "id": 101,
                                "nombre": "Almuerzo",
                                "miembros": ["martin",  "tatiana", "mariano"],
                                "total": 1500.33
                            }
                        ]
                        """));
    }

    @Test
    void listarCuandoExistenDosGrupos() {

        when(gruposService.listarGrupos())
                .thenReturn(asList(GRUPOS.ALMUERZO, GRUPOS.REGALO_PARA_LUCAS));

        given()
                .accept(ContentType.JSON)
                .port(randomServerPort)
                .contentType(ContentType.JSON)
            .when()
                .get("/api/grupos")
            .then()
                .statusCode(200)
                .body("id", contains(101,102))
                .body("nombre", contains("Almuerzo", "Regalo para Lucas"))
                .body("total", contains(1500.33f, 4000.0f));
    }

    @Test
    void agregarGastoQueDejaElTotalNegativoRespondeBadRequest() {
        when(gruposService.agregarGasto(eq(21L), any(Gasto.class)))
            .thenThrow(new GrupoInvalidoException());

        given()
            .accept(ContentType.JSON)
            .port(randomServerPort)
            .contentType(ContentType.JSON)
            .body("{\"monto\": -100.01}")
        .when()
            .post("/api/grupos/21/gastos")
        .then()
            .statusCode(400);
    }
}
