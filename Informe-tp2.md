# TP2 - BDD/TDD - repartir: «El total de un grupo no debe ser negativo»

**Alumno:** Bruno Quiroga

**Padrón:** 107.788

**Materia:** ESTÁNDARES DE CALIDAD Y MODELOS DE REFERENCIA (95.30)

**Facultad de Ingeniería — Universidad de Buenos Aires**

---

## 1. Descripción de la funcionalidad

`repartir` es una aplicación web (Spring Boot + Angular) para crear grupos de personas y acumular gastos compartidos. Cada grupo tiene un `total` que se actualiza al agregar un gasto (`POST /api/grupos/{id}/gastos`).

Antes del cambio, el monto de un gasto no tenía restricciones de signo: un gasto negativo podía dejar el total del grupo por debajo de cero.

### Regla de negocio

> **El total de un grupo no debe ser negativo.**

Para todo grupo y gasto debe cumplirse `totalActual + montoGasto >= 0`. Si el resultado fuera negativo, el gasto se **rechaza**: el grupo no se modifica ni se guarda, la API responde `400 Bad Request` y la interfaz informa **"El total del grupo no puede ser negativo"**. Un gasto que deja el total exactamente en cero es válido.

### Escenarios (Gherkin)

```gherkin
Característica: Mantener totales de grupo no negativos

  Regla: El total de un grupo no debe ser negativo

    Escenario: Rechazar un gasto que deja el total negativo
      Dado el grupo #21 'Viaje' con total '$ 100,00'
      Cuando se intenta agregar un gasto de '$ -100,01'
      Entonces se rechaza el gasto
      Y el total del grupo continúa siendo '$ 100,00'

    Escenario: Aceptar un gasto que deja el total en cero
      Dado el grupo #21 'Viaje' con total '$ 100,00'
      Cuando se intenta agregar un gasto de '$ -100,00'
      Entonces se acepta el gasto
      Y el total del grupo es '$ 0,00'

    Escenario: Aceptar un gasto que mantiene el total positivo
      Dado el grupo #21 'Viaje' con total '$ 100,00'
      Cuando se intenta agregar un gasto de '$ 25,00'
      Entonces se acepta el gasto
      Y el total del grupo es '$ 125,00'
```

Estos tres escenarios se automatizan como **pruebas de aceptación rápidas** (`fastAcceptanceTest`), sobre los objetos de dominio y sin navegador.

### Escenario de interfaz (acceptance test punta a punta)

Además, el escenario de rechazo se automatiza **de punta a punta** como `acceptanceTest` (Cucumber + Selenium + MariaDB con Testcontainers), en un segundo archivo `.feature` con la misma `Característica` y `Regla`. Mantiene las frases de negocio del escenario original y agrega la verificación del mensaje que ve el usuario:

```gherkin
Característica: Mantener totales de grupo no negativos

  Regla: El total de un grupo no debe ser negativo

    Escenario: Rechazar un gasto que deja el total negativo
      Dado el grupo #21 'Viaje' con total '$ 100,00'
      Cuando se intenta agregar un gasto de '$ -100,01'
      Entonces se rechaza el gasto
      Y se informa 'El total del grupo no puede ser negativo'
      Y el total del grupo continúa siendo '$ 100,00'
```

Es el mismo comportamiento de negocio que el primer escenario de la aceptación rápida, pero validado a través de la aplicación completa: frontend, API, servicio y base de datos. Los pasos se implementan con Selenium, en lugar de invocar los objetos de dominio. Solo se automatiza el caso de rechazo, porque los casos de total en cero y de total positivo ya están cubiertos por la aceptación rápida y los E2E son más lentos.

### Calidad de los escenarios

Los escenarios cumplen BRIEF: usan lenguaje de negocio, datos reales (grupo #21 «Viaje», `$ 100,00`), expresan la intención sin detalles de UI o HTTP y son breves. Cubren el límite (`-100,01` se rechaza, `-100,00` se acepta) y el caso positivo.

### Metodología

Se siguió el ciclo BDD/TDD: primero la regla y los escenarios, después las pruebas de aceptación y las unitarias, y por último la implementación mínima, en commits incrementales. Cada prueba que no pasaba se commiteó **comentada** y se habilitó junto con el código que la hace pasar.

---

## 2. Tests agregados

| Tipo | Archivo | Qué verifica |
|---|---|---|
| Aceptación rápida (Cucumber, dominio) | `src/fastAcceptanceTest/resources/features/gastos/total_no_negativo.feature` | Los 3 escenarios de la regla. |
| Unitaria | `src/test/java/ar/com/grupoesfera/repartir/services/MontosServiceTest.java` → `acumularGastoQueDejaElTotalNegativoLanzaExcepcion` | Un gasto que deja el total negativo lanza `GrupoInvalidoException`. |
| Unitaria | `MontosServiceTest` → `acumularGastoRechazadoNoModificaElTotal` | Tras el rechazo, el total del grupo sigue siendo el original (`$ 100,00`). |
| Integración | `src/integrationTest/java/ar/com/grupoesfera/repartir/controllers/GruposControllerIntegrationTest.java` → `agregarGastoQueDejaElTotalNegativoRespondeBadRequest` | `POST /api/grupos/21/gastos` responde `400` cuando el servicio lanza `GrupoInvalidoException`. |
| Aceptación E2E (Cucumber + Selenium) | `src/acceptanceTest/resources/features/gastos/total_no_negativo.feature` | Escenario «Rechazar un gasto que deja el total negativo» de punta a punta: el usuario intenta el gasto, ve el error "El total del grupo no puede ser negativo" y el total en pantalla no cambia. |

### Resultado de la verificación

`./gradlew check --offline` termina en `BUILD SUCCESSFUL` con todo en verde (unitarias, `fastAcceptanceTest`, `integrationTest`, `uiTest` y `acceptanceTest`). La última corrida forzada de `acceptanceTest` dio 16 casos, 0 fallos y 1 omitido (un escenario marcado `@pendiente`, preexistente). Los avisos de PMD no rompen el build.

### Decisiones sobre los tests

- No se agregaron las pruebas de total en cero y positivo en `MontosServiceTest`, ni la prueba de `GruposServiceTest` sobre el `save()`, porque ya pasaban con el código existente.
- No se agrego prueba unitaria en el frontend, ya que el proyecto no realiza pruebas de ese tipo para esta clase de reglas. El E2E solo cubre el caso de rechazo: es lento y los casos cero y positivo ya los cubre la aceptación rápida.

---

## 3. Código agregado

### Backend

**`src/main/java/ar/com/grupoesfera/repartir/services/MontosService.java`**: se valida el nuevo total **antes** de modificar el grupo. La validación se hace antes de asignar el total para que un gasto rechazado no deje cambios en memoria ni se persista (`GruposService.agregarGasto` llama a `acumularAlTotal` antes de `repository.save`).

```java
public void acumularAlTotal(Grupo grupo, Gasto gasto) {

    BigDecimal total = grupo.getTotal();
    total = total.add(gasto.getMonto());

    if (total.signum() < 0) {
        throw new GrupoInvalidoException();
    }

    grupo.setTotal(total);
}
```

**`src/main/java/ar/com/grupoesfera/repartir/controllers/GruposController.java`**: `agregarGasto` traduce `GrupoInvalidoException` a `400 Bad Request`. Antes caía en el `catch (Exception)` y respondía `500`.

```java
} catch (GrupoInvalidoException e) {

    response = ResponseEntity.badRequest().build();
}
```

### Frontend

**`src/main/frontend/src/app/services/grupo.service.ts`**: ante un `400` al agregar un gasto se muestra el mensaje de negocio; el resto de los errores conserva el mensaje genérico "No se puede guardar".

```ts
agregarGasto(grupo: Grupo, monto: number): Observable<Grupo> {
  ...
  return this.http.post<Grupo>(`/api/grupos/${grupo.id}/gastos`, nuevoGasto)
    .pipe(catchError(error => this.falloAlAgregarGasto(error)));
}

falloAlAgregarGasto(error: HttpErrorResponse) {
  if (error.status === 400) {
    return throwError(new Excepcion('El total del grupo no puede ser negativo'));
  }
  return this.falloAlGuardar(error);
}
```

**`src/main/frontend/src/app/components/gasto-nuevo/gasto-nuevo.component.html`**: el campo de monto pasó de `p-inputNumber` (PrimeNG 12.0.1) a un `<input type="number" step="0.01">`.

```html
<input id="montoGastoNuevoInput" type="number" step="0.01" class="inputfield w-full" pInputText [(ngModel)]="monto"/>
```

Motivo: con `p-inputNumber` no se podía ingresar un monto negativo. Usa el idioma del navegador (`en-US`, donde el decimal es `.` y la coma se ignora) y, tras tipear `-`, reformatea el campo dejando el cursor al final. Al tipear `-100,01` quedaba `-ARS 1.00`, y el gasto se enviaba con otro valor. El costo del cambio es que el campo ya no muestra formato de moneda mientras se escribe.
