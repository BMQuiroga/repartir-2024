package ar.com.grupoesfera.repartir.atest;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;

@Component
@Profile("acceptanceTest")
public class BaseDeDatosFixture {

    private static final String MIGRATION_PATH = "filesystem:src/main/db/migrations";
    private static final String DATA_PATH = "classpath:data/";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ResourceLoader resourceLoader;

    public void estaVacia() {

        Flyway flyway = Flyway
                .configure()
                .dataSource(dataSource)
                .locations(MIGRATION_PATH)
                .createSchemas(true)
                .load();

        flyway.clean();
        flyway.migrate();
    }

    public void existeUnUnicoGrupo() {

        estaVacia();
        var populator = new ResourceDatabasePopulator();
        populator.addScript(resourceLoader.getResource(DATA_PATH + "existeUnUnicoGrupo.sql"));
        populator.execute(dataSource);
    }

    public void tieneElGrupoConTotal(int id, String nombre, BigDecimal total) {

        estaVacia();
        var insert =
                """
                    INSERT INTO grupo (id, nombre, total)
                    VALUES (<ID>, '<NOMBRE>', <TOTAL>);

                    INSERT INTO grupo_miembros (grupo_id, miembro)
                    VALUES (<ID>, 'bob'), (<ID>, 'patricio')
                """
                .replaceAll("<ID>", String.valueOf(id))
                .replaceAll("<NOMBRE>", nombre)
                .replaceAll("<TOTAL>", total.toPlainString());

        var populator = new ResourceDatabasePopulator();
        populator.addScript(new ByteArrayResource(insert.getBytes()));
        populator.execute(dataSource);
    }

    public void tieneElGrupoSinGastos(int id, String nombre) {

        estaVacia();
        var insert =
                """
                    INSERT INTO grupo (id, nombre, total)
                    VALUES (<ID>, '<NOMBRE>', 0);
                            
                    INSERT INTO grupo_miembros (grupo_id, miembro)
                    VALUES (<ID>, 'bob'), (<ID>, 'patricio')
                """
                .replaceAll("<ID>", String.valueOf(id))
                .replaceAll("<NOMBRE>", nombre);

        var populator = new ResourceDatabasePopulator();
        populator.addScript(new ByteArrayResource(insert.getBytes()));
        populator.execute(dataSource);
    }
}
