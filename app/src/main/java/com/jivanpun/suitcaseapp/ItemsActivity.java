package com.jivanpun.suitcaseapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.squareup.picasso.BuildConfig;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ItemsActivity extends AppCompatActivity {

    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_items);

        ImageView itemImageView = findViewById(R.id.itemImageView);
        TextView itemNameTextView = findViewById(R.id.itemNameTextView);
        TextView itemPriceTextView = findViewById(R.id.itemPriceTextView);
        TextView itemDescriptionTextView = findViewById(R.id.itemDescriptionTextView);

        String imageUrl = getIntent().getStringExtra("imageUrl");
        String itemName = getIntent().getStringExtra("itemName");
        String itemPrice = getIntent().getStringExtra("itemPrice");
        String itemDescription = getIntent().getStringExtra("itemDescription");

        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        itemImageView.setImageBitmap(resource);
                    }
                });

        itemNameTextView.setText(itemName);
        itemPriceTextView.setText(itemPrice);
        itemDescriptionTextView.setText(itemDescription);

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                final int SWIPE_MIN_DISTANCE = 120;
                final int SWIPE_THRESHOLD_VELOCITY = 200;

                try {
                    if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                        // Right swipe detected, take the user to the homepage
                        Intent intent = new Intent(ItemsActivity.this, HomePage.class);
                        startActivity(intent);
                        finish(); // Optionally, you can finish the current activity
                        return true;
                    }
                } catch (Exception e) {
                    // Do nothing
                }

                return false;
            }
        });



        ImageView shareIcon = findViewById(R.id.shareIcon);
        shareIcon.setOnClickListener(v -> checkPermissionAndShare(imageUrl, itemName, itemPrice, itemDescription));
    }

    private void checkPermissionAndShare(String imageUrl, String itemName, String itemPrice, String itemDescription) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            shareItemDetails(imageUrl, itemName, itemPrice, itemDescription);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ImageView shareIcon = findViewById(R.id.shareIcon);
                shareIcon.performClick();
            } else {
                // Handle permission denied
            }
        }
    }

    private void shareItemDetails(String imageUrl, String itemName, String itemPrice, String itemDescription) {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        try {
                            File cachePath = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "shared_image.jpg");
                            FileOutputStream outputStream = new FileOutputStream(cachePath);
                            resource.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                            outputStream.close();

                            Uri imageUri = FileProvider.getUriForFile(ItemsActivity.this,
                                    BuildConfig.APPLICATION_ID + ".provider", cachePath);

                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("image/*");
                            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this item:\nName: " + itemName + "\nPrice: " + itemPrice + "\nDescription: " + itemDescription);
                            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            startActivity(Intent.createChooser(shareIntent, "Share Item Details"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }
}
