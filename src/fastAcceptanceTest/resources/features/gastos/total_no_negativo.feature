# language: es

Característica: Mantener totales de grupo no negativos

  Regla: El total de un grupo no debe ser negativo

#    Escenario: Rechazar un gasto que deja el total negativo
#      Dado el grupo #21 'Viaje' con total '$ 100,00'
#      Cuando se intenta agregar un gasto de '$ -100,01'
#      Entonces se rechaza el gasto
#      Y el total del grupo continúa siendo '$ 100,00'

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
