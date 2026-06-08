package ec.org.cedia.smartinventory.repository;

import ec.org.cedia.smartinventory.enums.OrderStatus;
import ec.org.cedia.smartinventory.model.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    boolean existsByProductIdAndStatusIn(Long productId, List<OrderStatus> statuses);

    Page<PurchaseOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT o FROM PurchaseOrder o WHERE o.status IN ('SENT', 'CONFIRMED') ORDER BY o.createdAt")
    List<PurchaseOrder> findPendingOrders();

    List<PurchaseOrder> findByStatus(OrderStatus status);
}
