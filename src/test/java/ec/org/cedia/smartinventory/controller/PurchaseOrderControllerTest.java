package ec.org.cedia.smartinventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.org.cedia.smartinventory.config.SecurityConfig;
import ec.org.cedia.smartinventory.dto.request.PurchaseOrderCancelDTO;
import ec.org.cedia.smartinventory.dto.request.PurchaseOrderRequestDTO;
import ec.org.cedia.smartinventory.dto.response.PurchaseOrderResponseDTO;
import ec.org.cedia.smartinventory.enums.OrderStatus;
import ec.org.cedia.smartinventory.exception.BusinessException;
import ec.org.cedia.smartinventory.exception.GlobalExceptionHandler;
import ec.org.cedia.smartinventory.service.PurchaseOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PurchaseOrderController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PurchaseOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PurchaseOrderService purchaseOrderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────────────
    // POST /purchase-orders con autenticación → 201
    // ─────────────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void crear_conAuth_retorna201() throws Exception {
        PurchaseOrderRequestDTO dto = new PurchaseOrderRequestDTO();
        dto.setProductId(1L);
        dto.setSupplierId(1L);
        dto.setQuantity(10);

        PurchaseOrderResponseDTO response = PurchaseOrderResponseDTO.builder()
                .id(1L)
                .productId(1L)
                .productName("Laptop")
                .supplierId(1L)
                .supplierName("Proveedor A")
                .quantity(10)
                .status(OrderStatus.DRAFT)
                .automatic(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(purchaseOrderService.create(any())).thenReturn(response);

        mockMvc.perform(post("/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.automatic").value(false));
    }

    // ─────────────────────────────────────────────────────────────────────
    // POST /purchase-orders sin autenticación → 401
    // ─────────────────────────────────────────────────────────────────────
    @Test
    void crear_sinAuth_retorna401() throws Exception {
        PurchaseOrderRequestDTO dto = new PurchaseOrderRequestDTO();
        dto.setProductId(1L);
        dto.setSupplierId(1L);
        dto.setQuantity(10);

        mockMvc.perform(post("/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────────────
    // PATCH /purchase-orders/{id}/cancel cuando transición inválida → 400
    // ─────────────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cancel_transicionInvalida_retorna400() throws Exception {
        when(purchaseOrderService.cancel(anyLong(), any()))
                .thenThrow(new BusinessException("Transición inválida: RECEIVED → CANCELLED"));

        PurchaseOrderCancelDTO dto = new PurchaseOrderCancelDTO();
        dto.setReason("Intento de cancelación de orden ya recibida");

        mockMvc.perform(patch("/purchase-orders/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Transición inválida: RECEIVED → CANCELLED"));
    }
}
