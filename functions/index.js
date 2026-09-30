const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.orderStatusChanged = onDocumentUpdated("order/{orderId}", async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();

  if (!before || !after) return;

  const oldStatus = before.order_status;
  const newStatus = after.order_status;

  // only run if status changed
  if (oldStatus === newStatus) return;

  const userId = after.user_doc_id;
  if (!userId) return;

  // get user document
  const userDoc = await admin.firestore()
    .collection("user")
    .doc(userId)
    .get();

  if (!userDoc.exists) return;

  const userData = userDoc.data();
  const token = userData.fcm_token;

  if (!token) return;

  let message = "Order updated";

  switch (newStatus) {
    case "PENDING":
      message = "Your order has been placed successfully.";
      break;
    case "PROCESSING":
      message = "Your order is now being processing.";
      break;
    case "PICKED_UP":
      message = "Your order is on the way.";
      break;
    case "DELIVERED":
      message = "Your order has been delivered.";
      break;
    default:
      message = `Your order status changed to ${newStatus}`;
  }

  // send FCM
  await admin.messaging().send({
    token: token,
    notification: {
      title: "Order Status Updated",
      body: message,
    },
    android: {
      priority: "high",
      notification: {
        channelId: "order_status_channel",
        sound: "default",
      },
    },
    data: {
      order_doc_id: event.params.orderId,
      status: newStatus,
    },
  });

//  save
  await admin.firestore().collection("notifications").add({
    user_doc_id: userId,
    order_doc_id: event.params.orderId,
    order_id: after.order_id || "",
    title: "Order Status Updated",
    message: message,
    status: newStatus,
    created_at: admin.firestore.FieldValue.serverTimestamp(),
    read: false,
  });

  console.log("Notification sent successfully");
});


