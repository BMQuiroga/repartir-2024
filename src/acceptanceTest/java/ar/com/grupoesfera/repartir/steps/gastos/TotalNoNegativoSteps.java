package ar.com.grupoesfera.repartir.steps.gastos;

import ar.com.grupoesfera.repartir.steps.CucumberSteps;
import ar.com.grupoesfera.repartir.steps.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

public class TotalNoNegativoSteps extends CucumberSteps {

    private int idGrupo;

    private String soloNumero(String monto) {
        return monto.replace("$", "").trim();
    }

    @Step("el grupo #{int} {string} con total {string}")
    public void elGrupoConTotal(int id, String nombre, String total) {

        this.idGrupo = id;
        var numero = soloNumero(total).replace(".", "").replace(",", ".");
        baseDeDatos.tieneElGrupoConTotal(id, nombre, new BigDecimal(numero));

        driver.navigate().to(url("/"));
        driver.findElement(By.id("iniciarBienvenidaButton")).click();
    }

    @Step("se intenta agregar un gasto de {string}")
    public void seIntentaAgregarUnGastoDe(String monto) {

        var wait = new WebDriverWait(driver, 2);
        wait.until(visibilityOfElementLocated(By.id("agregarGastoGruposButton-" + idGrupo))).click();

        var montoInput = driver.findElement(By.id("montoGastoNuevoInput"));
        montoInput.clear();
        montoInput.sendKeys(soloNumero(monto).replace(",", "."));


        driver.findElement(By.id("guardarGastoNuevoButton")).click();
    }

    @Step("se rechaza el gasto")
    public void seRechazaElGasto() {

        var wait = new WebDriverWait(driver, 2);
        var toast = wait.until(visibilityOfElementLocated(By.id("mensajesToast")));
        assertThat(toast.getText()).contains("Error");
    }

    @Step("se informa {string}")
    public void seInforma(String mensaje) {

        var toast = driver.findElement(By.id("mensajesToast"));
        assertThat(toast.getText()).contains(mensaje);
    }

    @Step("el total del grupo continúa siendo {string}")
    public void elTotalDelGrupoContinuaSiendo(String total) {

        var filas = driver.findElements(By.cssSelector("app-grupos table tr"));
        var celdas = filas.get(1).findElements(By.tagName("td"));
        assertThat(celdas.get(2).getText()).isEqualTo(total.replace("$ ", "$  "));
    }
}
