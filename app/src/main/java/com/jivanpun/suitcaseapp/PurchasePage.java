package com.jivanpun.suitcaseapp;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class PurchasePage extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_page);

        // Assuming you have a TextView in your PurchasePage layout to display purchased items
        TextView purchasedItemsTextView = findViewById(R.id.purchasedItemsTextView);

        // Retrieve and display purchased items (you can replace this with your data)
        String purchasedItems = "";
        purchasedItemsTextView.setText(purchasedItems);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_profile) {
            // Handle Profile menu item click
            // Add your code here
            return true;
        } else if (itemId == R.id.action_logout) {
            // Handle Logout menu item click
            // Add your code here
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
