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
public class Order {
    @DocumentId
    private String order_doc_id;
    private String user_doc_id;

    private String order_id;
    private Timestamp created_date;
    private String notes;
    private String payment_status;
    private String order_status;

    private String address_home;
    private String address_city;
    private String address_line_1;
    private String address_line_2;
    private String address_postal_code;
    private double total_amount;
    private double shipping_fee;

}
