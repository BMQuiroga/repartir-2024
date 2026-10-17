package ar.com.grupoesfera.repartir.steps.gastos;

import ar.com.grupoesfera.repartir.model.Gasto;
import ar.com.grupoesfera.repartir.model.Grupo;
import ar.com.grupoesfera.repartir.repositories.GruposRepository;
import ar.com.grupoesfera.repartir.services.GruposService;
import ar.com.grupoesfera.repartir.services.MontosService;
import ar.com.grupoesfera.repartir.steps.FastCucumberSteps;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;

import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TotalNoNegativoSteps extends FastCucumberSteps {

    private GruposService grupos;
    private Grupo grupo;
    private Exception error;

    private BigDecimal monto(String valor) {

        String numero = valor.replace("$", "").trim()
                .replace(".", "")
                .replace(",", ".");
        return new BigDecimal(numero);
    }

    @Dado("el grupo #{int} {string} con total {string}")
    public void elGrupoConTotal(int id, String nombre, String total) {

        grupo = new Grupo(id);
        grupo.setNombre(nombre);
        grupo.setTotal(monto(total));

        GruposRepository repository = mock(GruposRepository.class);
        when(repository.findById((long) id)).thenReturn(Optional.of(grupo));

        grupos = new GruposService();
        ReflectionTestUtils.setField(grupos, "repository", repository);
        ReflectionTestUtils.setField(grupos, "montos", new MontosService());
    }

    @Cuando("se intenta agregar un gasto de {string}")
    public void seIntentaAgregarUnGastoDe(String monto) {

        Gasto gasto = new Gasto();
        gasto.setMonto(monto(monto));

        try {
            grupos.agregarGasto(grupo.getId(), gasto);
        } catch (Exception e) {
            error = e;
        }
    }

    @Entonces("se rechaza el gasto")
    public void seRechazaElGasto() {

        assertThat(error).isNotNull();
    }

    @Entonces("se acepta el gasto")
    public void seAceptaElGasto() {

        assertThat(error).isNull();
    }

    @Y("el total del grupo continúa siendo {string}")
    public void elTotalDelGrupoContinuaSiendo(String total) {

        assertThat(grupo.getTotal()).isEqualByComparingTo(monto(total));
    }

    @Y("el total del grupo es {string}")
    public void elTotalDelGrupoEs(String total) {

        assertThat(grupo.getTotal()).isEqualByComparingTo(monto(total));
    }
}
