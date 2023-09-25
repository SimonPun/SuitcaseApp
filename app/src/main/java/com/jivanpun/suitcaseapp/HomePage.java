package com.jivanpun.suitcaseapp;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomePage extends AppCompatActivity {
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private FloatingActionButton floatingActionButton;
    private FirebaseFirestore db;
    private ItemsAdapter itemsAdapter;
    private List<Map<String, Object>> itemsList = new ArrayList<>();
    private ListenerRegistration itemsListener;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Map<String, Object> originalItemData;
    private SharedPreferences sharedPreferences;
    private static final int GALLERY_REQUEST_CODE = 100;
    private Dialog customDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        auth = FirebaseAuth.getInstance();
        googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("ItemPreferences", MODE_PRIVATE);
        setUpAppBar();
        floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomDialog();
            }
        });

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
                int position = viewHolder.getAdapterPosition();
                if (direction == ItemTouchHelper.LEFT) {
                    deleteItem(position);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    editItem(position);
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData();
            }
        });
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

    private void startItemsListener() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            Query itemsQuery = db.collection("Items")
                    .whereEqualTo("userId", currentUserId);

            itemsListener = itemsQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(QuerySnapshot queryDocumentSnapshots, FirebaseFirestoreException e) {
                    if (e != null) {
                        Toast.makeText(HomePage.this, "Error fetching items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                        return;
                    }
                    itemsList.clear();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Map<String, Object> itemData = documentSnapshot.getData();
                        itemData.put("docRef", documentSnapshot.getReference());
                        itemsList.add(itemData);
                    }
                    itemsAdapter.notifyDataSetChanged();
                    swipeRefreshLayout.setRefreshing(false);
                }
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
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_profile) {
            return true;
        } else if (id == R.id.action_logout) {
            showLogoutConfirmationDialog();
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCustomDialog() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            customDialog = new Dialog(this);
            customDialog.setContentView(R.layout.additem_menu);
            customDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            TextInputEditText inputDesName = customDialog.findViewById(R.id.nameEditText);
            TextInputEditText inputNote = customDialog.findViewById(R.id.descriptionEditText);
            TextInputEditText inputPrice = customDialog.findViewById(R.id.priceEditText);
            Button saveButton = customDialog.findViewById(R.id.saveButton);
            ImageView imageView = customDialog.findViewById(R.id.imageView);
            TextView chooseImage = customDialog.findViewById(R.id.chooseImage);

            chooseImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
                }
            });

            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String itemName = inputDesName.getText().toString().trim();
                    String note = inputNote.getText().toString().trim();
                    String itemPrice = inputPrice.getText().toString().trim();
                    if (itemName.isEmpty() || note.isEmpty() || itemPrice.isEmpty()) {
                        Toast.makeText(HomePage.this, "Item name, description, and price are required!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Now, upload the image to Firebase Storage
                        uploadImageToFirebaseStorage(imageUri, itemName, note, itemPrice);
                    }
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

                // Save the image URI to a class variable for later use
                this.imageUri = imageUri;
            }
        }
    }
    private void uploadImageToFirebaseStorage(Uri imageUri, String itemName, String note, String itemPrice) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();

            // Generate a unique name for the image file (you can use the current timestamp)
            String imageName = "item_image_" + System.currentTimeMillis() + ".jpg";

            // Get a reference to the Firebase Storage location where you want to store the image
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                    .child("item_images")
                    .child(currentUserId)
                    .child(imageName);

            // Upload the image to Firebase Storage
            UploadTask uploadTask = storageRef.putFile(imageUri);

            uploadTask.addOnCompleteListener(this, new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        // Image uploaded successfully, now get the download URL
                        storageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri downloadUri) {
                                // After getting the download URL, save all item details in Firestore
                                saveItemToFirestore(downloadUri.toString(), itemName, note, itemPrice);
                            }
                        });
                    } else {
                        // Handle the error case if the image upload fails
                        Toast.makeText(HomePage.this, "Failed to upload image: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void saveItemToFirestore(String imageUrl, String itemName, String note, String itemPrice) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();

            // Create a Map containing all the item data including the image URL
            Map<String, Object> itemsData = new HashMap<>();
            itemsData.put("Items Name", itemName);
            itemsData.put("notes", note);
            itemsData.put("price", itemPrice);
            itemsData.put("imageUrl", imageUrl); // Add the image URL
            itemsData.put("userId", currentUserId); // Add the user ID

            db.collection("Items")
                    .add(itemsData)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Toast.makeText(HomePage.this, "Item added!", Toast.LENGTH_SHORT).show();
                            customDialog.dismiss();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(HomePage.this, "Failed to add item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }



    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Logout");
        builder.setMessage("Are you sure you want to log out?");
        builder.setPositiveButton("Logout", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                googleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            auth.signOut();
                            Toast.makeText(getApplicationContext(), "Logged out Successfully", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
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

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                positiveButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                negativeButton.setTextColor(getResources().getColor(android.R.color.black));
            }
        });
        dialog.show();
    }

    private void editItem(int position) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            originalItemData = itemsList.get(position);
            Map<String, Object> itemData = itemsList.get(position);
            String itemName = (String) itemData.get("Items Name");
            String itemNote = (String) itemData.get("notes");
            String itemPrice = (String) itemData.get("price");
            Dialog editDialog = new Dialog(this);
            editDialog.setContentView(R.layout.edit_item_dialog);
            TextInputEditText editNameEditText = editDialog.findViewById(R.id.nameEditText);
            TextInputEditText editDescriptionEditText = editDialog.findViewById(R.id.descriptionEditText);
            TextInputEditText editPriceEditText = editDialog.findViewById(R.id.priceEditText);
            Button saveEditButton = editDialog.findViewById(R.id.saveButton);
            editNameEditText.setText(itemName);
            editDescriptionEditText.setText(itemNote);
            editPriceEditText.setText(itemPrice);
            saveEditButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String editedName = editNameEditText.getText().toString().trim();
                    String editedDescription = editDescriptionEditText.getText().toString().trim();
                    String editedPrice = editPriceEditText.getText().toString().trim();
                    if (editedName.isEmpty() || editedDescription.isEmpty() || editedPrice.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "Item name, description, and price are required!", Toast.LENGTH_SHORT).show();
                    } else {
                        DocumentReference itemRef = (DocumentReference) itemData.get("docRef");
                        Map<String, Object> updatedData = new HashMap<>();
                        updatedData.put("Items Name", editedName);
                        updatedData.put("notes", editedDescription);
                        updatedData.put("price", editedPrice);
                        itemRef.update(updatedData)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(HomePage.this, "Item updated successfully!", Toast.LENGTH_SHORT).show();
                                        editDialog.dismiss();
                                        itemsAdapter.notifyItemChanged(position);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(HomePage.this, "Failed to update item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                }
            });
            editDialog.show();
        }
    }

    private void deleteItem(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Delete");
        builder.setMessage("Are you sure you want to delete this item?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                DocumentReference itemRef = (DocumentReference) itemsList.get(position).get("docRef");
                itemRef.delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(HomePage.this, "Item deleted successfully!", Toast.LENGTH_SHORT).show();
                                itemsAdapter.notifyItemRemoved(position);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(HomePage.this, "Failed to delete item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
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

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                positiveButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                negativeButton.setTextColor(getResources().getColor(android.R.color.black));
            }
        });
        dialog.show();
    }

    private void refreshData() {
        itemsList.clear();
        itemsAdapter.notifyDataSetChanged();
        startItemsListener();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ItemViewHolder> {
        private List<Map<String, Object>> itemsList;

        public ItemsAdapter(List<Map<String, Object>> itemsList) {
            this.itemsList = itemsList;
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            Map<String, Object> itemData = itemsList.get(position);
            String itemName = (String) itemData.get("Items Name");

            // Set the item name in the TextView
            holder.itemNameTextView.setText(itemName);

            // Set an OnClickListener for the item view
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Call the method to display item details in a dialog box
                    showItemDetailsDialog(itemData);
                }
            });

        }
        private void showItemDetailsDialog(Map<String, Object> itemData) {
            // Create a Material Design dialog
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(HomePage.this);
            builder.setTitle("Item Details"); // Set the title
            // Set an icon if needed

            // Create a layout for item details
            View dialogView = getLayoutInflater().inflate(R.layout.item_details_dialog, null);

            // Initialize TextViews and other views in the dialog layout
            ImageView itemImageView = dialogView.findViewById(R.id.itemImageView);
            TextView itemNameTextView = dialogView.findViewById(R.id.itemNameTextView);
            TextView itemDescriptionTextView = dialogView.findViewById(R.id.itemDescriptionTextView);
            TextView itemPriceTextView = dialogView.findViewById(R.id.itemPriceTextView);

            // Retrieve item details from the itemData Map
            String imageUrl = (String) itemData.get("imageUrl");
            String itemName = (String) itemData.get("Items Name");
            String itemDescription = (String) itemData.get("notes");
            String itemPrice = (String) itemData.get("price");

            // Set the retrieved item details to the TextViews
            Picasso.get().load(imageUrl).into(itemImageView);
            itemNameTextView.setText(itemName);
            itemDescriptionTextView.setText(itemDescription);
            itemPriceTextView.setText(itemPrice);

            builder.setView(dialogView);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Handle the OK button click if needed
                }
            });

            // Show the Material Design dialog
            builder.show();
        }



        @Override
        public int getItemCount() {
            return itemsList.size();
        }

        public class ItemViewHolder extends RecyclerView.ViewHolder {
            public TextView itemNameTextView;

            public ItemViewHolder(@NonNull View itemView) {
                super(itemView);
                itemNameTextView = itemView.findViewById(R.id.nameTextView);
            }
        }
    }

}
