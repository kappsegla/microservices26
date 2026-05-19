package org.example.stockservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stocks")
public class Stock {
    @Id
    private String product;
    private Integer quantity;

    public Stock() {}

    public Stock(String product, Integer quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
