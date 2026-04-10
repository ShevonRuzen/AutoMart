package com.shehan.automart.model;


import com.google.firebase.firestore.DocumentId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Address {

    @DocumentId
    private String address_doc_id;
    private String user_id;
    private String home_name;
    private String address_line1;
    private String address_line2;
    private String city;
    private String postal_code;
    private boolean checked;
    private double shippingFee;


}
