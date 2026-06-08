# Tasks — Órdenes de Compra

Ordenadas por dependencia. **Compilar (`./mvnw compile`) tras cada task** y hacer
commit por avance. Paquete `ec.org.cedia.smartinventory`. Mappings SIN `/api`.

## TASK-01 — Enum OrderStatus
- `enums/OrderStatus.java`: `DRAFT, SENT, CONFIRMED, RECEIVED, CANCELLED`
  (crear el paquete `ec.org.cedia.smartinventory.enums`).

## TASK-02 — Prerequisitos
- `model/Supplier.java` (`@Entity @Table("suppliers")`, Lombok): id, name,
  deliveryDays:Integer, active:Boolean.
- `model/Product.java` (modificar): agregar `minimumStock:Integer` y
  `@ManyToMany Set<Supplier> suppliers` (tabla `product_suppliers`). **No tocar**
  los campos existentes (incluido `isAvailable`).
- `exception/BusinessException.java extends RuntimeException`.
- `exception/GlobalExceptionHandler.java`: `@ExceptionHandler(BusinessException)`
  → 400 con JSON `{timestamp,status,error}` + `log.warn`.

## TASK-03 — Entidad PurchaseOrder
- `model/PurchaseOrder.java` según `plan.md`: product/supplier `@ManyToOne(LAZY)`,
  quantity, status `@Enumerated(STRING)`, automatic:boolean, reason (nullable),
  createdAt `@CreationTimestamp`, estimatedDeliveryDate:LocalDate (nullable),
  receivedAt:LocalDateTime (nullable).

## TASK-04 — DTOs
- request: `PurchaseOrderRequestDTO` (productId, supplierId, quantity `@NotNull @Min(1)`),
  `PurchaseOrderConfirmDTO` (estimatedDeliveryDate `@NotNull` LocalDate),
  `PurchaseOrderCancelDTO` (reason `@NotBlank @Size(max=500)`),
  `SupplierRequestDTO` (name `@NotBlank`, deliveryDays `@NotNull @Min(0)`).
- response: `PurchaseOrderResponseDTO` (id, productId, productName, supplierId,
  supplierName, quantity, status, automatic, reason, createdAt,
  estimatedDeliveryDate, receivedAt), `SupplierResponseDTO`,
  `PendingOrderReportDTO` (orderId, productName, supplierName, quantity, status,
  daysPending, estimatedDeliveryDate), `SupplierDeliveryAvgDTO` (supplierId,
  supplierName, avgDeliveryDays, totalReceived).

## TASK-05 — Repositorios
- `SupplierRepository extends JpaRepository<Supplier,Long>`.
- `PurchaseOrderRepository extends JpaRepository<PurchaseOrder,Long>`:
  - `boolean existsByProductIdAndStatusIn(Long productId, List<OrderStatus> statuses)`
  - `Page<PurchaseOrder> findAllByOrderByCreatedAtDesc(Pageable pageable)`
  - `@Query` pendientes: `status IN (SENT, CONFIRMED) ORDER BY createdAt` → List
  - `List<PurchaseOrder> findByStatus(OrderStatus status)` (para el reporte avg)

## TASK-06 — Servicios
- `SupplierService` (`@Slf4j @RequiredArgsConstructor`): crear, listar, y vincular
  proveedor a producto.
- `PurchaseOrderService` (`@Slf4j @RequiredArgsConstructor`):
  - `create(dto)`: producto/proveedor existen (404), proveedor vinculado al
    producto (si no → `BusinessException` 400); crea `DRAFT`, `automatic=false`.
  - `createAutomaticIfNeeded(Product)`: si hay orden activa → return; proveedor
    activo con menor `deliveryDays` (si no hay → `log.warn`, return);
    `quantity = minimumStock*2`, `DRAFT`, `automatic=true`.
  - `send/confirm/receive/cancel`: `validateTransition` primero; `receive`
    `@Transactional` suma `quantity` al stock del producto.
  - `validateTransition(from,to)`: mapa de transiciones (ver `plan.md`); inválida →
    `BusinessException`.
  - `list(Pageable)`, `getById(Long)` (404), `getPendingOrdersReport()`,
    `getSupplierDeliveryAvg()` (promedio calculado en Java).
  - `log.info` en éxitos, `log.warn` en casos esperados.

## TASK-07 — Disparo automático en ProductService
- Inyectar `PurchaseOrderService` (`@RequiredArgsConstructor`; usar `@Lazy` si hay
  dependencia circular).
- En el método que actualiza el stock del producto: tras guardar, si
  `stock ≤ minimumStock` → `createAutomaticIfNeeded(product)`. No cambiar otra lógica.

## TASK-08 — Controllers
- `SupplierController` (`@RequestMapping("/suppliers")`, `@Tag`): POST crear, GET
  listar; endpoint para vincular proveedor a producto (`POST /products/{id}/suppliers`).
- `PurchaseOrderController` (`@RequestMapping("/purchase-orders")`, `@Tag("Órdenes de Compra")`):
  POST crear, GET listar (Pageable), GET `/{id}`, PATCH `/{id}/send`, `/confirm`,
  `/receive`, `/cancel`. `@Operation` + `@ApiResponse` en cada método.
- `ReportController` (`@RequestMapping("/reports")`, `@Tag("Reportes")`): GET
  `/pending-orders`, GET `/supplier-delivery-avg`. **Públicos** → agregar
  `/reports/**` al `permitAll()` de `SecurityConfig`.

## TASK-09 — Tests
- `PurchaseOrderServiceTest` (`@ExtendWith(MockitoExtension.class)`):
  - `createAutomaticIfNeeded_sinOrdenActiva_creaDraft`
  - `createAutomaticIfNeeded_conOrdenActiva_noCrea`
  - `createAutomaticIfNeeded_sinProveedor_noCrea`
  - `receive_sumaStockYMarcaRecibida`
  - `cancel_sobreRecibida_lanzaBusinessException`
- `PurchaseOrderControllerTest` (`@WebMvcTest` + `@MockitoBean` + `@Import(SecurityConfig)`):
  - `crear_conAuth_retorna201`
  - `crear_sinAuth_retorna401`
  - `cancel_transicionInvalida_retorna400`
- `./mvnw test` → verde.
