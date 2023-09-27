package com.jivanpun.suitcaseapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class ItemActions {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Context context;

    public ItemActions(Context context, FirebaseAuth auth, FirebaseFirestore db) {
        this.context = context;
        this.auth = auth;
        this.db = db;
    }

    // Update the editItem method
    public void editItem(Map<String, Object> itemData, String editedName, String editedDescription, String editedPrice, Uri newImageUri) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            DocumentReference itemRef = (DocumentReference) itemData.get("docRef");
            Map<String, Object> updatedData = new HashMap<>();
            updatedData.put("Items Name", editedName);
            updatedData.put("notes", editedDescription);
            updatedData.put("price", editedPrice);

            // Check if a new image is selected
            if (newImageUri != null) {
                // Upload the new image to Firebase Storage
                uploadImageToFirebaseStorage(newImageUri, itemRef, updatedData);
            } else {
                // If no new image is selected, update only the item details
                itemRef.update(updatedData)
                        .addOnSuccessListener(aVoid -> {
                            // Handle edit success
                            showToast("Item edited successfully!");
                        })
                        .addOnFailureListener(e -> {
                            // Handle edit failure
                            showToast("Failed to edit item: " + e.getMessage());
                        });
            }
        }
    }

    // Add the uploadImageToFirebaseStorage method
    private void uploadImageToFirebaseStorage(Uri imageUri, DocumentReference itemRef, Map<String, Object> updatedData) {
        String currentUserId = auth.getCurrentUser().getUid();
        String imageName = "item_image_" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("item_images")
                .child(currentUserId)
                .child(imageName);

        UploadTask uploadTask = storageRef.putFile(imageUri);

        uploadTask.addOnCompleteListener((Executor) this, task -> {
            if (task.isSuccessful()) {
                // Get the download URL of the uploaded image
                storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    // Add the new image URL to the item's data
                    updatedData.put("imageUrl", downloadUri.toString());

                    // Update the item's data in Firestore with the new image URL
                    itemRef.update(updatedData)
                            .addOnSuccessListener(aVoid -> {
                                // Handle edit success
                                showToast("Item edited successfully!");
                            })
                            .addOnFailureListener(e -> {
                                // Handle edit failure
                                showToast("Failed to edit item: " + e.getMessage());
                            });
                });
            } else {
                // Handle image upload failure
                showToast("Failed to upload image: " + task.getException().getMessage());
            }
        });
    }

    // Example of how to call editItem when editing an item
    private void editItemExample(Map<String, Object> itemData, String editedName, String editedDescription, String editedPrice, Uri newImageUri) {
        editItem(itemData, editedName, editedDescription, editedPrice, newImageUri);
    }


    public void deleteItem(Map<String, Object> itemData) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            DocumentReference itemRef = (DocumentReference) itemData.get("docRef");

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Confirm Delete");
            builder.setMessage("Are you sure you want to delete this item?");
            builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    itemRef.delete()
                            .addOnSuccessListener(aVoid -> {
                                // Handle delete success
                                showToast("Item deleted successfully!");
                            })
                            .addOnFailureListener(e -> {
                                // Handle delete failure
                                showToast("Failed to delete item: " + e.getMessage());
                            });
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
