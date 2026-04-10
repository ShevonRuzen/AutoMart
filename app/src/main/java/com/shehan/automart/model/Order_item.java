package com.shehan.automart.model;


import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Order_item {

    @DocumentId
    private String order_item_doc_id;
    private String order_doc_id;

    private String product_doc_id;
    private double product_price;
    private double qty;
    private double subtotal;

}
