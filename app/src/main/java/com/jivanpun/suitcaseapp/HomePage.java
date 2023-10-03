package com.jivanpun.suitcaseapp;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomePage extends AppCompatActivity {
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private FirebaseFirestore db;
    private ItemsAdapter itemsAdapter;
    final List<Map<String, Object>> itemsList = new ArrayList<>();
    private ListenerRegistration itemsListener;
    private SwipeRefreshLayout swipeRefreshLayout;
    private static final int GALLERY_REQUEST_CODE = 100;
    private Dialog customDialog;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ShakeDetector shakeDetector; // Add this line
    private ImageView itemImageView;
    private ImageView editItemImageView;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        auth = FirebaseAuth.getInstance();
        googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);
        db = FirebaseFirestore.getInstance();
        setUpAppBar();
        FloatingActionButton floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(v -> showCustomDialog());

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        itemsAdapter = new ItemsAdapter(itemsList);
        recyclerView.setAdapter(itemsAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition(); // Use getBindingAdapterPosition() here
                if (direction == ItemTouchHelper.LEFT) {
                    deleteItem(position);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    editItem(position);
                }
            }

        });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);

        // Initialize ShakeDetector
        shakeDetector = new ShakeDetector(this);
        shakeDetector.setOnShakeListener(() -> clearInputFields());

        // Initialize the galleryLauncher
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (itemImageView != null) {
                            // Set the selected image for adding a new item
                            itemImageView.setImageURI(selectedImageUri);
                            imageUri = selectedImageUri;
                        } else if (editItemImageView != null) {
                            // Set the selected image for editing an item
                            editItemImageView.setImageURI(selectedImageUri);
                            // Store the selected image URI for editing
                            // You may need to update this logic based on your use case
                            imageUri = selectedImageUri;
                        }
                    }
                });


    }

    // Inside your HomePage class
    private void clearInputFields() {
        // Assuming your custom dialog has EditText fields, clear them here
        androidx.appcompat.widget.AppCompatEditText inputDesName = customDialog.findViewById(R.id.nameEditText);
        androidx.appcompat.widget.AppCompatEditText inputNote = customDialog.findViewById(R.id.descriptionEditText);
        androidx.appcompat.widget.AppCompatEditText inputPrice = customDialog.findViewById(R.id.priceEditText);

        // Clear the EditText fields
        inputDesName.setText("");
        inputNote.setText("");
        inputPrice.setText("");
    }


    @Override
    protected void onStart() {
        super.onStart();
        startItemsListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopItemsListener();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void startItemsListener() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            Query itemsQuery = db.collection("Items")
                    .whereEqualTo("userId", currentUserId);

            itemsListener = itemsQuery.addSnapshotListener((queryDocumentSnapshots, e) -> {
                if (e != null) {
                    Toast.makeText(HomePage.this, "Error fetching items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                    return;
                }
                itemsList.clear();
                assert queryDocumentSnapshots != null;
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    Map<String, Object> itemData = documentSnapshot.getData();
                    itemData.put("docRef", documentSnapshot.getReference());
                    itemsList.add(itemData);
                }
                itemsAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            });
        }
    }

    private void stopItemsListener() {
        if (itemsListener != null) {
            itemsListener.remove();
            itemsListener = null;
        }
    }

    private void setUpAppBar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_profile) {
            return true;
        } else if (id == R.id.action_logout) {
            showLogoutConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCustomDialog() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
            dialogBuilder.setTitle("Add Item");

            View dialogView = getLayoutInflater().inflate(R.layout.additem_menu, null);
            dialogBuilder.setView(dialogView);
            itemImageView = dialogView.findViewById(R.id.imageViewItem);
            TextView chooseImageTextView = dialogView.findViewById(R.id.chooseImage);
            TextInputEditText inputDesName = dialogView.findViewById(R.id.nameEditText);
            TextInputEditText inputNote = dialogView.findViewById(R.id.descriptionEditText);
            TextInputEditText inputPrice = dialogView.findViewById(R.id.priceEditText);

            chooseImageTextView.setOnClickListener(v -> {
                // Launch the gallery to select an image
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(intent);
            });

            dialogBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String itemName = inputDesName.getText().toString().trim();
                    String note = inputNote.getText().toString().trim();
                    String itemPrice = inputPrice.getText().toString().trim();
                    if (itemName.isEmpty() || note.isEmpty() || itemPrice.isEmpty() || imageUri == null) {
                        Toast.makeText(HomePage.this, "Item name, description, price, and image are required!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Check if an image has been selected
                        uploadImageToFirebaseStorage(imageUri, itemName, note, itemPrice);
                    }
                }
            });

            dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });

            AlertDialog customDialog = dialogBuilder.create();
            customDialog.show();
        }
    }





    private Uri imageUri;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // Get the selected image URI
            Uri selectedImageUri = data.getData();

            // Display the selected image in the ImageView
            itemImageView.setImageURI(selectedImageUri);

            // Store the selected image URI in a member variable
            imageUri = selectedImageUri;
        }

    }

    private void uploadImageToFirebaseStorage(Uri imageUri, String itemName, String note, String itemPrice) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            String imageName = "item_image_" + System.currentTimeMillis() + ".jpg";
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                    .child("item_images")
                    .child(currentUserId)
                    .child(imageName);

            UploadTask uploadTask = storageRef.putFile(imageUri);

            uploadTask.addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> saveItemToFirestore(downloadUri.toString(), itemName, note, itemPrice));
                } else {
                    Toast.makeText(HomePage.this, "Failed to upload image: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void saveItemToFirestore(String imageUrl, String itemName, String note, String itemPrice) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            Map<String, Object> itemsData = new HashMap<>();
            itemsData.put("Items Name", itemName);
            itemsData.put("notes", note);
            itemsData.put("price", itemPrice);
            itemsData.put("imageUrl", imageUrl);
            itemsData.put("userId", currentUserId);
            itemsData.put("purchased", false);

            db.collection("Items")
                    .add(itemsData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(HomePage.this, "Item added!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(HomePage.this, "Failed to add item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Logout");
        builder.setMessage("Are you sure you want to log out?");
        builder.setPositiveButton("Logout", (dialogInterface, i) -> googleSignInClient.signOut().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                auth.signOut();
                Toast.makeText(getApplicationContext(), "Logged out Successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        }));
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());
        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            positiveButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            negativeButton.setTextColor(getResources().getColor(android.R.color.black));
        });
        dialog.show();
    }

    private void editItem(int position) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            Map<String, Object> itemData = itemsList.get(position);
            String itemName = (String) itemData.get("Items Name");
            String itemNote = (String) itemData.get("notes");
            String itemPrice = (String) itemData.get("price");
            String itemImage = (String) itemData.get("imageUrl");

            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
            dialogBuilder.setTitle("Edit Item");

            View dialogView = getLayoutInflater().inflate(R.layout.edit_item_dialog, null);
            dialogBuilder.setView(dialogView);

            AppCompatEditText editNameEditText = dialogView.findViewById(R.id.nameEditText);
            AppCompatEditText editDescriptionEditText = dialogView.findViewById(R.id.descriptionEditText);
            AppCompatEditText editPriceEditText = dialogView.findViewById(R.id.priceEditText);
            TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
            editItemImageView = dialogView.findViewById(R.id.imageViewEdit);

            titleTextView.setOnClickListener(v -> {
                // Launch the gallery to select a new image
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(intent);
            });

            // Load and display the image using Glide
            Glide.with(this)
                    .load(itemImage)
                    .placeholder(R.drawable.image_placeholder) // Placeholder image while loading
                    .error(R.drawable.image_placeholder) // Error image if loading fails
                    .into(editItemImageView);

            editNameEditText.setText(itemName);
            editDescriptionEditText.setText(itemNote);
            editPriceEditText.setText(itemPrice);

            dialogBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String editedName = editNameEditText.getText().toString().trim();
                    String editedDescription = editDescriptionEditText.getText().toString().trim();
                    String editedPrice = editPriceEditText.getText().toString().trim();
                    if (editedName.isEmpty() || editedDescription.isEmpty() || editedPrice.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "Item name, description, and price are required!", Toast.LENGTH_SHORT).show();
                    } else {
                        DocumentReference itemRef = (DocumentReference) itemData.get("docRef");
                        if (itemRef != null) {
                            // Update the Firestore document with the new data
                            Map<String, Object> updatedData = new HashMap<>();
                            updatedData.put("Items Name", editedName);
                            updatedData.put("notes", editedDescription);
                            updatedData.put("price", editedPrice);

                            // Check if a new image was selected
                            if (imageUri != null) {
                                // Upload the new image to Firebase Storage
                                uploadImageToFirebaseStorage(imageUri, editedName, editedDescription, editedPrice);
                                updatedData.put("imageUrl", imageUri.toString()); // Update the image URL in Firestore
                            }

                            // Update the Firestore document
                            itemRef.update(updatedData)
                                    .addOnSuccessListener(aVoid -> {
                                        // Successfully updated the document
                                        Toast.makeText(HomePage.this, "Item updated successfully!", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        // Handle the error if the update fails
                                        Toast.makeText(HomePage.this, "Failed to update item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                }
            });

            dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });

            AlertDialog editDialog = dialogBuilder.create();
            editDialog.show();
        }
    }




    private void deleteItem(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Delete");
        builder.setMessage("Are you sure you want to delete this item?");
        builder.setPositiveButton("Delete", (dialogInterface, i) -> {
            DocumentReference itemRef = (DocumentReference) itemsList.get(position).get("docRef");
            assert itemRef != null;
            itemRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(HomePage.this, "Item deleted successfully!", Toast.LENGTH_SHORT).show();
                        itemsAdapter.notifyItemRemoved(position);
                    })
                    .addOnFailureListener(e -> Toast.makeText(HomePage.this, "Failed to delete item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());
        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            positiveButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            negativeButton.setTextColor(getResources().getColor(android.R.color.black));
        });
        dialog.show();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void refreshData() {
        itemsList.clear();
        itemsAdapter.notifyDataSetChanged();
        startItemsListener();
    }

    private class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ItemViewHolder> {
        private final List<Map<String, Object>> itemsList;

        public ItemsAdapter(List<Map<String, Object>> itemsList) {
            this.itemsList = itemsList;
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_row, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            Map<String, Object> itemData = itemsList.get(position);
            String itemName = (String) itemData.get("Items Name");
            holder.itemNameTextView.setText(itemName);

            // Retrieve the "purchased" field from the Firestore document
            Boolean purchasedValue = (Boolean) itemData.get("purchased");
            boolean isPurchased = purchasedValue != null && purchasedValue;
            holder.checkboxPurchased.setChecked(isPurchased);

            // Apply strike-through style if item is purchased
            if (isPurchased) {
                holder.itemNameTextView.setPaintFlags(holder.itemNameTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                holder.itemNameTextView.setPaintFlags(holder.itemNameTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            holder.checkboxPurchased.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Get the Firestore document reference for the item
                DocumentReference itemRef = (DocumentReference) itemData.get("docRef");
                if (itemRef != null) {
                    // Update the "purchased" field based on the checkbox state
                    itemRef.update("purchased", isChecked)
                            .addOnSuccessListener(aVoid -> {
                                // Successfully updated the "purchased" field
                                // You can perform any additional actions if needed

                                // Display a Toast message when item is purchased
                                if (isChecked) {
                                    Toast.makeText(holder.itemView.getContext(), itemName + " purchased", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                // Handle the error if the update fails
                                // You can display a toast message or take other actions
                            });
                }
            });

            holder.itemView.setOnClickListener(v -> {
                // Retrieve the data of the clicked item
                String imageUrl = (String) itemData.get("imageUrl");
                String itemPrice = (String) itemData.get("price");
                String itemDescription = (String) itemData.get("notes");

                // Create an intent to start the ItemDetailsActivity
                Intent intent = new Intent(v.getContext(), ItemsActivity.class);

                // Pass the item details as extras to the intent
                intent.putExtra("imageUrl", imageUrl);
                intent.putExtra("itemName", itemName);
                intent.putExtra("itemPrice", itemPrice);
                intent.putExtra("itemDescription", itemDescription);

                // Start the ItemDetailsActivity
                v.getContext().startActivity(intent);
            });
        }




        @Override
        public int getItemCount() {
            return itemsList.size();
        }

        public class ItemViewHolder extends RecyclerView.ViewHolder {
            public TextView itemNameTextView;
            public CheckBox checkboxPurchased;

            public ItemViewHolder(@NonNull View itemView) {
                super(itemView);
                itemNameTextView = itemView.findViewById(R.id.nameTextView);
                checkboxPurchased = itemView.findViewById(R.id.checkBox);
            }
        }
    }

}