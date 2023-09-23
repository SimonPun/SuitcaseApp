    package com.jivanpun.suitcaseapp;

    import android.app.Dialog;
    import android.content.DialogInterface;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.graphics.Paint;
    import android.hardware.Sensor;
    import android.hardware.SensorEvent;
    import android.hardware.SensorEventListener;
    import android.hardware.SensorManager;
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
    import android.widget.CompoundButton;
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
    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;

    public class HomePage extends AppCompatActivity implements SensorEventListener {
        private FirebaseAuth auth;
        private GoogleSignInClient googleSignInClient;
        private FloatingActionButton floatingActionButton;
        private FirebaseFirestore db;
        private ItemsAdapter itemsAdapter;
        private List<Map<String, Object>> itemsList = new ArrayList<>();
        private ListenerRegistration itemsListener;
        private SwipeRefreshLayout swipeRefreshLayout;
        private Map<String, Object> originalItemData;
        private SensorManager sensorManager;
        private Sensor accelerometer;
        private float lastAcceleration = 0;
        private float currentAcceleration = 0;
        private static final float SHAKE_THRESHOLD = 5.0f;
        private boolean isShakeEnabled = true;
        private SharedPreferences sharedPreferences;
        private static final int GALLERY_REQUEST_CODE = 100;
        private Dialog customDialog;
        private Uri selectedImageUri;

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


            // Initialize accelerometer sensor
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        }


        @Override
        protected void onStart() {
            super.onStart();
            startItemsListener();
        }

        @Override
        protected void onResume() {
            super.onResume();
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            isShakeEnabled = true;
        }

        @Override
        protected void onPause() {
            super.onPause();
            sensorManager.unregisterListener(this);
            isShakeEnabled = false;
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

                // Set an OnClickListener for the "Choose Image" TextView
                chooseImage.setOnClickListener(v -> {
                    // Open the gallery to choose an image
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
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
                            String currentUserId = currentUser.getUid();
                            Map<String, Object> itemsData = new HashMap<>();
                            itemsData.put("Items Name", itemName);
                            itemsData.put("notes", note);
                            itemsData.put("price", itemPrice);
                            itemsData.put("userId", currentUserId);

                            // Add the item data to Firestore
                            db.collection("Items")
                                    .add(itemsData)
                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                        @Override
                                        public void onSuccess(DocumentReference documentReference) {
                                            // Set the purchase status to false for the newly added item in SharedPreferences
                                            int position = itemsList.size(); // Get the position of the newly added item
                                            SharedPreferences.Editor editor = sharedPreferences.edit();
                                            editor.putBoolean("item_" + position + "_purchased", false);
                                            editor.apply();

                                            Toast.makeText(HomePage.this, "Item added!", Toast.LENGTH_SHORT).show();
                                            customDialog.dismiss(); // Close the dialog on success
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
                });
                customDialog.show();
            }
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
                // Get the selected image URI from the data
                Uri imageUri = data.getData();

                // Find the ImageView in the existing Dialog
                if (customDialog != null) {
                    ImageView imageView = customDialog.findViewById(R.id.imageView);

                    // Display the selected image in the ImageView
                    imageView.setImageURI(imageUri);
                }
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
                                            showToast("Item updated successfully!");
                                            editDialog.dismiss(); // Close the dialog on success
                                            itemsAdapter.notifyItemChanged(position);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            showToast("Failed to update item: " + e.getMessage());
                                        }
                                    });
                        }
                    }
                });
                editDialog.show();
            }
        }

        private void undoEdit() {
            if (originalItemData != null) {
                FirebaseUser currentUser = auth.getCurrentUser();
                if (currentUser != null) {
                    DocumentReference itemRef = (DocumentReference) originalItemData.get("docRef");
                    itemRef.update(originalItemData)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    showToast("Edit undone successfully!");
                                    itemsAdapter.notifyDataSetChanged();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    showToast("Failed to undo edit: " + e.getMessage());
                                }
                            });
                }
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
                                    showToast("Item deleted successfully!");
                                    itemsAdapter.notifyItemRemoved(position);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    showToast("Failed to delete item: " + e.getMessage());
                                }
                            });
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    itemsAdapter.notifyItemChanged(position);
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

        private void showToast(String message) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }

        private void refreshData() {
            if (itemsListener != null) {
                itemsListener.remove();
            }
            startItemsListener();
        }

        private class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ViewHolder> {

            private List<Map<String, Object>> itemsList;

            public ItemsAdapter(List<Map<String, Object>> itemsList) {
                this.itemsList = itemsList;
            }

            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_row, parent, false);
                return new ViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                Map<String, Object> itemData = itemsList.get(position);
                String itemName = (String) itemData.get("Items Name");
                String itemPrice = (String) itemData.get("price");
                String itemNote = (String) itemData.get("notes");

                holder.nameTextView.setText(itemName);
                holder.priceTextView.setText(itemPrice);
                holder.noteTextView.setText(itemNote);

                holder.checkBox.setVisibility(View.VISIBLE);
                holder.purchaseMessageTextView.setVisibility(View.GONE);

                boolean isPurchased = sharedPreferences.getBoolean("item_" + position + "_purchased", false);

                if (isPurchased) {
                    holder.checkBox.setVisibility(View.GONE);
                    holder.purchaseMessageTextView.setVisibility(View.VISIBLE);
                    holder.purchaseMessageTextView.setText("Purchased");
                } else {
                    holder.checkBox.setVisibility(View.VISIBLE);
                    holder.purchaseMessageTextView.setVisibility(View.GONE);
                }

                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.purchaseMessageTextView.getLayoutParams();
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.addRule(RelativeLayout.CENTER_VERTICAL);
                holder.purchaseMessageTextView.setLayoutParams(params);

                holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        int adapterPosition = holder.getAdapterPosition();
                        if (adapterPosition != RecyclerView.NO_POSITION) {
                            Map<String, Object> itemData = itemsList.get(adapterPosition);
                            String itemName = (String) itemData.get("Items Name");
                            String itemPrice = (String) itemData.get("price");
                            String itemNote = (String) itemData.get("notes");
                            String shareMessage = "Item Name: " + itemName + "\n" +
                                    "Item Price: " + itemPrice + "\n" +
                                    "Item Note: " + itemNote;
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("text/plain");
                            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                            if (shareIntent.resolveActivity(holder.itemView.getContext().getPackageManager()) != null) {
                                holder.itemView.getContext().startActivity(shareIntent);
                            } else {
                                showToast("No SMS app available.");
                            }
                        }
                        return true;
                    }
                });

                holder.checkBox.setOnClickListener(v -> {
                    int adapterPosition = holder.getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        boolean isChecked = holder.checkBox.isChecked();
                        if (isChecked) {
                            holder.checkBox.setVisibility(View.GONE);
                            holder.purchaseMessageTextView.setVisibility(View.VISIBLE);
                            holder.purchaseMessageTextView.setText("Purchased");

                            // Display a toast message when the item is purchased
                            showToast("Item Purchased");
                        } else {
                            holder.checkBox.setVisibility(View.VISIBLE);
                            holder.purchaseMessageTextView.setVisibility(View.GONE);
                        }
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("item_" + adapterPosition + "_purchased", isChecked);
                        editor.apply();
                    }
                });

            }


            @Override
            public int getItemCount() {
                return itemsList.size();
            }

            // Inside your ItemsAdapter ViewHolder class
            public class ViewHolder extends RecyclerView.ViewHolder {
                public TextView nameTextView;
                public TextView priceTextView;
                public TextView noteTextView;
                public CheckBox checkBox;
                public TextView purchaseMessageTextView; // New TextView for the purchase message

                public ViewHolder(View itemView) {
                    super(itemView);
                    nameTextView = itemView.findViewById(R.id.nameTextView);
                    priceTextView = itemView.findViewById(R.id.priceTextView);
                    noteTextView = itemView.findViewById(R.id.noteTextView);
                    checkBox = itemView.findViewById(R.id.checkBox);
                    purchaseMessageTextView = itemView.findViewById(R.id.purchaseMessageTextView); // Initialize the new TextView

                    // Set an OnClickListener for the checkbox
                    checkBox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Check if the checkbox is checked
                            boolean isChecked = checkBox.isChecked();

                            // Apply the strike-through effect and show/hide the purchase message accordingly
                            if (isChecked) {
                                // Checkbox is checked
                                nameTextView.setPaintFlags(nameTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                                purchaseMessageTextView.setVisibility(View.VISIBLE);
                            } else {
                                // Checkbox is unchecked
                                nameTextView.setPaintFlags(nameTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                                purchaseMessageTextView.setVisibility(View.GONE);
                            }
                        }
                    });
                }
            }

        }

        // Change this threshold to make shake detection less sensitive


        @Override
        public void onSensorChanged(SensorEvent event) {
            if (isShakeEnabled) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                lastAcceleration = currentAcceleration;
                currentAcceleration = (float) Math.sqrt(x * x + y * y + z * z);

                // Adjust the threshold here
                float customShakeThreshold = 10.0f; // You can change this value as needed

                float delta = currentAcceleration - lastAcceleration;
                if (delta > customShakeThreshold) {
                    undoEdit();
                }
            }
        }


        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not used in this example
        }
    }