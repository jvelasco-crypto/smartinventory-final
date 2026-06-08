# Spec — Módulo de Órdenes de Compra

## Descripción
Agregar a SmartInventory la gestión de **órdenes de compra** a proveedores:
crearlas manualmente o de forma automática cuando un producto baja de su stock
mínimo, gestionar su ciclo de vida, y al recibirlas reponer el stock. Incluye
reportes operativos.

## Prerequisitos a construir (no existen en la base)
- **Supplier**: proveedor (id, name, deliveryDays, active).
- **Vínculo Product–Supplier**: un producto puede tener varios proveedores, cada
  uno con su `deliveryDays` (para elegir el más rápido).
- **`minimumStock`** (Integer) en `Product`.
- **`BusinessException`** (RuntimeException) + handler 400 en `GlobalExceptionHandler`.

## Historias de usuario
- Como encargado de inventario, quiero **crear una orden de compra** a un proveedor
  para reponer un producto.
- Como sistema, quiero **generar una orden automática** cuando un producto cae a/por
  debajo de su stock mínimo, eligiendo el proveedor más rápido, para no quedar sin
  stock.
- Como encargado, quiero **avanzar el ciclo de vida** de la orden (enviar, confirmar,
  recibir, cancelar) con validación de transiciones.
- Como encargado, quiero que **al recibir** una orden el **stock se actualice**
  automáticamente.
- Como supervisor, quiero **reportes** de órdenes pendientes y del tiempo promedio
  de entrega por proveedor.

## Reglas de negocio
1. **Ciclo de vida:** `DRAFT → SENT → CONFIRMED → RECEIVED`, y `CANCELLED` desde
   cualquier estado no terminal. `RECEIVED` y `CANCELLED` son terminales.
2. **Orden automática:** al actualizar el stock de un producto y quedar
   `stock ≤ minimumStock`:
   - si **no** hay una orden activa (DRAFT/SENT/CONFIRMED) para ese producto,
   - y el producto tiene al menos un proveedor activo,
   - crear una orden con `automatic=true`, estado `DRAFT`, proveedor de **menor
     `deliveryDays`**, y `quantity = minimumStock × 2`.
   - Si no hay proveedor activo: `log.warn` y no crear.
3. **Recepción (`receive`)**: solo desde `CONFIRMED`. En una transacción:
   cambia a `RECEIVED`, registra `receivedAt`, y **suma `quantity` al stock** del
   producto.
4. **Cancelación**: válida desde DRAFT/SENT/CONFIRMED; requiere `reason`. No se
   puede cancelar una orden `RECEIVED` ni `CANCELLED`.
5. **Transición inválida** → `BusinessException` (HTTP 400).
6. **Orden manual**: el proveedor debe estar vinculado al producto.

## Endpoints (context-path `/api`; los mappings van sin `/api`)
| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/api/purchase-orders` | Sí | Crear orden manual |
| GET | `/api/purchase-orders` | Sí | Listar (paginado) |
| GET | `/api/purchase-orders/{id}` | Sí | Obtener por id |
| PATCH | `/api/purchase-orders/{id}/send` | Sí | DRAFT → SENT |
| PATCH | `/api/purchase-orders/{id}/confirm` | Sí | SENT → CONFIRMED (+ fecha estimada) |
| PATCH | `/api/purchase-orders/{id}/receive` | Sí | CONFIRMED → RECEIVED (+ stock) |
| PATCH | `/api/purchase-orders/{id}/cancel` | Sí | → CANCELLED (+ razón) |
| POST | `/api/suppliers` | Sí | Crear proveedor |
| GET | `/api/suppliers` | Sí | Listar proveedores |
| POST | `/api/products/{id}/suppliers` | Sí | Vincular proveedor a producto (con deliveryDays) |
| GET | `/api/reports/pending-orders` | público | Órdenes pendientes con días transcurridos |
| GET | `/api/reports/supplier-delivery-avg` | público | Tiempo promedio de entrega por proveedor |

## Criterios de aceptación
- Crear orden manual válida → `201` con la orden en `DRAFT`.
- Bajar el stock de un producto bajo su mínimo → se crea **una** orden automática
  (`automatic=true`, `DRAFT`) con el proveedor más rápido y `quantity = minimumStock×2`.
- Recibir una orden `CONFIRMED` → `200`, estado `RECEIVED`, y el stock del producto
  aumenta en `quantity`.
- Cancelar una orden `RECEIVED` → `400` con mensaje de transición inválida.
- Reporte de pendientes lista solo órdenes en SENT/CONFIRMED con sus días transcurridos.

## Casos borde
- Producto sin proveedores activos al disparar el mínimo → no se crea orden (`log.warn`).
- Ya existe orden activa para el producto → no se crea otra automática.
- Confirmar/recibir/enviar fuera de la secuencia → `400` (`BusinessException`).
- Crear orden manual con proveedor no vinculado al producto → `400`.
- Producto o proveedor inexistente → `404` (`ResourceNotFoundException`).
