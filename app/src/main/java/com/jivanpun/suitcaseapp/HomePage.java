package com.jivanpun.suitcaseapp;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
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
import com.google.android.material.textfield.TextInputLayout;
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

public class HomePage extends AppCompatActivity {

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private FloatingActionButton floatingActionButton;
    private FirebaseFirestore db;
    private ItemsAdapter itemsAdapter;
    private List<Map<String, Object>> itemsList = new ArrayList<>();
    private ListenerRegistration itemsListener;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        auth = FirebaseAuth.getInstance();
        googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);
        db = FirebaseFirestore.getInstance();
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
        } else if (id == R.id.action_purchase) {
            Intent purchaseIntent = new Intent(this, PurchasePage.class);
            startActivity(purchaseIntent);
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
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.additem_menu);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            TextInputEditText inputDesName = dialog.findViewById(R.id.nameEditText);
            TextInputEditText inputNote = dialog.findViewById(R.id.descriptionEditText);
            TextInputEditText inputPrice = dialog.findViewById(R.id.priceEditText);
            Button saveButton = dialog.findViewById(R.id.saveButton);
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
                        db.collection("Items")
                                .add(itemsData)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        Toast.makeText(HomePage.this, "Item added!", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
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
            dialog.show();
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
            TextInputLayout nameInputLayout = editDialog.findViewById(R.id.nameInputLayout);
            TextInputLayout descriptionInputLayout = editDialog.findViewById(R.id.descriptionInputLayout);
            TextInputLayout priceInputLayout = editDialog.findViewById(R.id.priceInputLayout);
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
                                        editDialog.dismiss();
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

    private void deleteItem(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are you sure you want to delete this item?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Map<String, Object> deletedItem = itemsList.get(position);
                DocumentReference itemRef = (DocumentReference) deletedItem.get("docRef");
                itemRef.delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                showToast("Item deleted successfully!");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                showToast("Failed to delete item: " + e.getMessage());
                            }
                        });
                itemsAdapter.removeItem(position);
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
        dialog.show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void refreshData() {
        startItemsListener();
    }

    class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ViewHolder> {

        private List<Map<String, Object>> itemsList;

        ItemsAdapter(List<Map<String, Object>> itemsList) {
            this.itemsList = itemsList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> itemData = itemsList.get(position);
            String itemName = (String) itemData.get("Items Name");
            String itemPrice = (String) itemData.get("price");
            String itemNote = (String) itemData.get("notes");
            holder.nameTextView.setText(itemName);
            holder.priceTextView.setText("Price: " + itemPrice);
            holder.noteTextView.setText(itemNote);
        }

        @Override
        public int getItemCount() {
            return itemsList.size();
        }

        void removeItem(int position) {
            itemsList.remove(position);
            notifyItemRemoved(position);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameTextView;
            TextView priceTextView;
            TextView noteTextView;

            ViewHolder(View itemView) {
                super(itemView);
                nameTextView = itemView.findViewById(R.id.nameTextView);
                priceTextView = itemView.findViewById(R.id.priceTextView);
                noteTextView = itemView.findViewById(R.id.noteTextView);
            }
        }
    }
}
