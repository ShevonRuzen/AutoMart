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
public class SpecialMenu {
    @DocumentId
    private String special_menu_doc_id;
    private String product_doc_id;

}