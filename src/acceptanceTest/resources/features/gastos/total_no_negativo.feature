# language: es

Característica: Mantener totales de grupo no negativos

  Regla: El total de un grupo no debe ser negativo

    Escenario: Rechazar un gasto que deja el total negativo
      Dado el grupo #21 'Viaje' con total '$ 100,00'
      Cuando se intenta agregar un gasto de '$ -100,01'
      Entonces se rechaza el gasto
      Y se informa 'El total del grupo no puede ser negativo'
      Y el total del grupo continúa siendo '$ 100,00'
