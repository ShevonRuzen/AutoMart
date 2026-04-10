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
public class Notification {

    @DocumentId
    private String notification_doc_id;
    private String user_doc_id;
    private String order_doc_id;
    private String order_id;
    private String title;
    private String message;
    private String status;
    private Timestamp created_at;
    private boolean read;


}
