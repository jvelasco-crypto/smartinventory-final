# Constitución — SmartInventory (Proyecto Final)

Reglas estrictas y NO negociables que la IA debe respetar al implementar
cualquier feature de este proyecto.

## Stack (fijo)
- Java 25, Spring Boot 4.0.6, Maven (usar `./mvnw`).
- Spring Web MVC, Spring Data JPA / Hibernate.
- PostgreSQL (dev/prod), H2 (tests).
- Lombok.
- springdoc-openapi 3.0.3 (Swagger/OpenAPI).
- Spring Security con **HTTP Basic**.
- MCP (`io.modelcontextprotocol`, mcp-bom 1.1.2) — servidor MCP existente en `/mcp`.

## Convenciones (no negociables)
- **Paquete raíz:** `ec.org.cedia.smartinventory`. Nunca `com.cedia` ni otro.
- **context-path = `/api`** (`server.servlet.context-path=/api`). Por lo tanto:
  - Los `@RequestMapping` van **SIN** `/api` (ej. `@RequestMapping("/purchase-orders")`
    se expone como `/api/purchase-orders`).
  - Los matchers de `SecurityConfig` van **SIN** `/api`.
- **Puerto:** 8090.
- **Arquitectura por capas:** controller → service → repository → entity. DTOs
  separados (request/response); nunca exponer entidades JPA directamente.
- **Excepciones tipadas:** `ResourceNotFoundException` (404) y `BusinessException`
  (400), manejadas en `GlobalExceptionHandler` (`@RestControllerAdvice`) con JSON
  estructurado (`timestamp`, `status`, `error`, y `fields` en validaciones).
- **Validaciones:** `jakarta.validation` con `@Valid` en los controllers.
- **Lombok:** `@Data/@Builder/@NoArgsConstructor/@AllArgsConstructor` en
  entidades/DTOs; `@RequiredArgsConstructor` + `@Slf4j` en servicios.
- **Logging SLF4J:** INFO en operaciones exitosas, WARN en casos esperados
  (no encontrado, sin proveedor), ERROR en fallos no controlados.
- **Swagger:** anotar controllers con `@Tag`, `@Operation`, `@ApiResponse`.
- **Auth:** HTTP Basic (`admin`/`admin123`, rol ADMIN). GET de catálogo, `/health`,
  Swagger y `/mcp` públicos; el resto autenticado.

## Perfiles
- `dev` (activo por defecto): PostgreSQL, `ddl-auto=update`, logging DEBUG.
- `prod`: variables sin defaults, `ddl-auto=validate`.
- `test`: H2 en memoria.

## Reglas para módulos nuevos
- `@Transactional` en operaciones con efectos múltiples (ej. recibir orden:
  cambia estado + suma stock en una sola transacción).
- Máquina de estados explícita; transición inválida → `BusinessException` (400).
- Tests: Service con Mockito; Controller con `@WebMvcTest` + **`@MockitoBean`**
  (NO `@MockBean`, deprecado en Boot 4).

## Lo que NO se debe hacer
- No cambiar el paquete (`ec.org.cedia.smartinventory`).
- No migrar a JWT/OAuth (se mantiene Basic).
- No agregar `context-path` en los mappings ni duplicar `/api`.
- No romper el módulo MCP existente (`McpConfig`, `McpToolService`, endpoint `/mcp`).
- No exponer entidades JPA directamente en los endpoints.
- No usar `@MockBean`.
- No sobre-construir: para Órdenes de Compra crear solo lo mínimo necesario
  (`Supplier`, `minimumStock`, `BusinessException`), sin un sistema completo de
  alertas/movimientos.
