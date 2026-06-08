# Plan Técnico — Órdenes de Compra

## 1. Modelo de datos

### `Supplier` (tabla `suppliers`)
| Campo | Tipo | Notas |
|---|---|---|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| name | String | obligatorio |
| deliveryDays | Integer | días de entrega (para elegir el más rápido) |
| active | Boolean | |

### `Product` (modificar)
- Agregar `minimumStock: Integer`.
- Agregar relación `@ManyToMany` con `Supplier`: `Set<Supplier> suppliers`
  (tabla intermedia `product_suppliers`). El "proveedor más rápido" del producto =
  el de menor `deliveryDays` entre sus proveedores activos.
  > Decisión de alcance: usar `@ManyToMany` simple en vez de una entidad
  > `ProductSupplier` con atributos. Suficiente para elegir el proveedor más rápido.

### `PurchaseOrder` (tabla `purchase_orders`)
| Campo | Tipo | Notas |
|---|---|---|
| id | Long | @Id @GeneratedValue(IDENTITY) |
| product | Product | @ManyToOne(FetchType.LAZY) |
| supplier | Supplier | @ManyToOne(FetchType.LAZY) |
| quantity | Integer | |
| status | OrderStatus | @Enumerated(EnumType.STRING) |
| automatic | boolean | true si fue generada automáticamente |
| reason | String | motivo de cancelación (nullable) |
| createdAt | LocalDateTime | @CreationTimestamp |
| estimatedDeliveryDate | LocalDate | nullable, se setea al confirmar |
| receivedAt | LocalDateTime | nullable, se setea al recibir |

### `OrderStatus` (enum)
`DRAFT, SENT, CONFIRMED, RECEIVED, CANCELLED`

## 2. Máquina de estados
| Desde | Hacia válido |
|---|---|
| DRAFT | SENT, CANCELLED |
| SENT | CONFIRMED, CANCELLED |
| CONFIRMED | RECEIVED, CANCELLED |
| RECEIVED | — (terminal) |
| CANCELLED | — (terminal) |

`validateTransition(from, to)`: si `to` no está permitido para `from` →
`BusinessException("Transición inválida: " + from + " → " + to)` (HTTP 400).

## 3. Disparo de orden automática
En `ProductService`, tras guardar un producto cuyo stock cambió:
```
if (product.getMinimumStock() != null && product.getStock() <= product.getMinimumStock())
    purchaseOrderService.createAutomaticIfNeeded(product);
```
`createAutomaticIfNeeded(product)`:
1. Si `existsByProductIdAndStatusIn(id, [DRAFT,SENT,CONFIRMED])` → return.
2. Elegir proveedor activo con menor `deliveryDays` entre `product.getSuppliers()`.
   Si no hay → `log.warn` y return.
3. Crear `PurchaseOrder(product, supplier, quantity=minimumStock*2, DRAFT, automatic=true)` y guardar.

> Inyectar `PurchaseOrderService` en `ProductService` con `@RequiredArgsConstructor`.
> (Si hay riesgo de dependencia circular, usar `@Lazy`.)

## 4. Recepción (`receive`) — `@Transactional`
1. Cargar la orden; `validateTransition(status, RECEIVED)` (debe venir de CONFIRMED).
2. `status = RECEIVED`, `receivedAt = now`.
3. `product.stock += order.quantity`; guardar producto.
4. Guardar orden.

## 5. Archivos

**Nuevos**
- `enums/OrderStatus.java`
- `model/Supplier.java`, `model/PurchaseOrder.java`
- `exception/BusinessException.java`
- `dto/request/`: `PurchaseOrderRequestDTO`, `PurchaseOrderConfirmDTO`, `PurchaseOrderCancelDTO`, `SupplierRequestDTO`
- `dto/response/`: `PurchaseOrderResponseDTO`, `SupplierResponseDTO`, `PendingOrderReportDTO`, `SupplierDeliveryAvgDTO`
- `repository/`: `PurchaseOrderRepository`, `SupplierRepository`
- `service/`: `PurchaseOrderService`, `SupplierService`
- `controller/`: `PurchaseOrderController`, `SupplierController`, `ReportController`

**Modificados**
- `model/Product.java` (+ `minimumStock`, + `suppliers`)
- `service/ProductService.java` (disparo automático en update de stock)
- `exception/GlobalExceptionHandler.java` (+ handler `BusinessException` → 400)

## 6. Queries (`PurchaseOrderRepository`)
- `boolean existsByProductIdAndStatusIn(Long productId, List<OrderStatus> statuses)`
- `Page<PurchaseOrder> findAllByOrderByCreatedAtDesc(Pageable pageable)`
- `@Query` pendientes: `status IN (SENT, CONFIRMED) ORDER BY createdAt`
- Promedio de entrega por proveedor: **calcular en el Service** (cargar órdenes
  `RECEIVED` y promediar días `createdAt→receivedAt` por proveedor) para evitar
  funciones de fecha específicas del motor. JPQL solo trae las RECEIVED.
