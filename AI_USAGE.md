# Uso de IA en el Proyecto Final

## Herramienta utilizada
**Claude Code** (agente de IA en terminal) con metodología **SDD (Spec-Driven
Development)**: primero se generaron los artefactos (los "planos") y recién después
el agente implementó el código tarea por tarea, revisando cada cambio.

## Flujo SDD aplicado

### Artefactos generados con IA (y revisados)
- **`constitution.md`**: reglas no negociables del proyecto (stack Boot 4.0.6 /
  Java 25, paquete `ec.org.cedia.smartinventory`, context-path `/api`, Basic auth,
  no romper el MCP existente, `@MockitoBean` en vez de `@MockBean`). Se ajustó para
  que reflejara EXACTAMENTE las convenciones de la base oficial del instructor (no
  las del proyecto de prácticas, que usaba otro paquete).
- **`spec.md` / `clarification.md`**: requerimientos del módulo, historias de
  usuario, reglas de negocio y casos borde. Se resolvieron ambigüedades (cancelar
  una orden RECEIVED, estado de la orden automática, producto sin proveedores, etc.).
- **`plan.md`**: modelo de datos, máquina de estados y queries. Decisión propia:
  usar `@ManyToMany` Product–Supplier (más simple) en vez de una entidad
  `ProductSupplier` con atributos.
- **`tasks.md`**: 9 tareas ordenadas por dependencia.

## Implementación

### Lo que el agente generó correctamente sin corrección
- Enum `OrderStatus` y entidad `PurchaseOrder` con relaciones `@ManyToOne(LAZY)`.
- Máquina de estados (`validateTransition`) con el mapa de transiciones correcto.
- `receive()` con `@Transactional` que cambia estado y suma stock en una sola
  transacción (efecto sobre dos tablas).
- `createAutomaticIfNeeded`: no duplica órdenes activas, elige el proveedor con
  menor `deliveryDays`, cantidad `minimumStock × 2`, `log.warn` si no hay proveedor.
- Tests con `@WebMvcTest` + `@MockitoBean` (no `@MockBean`, eliminado en Boot 4).

### Lo que el agente generó incorrectamente y cómo se corrigió
- **Campo `minimumStock` no expuesto:** el agente agregó `minimumStock` a la entidad
  `Product`, pero olvidó incluirlo en `ProductRequestDTO` y mapearlo en
  `ProductService`. Resultado: `minimumStock` quedaba `null` y la **orden automática
  nunca se disparaba**. Se detectó al probar el flujo y se corrigió (campo en el DTO
  + mapeo en create y update).
- **Swagger sin botón "Authorize":** `OpenApiConfig` no declaraba esquema de
  seguridad, así que no se podían ejecutar los endpoints protegidos desde Swagger UI
  (401). Se agregó el `SecurityScheme` HTTP Basic al OpenAPI.

### Decisiones que tomé yo (que el agente no podía tomar solo)
- **Alcance pragmático:** la guía asumía módulos (StockAlert, StockMovement,
  AuditableEntity, JWT) que la base oficial NO tenía. Decidí construir solo lo
  mínimo necesario (`Supplier`, `minimumStock`, `BusinessException`) y disparar la
  orden automática directamente en el flujo de stock, sin reconstruir un sistema
  completo de alertas/movimientos.
- **Cantidad sugerida fija** (`minimumStock × 2`) en vez de configurable.
- **Mantener las convenciones de la base** (paquete `ec.org.cedia`, context-path,
  Basic auth) y **no romper el MCP** existente.

## Qué aprendí sobre trabajar con agentes
El SDD ordena mucho el trabajo: tener spec/plan/tasks como "contrato" hace que el
agente genere código consistente y que sea fácil revisarlo tarea por tarea. Aun así,
el agente comete omisiones sutiles (como no exponer un campo en el DTO) que solo se
detectan **probando el flujo real** — la supervisión humana y los tests siguen siendo
imprescindibles. La IA acelera enormemente el boilerplate, pero las decisiones de
arquitectura y de alcance las toma el desarrollador.
