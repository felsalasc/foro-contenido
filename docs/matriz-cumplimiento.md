# Matriz de evidencia del foro

Actualizada: 31 de octubre de 2026. Pesos oficiales de la evaluación conjunta; no son puntos obtenidos ni una promesa de nota.

| Criterio | Peso | Evidencia disponible | Pendiente |
|---|---:|---|---|
| Git | 10 | Dos repositorios locales independientes, con historial propio | Enlaces remotos y publicación autorizada |
| CRUD Oracle | 20 | CRUD y restricciones ejecutados en Postman con ambos servidores en perfil Oracle; scripts propios | Búsqueda CLOB larga, disponibilidad y concurrencia Oracle |
| Servicios y consultas | 30 | Usuarios/auth y contenido separados; búsqueda combinada, roles, autoría, cascada y concurrencia locales | CLOB largo, caída de identidad y concurrencia Oracle |
| JSON y Postman | 25 | Runner real aportado por el usuario: usuarios 36 y contenido 66 aserciones aprobadas, cero errores/fallos | Integrar evidencias en grabación y entrega |
| Video | 15 | Guion conjunto de 8–9 minutos listo | Grabar 7–10 minutos en Kaltura y entregar en AVA |

Datos mínimos comprobados localmente: tres cuentas reales de prueba, cinco categorías, tres temas y tres comentarios. SQL parametrizado usa IDs proporcionados por el operador; la carga H2 usa identificadores simulados diferentes. Los datos también se crearon mediante las colecciones ejecutadas con perfil Oracle; falta comprobar cantidades/relaciones por SQL y persistencia tras reiniciar.

Fuera del alcance acordado: frontend y tercer servicio de moderación/baneos. Se implementan dos servicios y dos repositorios. El ZIP final, enlaces y grabación permanecen pendientes; no se publican automáticamente.

Conteos SQL previos al reinicio confirmados por el usuario: CATEGORIAS: 10; TEMAS: 3; COMENTARIOS: 3. Se cumplen los mínimos de cinco categorías y tres registros por tabla. Persistencia tras reiniciar aún pendiente.

Actualización: persistencia de conteos tras reiniciar ambos JAR confirmada textualmente por el usuario. Ver docs/evidencias/2026-10-31-oracle-reinicio.md; falta captura posterior para la demostración y las verificaciones adicionales indicadas.

Relaciones comprobadas por capturas SQL: cero huérfanos, FK_COMENTARIOS_TEMA ENABLED/VALIDATED/CASCADE y FK_TEMAS_CATEGORIA ENABLED/VALIDATED/NO ACTION. Colección CLOB adicional preparada y verificada localmente; Oracle pendiente.
