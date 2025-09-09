package ru.yandex.practicum.market.dao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.List;

@Entity
@Table(name = "items")
@Getter
@Setter
public class ItemEntity {

    public ItemEntity() {
    }

    public ItemEntity(Long id, String name, String description, BigDecimal price, byte[] image, Integer quantity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.image = image;
        this.quantity = quantity;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price")
    private BigDecimal price;

    @Lob
    @JdbcTypeCode(Types.VARBINARY)
    @Column(name = "image")
    private byte[] image;

    @Transient
    private Integer quantity;

    public String getImageBase64() {
        if (image != null) {
            return java.util.Base64.getEncoder().encodeToString(image);
        }
        return null;
    }

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> orderItems;
}