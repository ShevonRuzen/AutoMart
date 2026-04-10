package com.shehan.automart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    private String id;
    private String name;
    private String description;
    private String category_id;
    private String status_id;
    private String image;
    private String color_id;
    private double price;
}
