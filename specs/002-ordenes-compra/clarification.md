# Clarification — Órdenes de Compra

Resolución de ambigüedades detectadas en `spec.md`.

### 1. ¿Se puede cancelar una orden RECEIVED?
No. `RECEIVED` es estado terminal. Intentarlo lanza `BusinessException` (HTTP 400)
con mensaje `"Transición inválida: RECEIVED → CANCELLED"`.

### 2. ¿En qué estado se crea la orden automática?
En **`DRAFT`**, igual que una orden manual. El operador decide cuándo enviarla y
confirmarla. No se envía sola.

### 3. ¿Qué pasa si el producto no tiene proveedores activos al caer bajo el mínimo?
**No se crea** la orden. Se registra `log.warn` (`"Sin proveedor activo para
producto id=..."`). **No se lanza excepción**: el flujo de actualización de stock
no debe fallar por esto.

### 4. ¿Qué pasa si ya existe una orden activa para el producto?
No se crea otra automática. Se considera **orden activa** la que está en `DRAFT`,
`SENT` o `CONFIRMED`. Se verifica con
`existsByProductIdAndStatusIn(productId, [DRAFT, SENT, CONFIRMED])`.

### 5. ¿La cantidad sugerida es configurable?
No en esta versión: es fija, `quantity = minimumStock × 2`. Se deja documentado
como punto de extensión futuro (p. ej. una propiedad), pero no se implementa ahora
para mantener el alcance.

### 6. ¿Dónde y cómo se dispara la orden automática?
En `ProductService`, **después de persistir un cambio de stock**: si el producto
queda con `stock ≤ minimumStock`, se llama a
`purchaseOrderService.createAutomaticIfNeeded(product)`.
Decisión de alcance: **no** se implementa un sistema separado de `StockAlert`; el
disparo va directo en el flujo de stock.

### 7. ¿La recepción resuelve alguna alerta de stock?
La base no tiene `StockAlert`, así que `receive` **solo suma stock** al producto.
(Si en el futuro se agregaran alertas, `receive` las resolvería; fuera de alcance.)

### 8. ¿La fecha estimada es obligatoria al confirmar?
Sí. `estimatedDeliveryDate` es `@NotNull` en el DTO de confirmación; sin ella → 400
de validación.

### 9. ¿Validaciones de la orden manual?
- `quantity`: `@NotNull @Min(1)`.
- El proveedor debe estar **vinculado** al producto; si no → `BusinessException` (400).
- Producto o proveedor inexistente → `ResourceNotFoundException` (404).

### 10. ¿Cómo se calcula el "tiempo promedio de entrega" del reporte?
Diferencia en días entre `createdAt` y `receivedAt`, solo de órdenes en estado
`RECEIVED`, agrupada (promedio) por proveedor.
