package com.shehan.automart.model;

import com.google.firebase.firestore.DocumentId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartItems {

    @DocumentId
    private String docId;

    private String cart_id;
    private String product_id;
    private double qty;

}
