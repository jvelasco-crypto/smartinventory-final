package ec.org.cedia.smartinventory.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity                                // ← NUEVO
@Table(name = "products")             // ← NUEVO
public class Product {

    @Id                                // ← NUEVO
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // ← NUEVO
    private Long id;

    private String name;
    
    // @Transient
    private String description;

    private Double price;
    @JsonIgnore
    private Integer stock;
    private Boolean active;
    @Column(name = "is_available")  // ← NUEVO
    private String isAvailable;

    private Integer minimumStock;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_suppliers",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "supplier_id")
    )
    @Builder.Default
    private Set<Supplier> suppliers = new HashSet<>();
}