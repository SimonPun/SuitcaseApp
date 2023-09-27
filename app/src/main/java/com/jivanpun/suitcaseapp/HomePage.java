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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
            customDialog = new Dialog(this);
            customDialog.setContentView(R.layout.additem_menu);
            Objects.requireNonNull(customDialog.getWindow()).setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            androidx.appcompat.widget.AppCompatEditText inputDesName = customDialog.findViewById(R.id.nameEditText);
            androidx.appcompat.widget.AppCompatEditText inputNote = customDialog.findViewById(R.id.descriptionEditText);
            androidx.appcompat.widget.AppCompatEditText inputPrice = customDialog.findViewById(R.id.priceEditText);
            Button saveButton = customDialog.findViewById(R.id.saveButton);
            TextView chooseImage = customDialog.findViewById(R.id.chooseImage);

            chooseImage.setOnClickListener(v -> {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
            });

            saveButton.setOnClickListener(v -> {
                String itemName = Objects.requireNonNull(inputDesName.getText()).toString().trim();
                String note = Objects.requireNonNull(inputNote.getText()).toString().trim();
                String itemPrice = Objects.requireNonNull(inputPrice.getText()).toString().trim();
                if (itemName.isEmpty() || note.isEmpty() || itemPrice.isEmpty()) {
                    Toast.makeText(HomePage.this, "Item name, description, and price are required!", Toast.LENGTH_SHORT).show();
                } else {
                    uploadImageToFirebaseStorage(imageUri, itemName, note, itemPrice);
                }
            });
            customDialog.show();
        }
    }

    private Uri imageUri;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();

            if (customDialog != null) {
                ImageView imageView = customDialog.findViewById(R.id.imageView);
                imageView.setImageURI(imageUri);
                this.imageUri = imageUri;
            }
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
                        customDialog.dismiss();
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
            Dialog editDialog = new Dialog(this);
            editDialog.setContentView(R.layout.edit_item_dialog);
            androidx.appcompat.widget.AppCompatEditText editNameEditText = editDialog.findViewById(R.id.nameEditText);
            androidx.appcompat.widget.AppCompatEditText editDescriptionEditText = editDialog.findViewById(R.id.descriptionEditText);
            androidx.appcompat.widget.AppCompatEditText editPriceEditText = editDialog.findViewById(R.id.priceEditText);
            Button saveEditButton = editDialog.findViewById(R.id.saveButton);
            editNameEditText.setText(itemName);
            editDescriptionEditText.setText(itemNote);
            editPriceEditText.setText(itemPrice);
            saveEditButton.setOnClickListener(v -> {
                String editedName = Objects.requireNonNull(editNameEditText.getText()).toString().trim();
                String editedDescription = Objects.requireNonNull(editDescriptionEditText.getText()).toString().trim();
                String editedPrice = Objects.requireNonNull(editPriceEditText.getText()).toString().trim();
                if (editedName.isEmpty() || editedDescription.isEmpty() || editedPrice.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Item name, description, and price are required!", Toast.LENGTH_SHORT).show();
                } else {
                    DocumentReference itemRef = (DocumentReference) itemData.get("docRef");
                    Map<String, Object> updatedData = new HashMap<>();
                    updatedData.put("Items Name", editedName);
                    updatedData.put("notes", editedDescription);
                    updatedData.put("price", editedPrice);
                    assert itemRef != null;
                    itemRef.update(updatedData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(HomePage.this, "Item updated successfully!", Toast.LENGTH_SHORT).show();
                                editDialog.dismiss();
                                itemsAdapter.notifyItemChanged(position);
                            })
                            .addOnFailureListener(e -> Toast.makeText(HomePage.this, "Failed to update item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
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