package com.asm.logbookapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.asm.logbookapp.adapter.ImageAdapter;
import com.asm.logbookapp.databinding.ActivityMainBinding;
import com.asm.logbookapp.models.ImageModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private ActivityMainBinding binding;

    private ImageAdapter imageAdapter;
    private List<ImageModel> imageList;
    private int currentImagePos;
    private String urlImage;
    private static final int CAMERA_PERM_CODE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // call inflate
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        // replace the argument passed to setContentView with binding.getRoot()
        setContentView(binding.getRoot());
        // Init SharedPreferences
        sharedPreferences = getSharedPreferences("image_store", Context.MODE_PRIVATE);
        initButton();
        loadImage();
        // Receive event when page on viewpager selected
        binding.imageSlider.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentImagePos = position;
                invalidateButton();
            }
        });
        // Listener click add link button
        binding.buttonAddLink.setOnClickListener(view -> {
            // Get string from edittext
            urlImage = binding.addLinkEdit.getText().toString();
            if (isValidURL()) {
                addImage();
                Toast.makeText(MainActivity.this, "Added link", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Invalid Url!", Toast.LENGTH_SHORT).show();
            }
        });

        // Listener click capture button
        binding.buttonCapture.setOnClickListener(view -> verifyPermissions());

    }

    // Verify permissions of app
    private void verifyPermissions() {
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[0]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[1]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[2]) == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent();
        } else {
            ActivityCompat.requestPermissions(this, permissions, CAMERA_PERM_CODE);
        }
    }

    // Request grant permission for app
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERM_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera Permission is Required to Use camera.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                File f = new File(currentPhotoPath);
                Toast.makeText(this, "Uri: " +  Uri.fromFile(f).toString(), Toast.LENGTH_SHORT).show();
                // Add image to photo gallery
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);
                // Add new image captured to share preferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                Gson gson = new Gson();
                imageList.add(new ImageModel(null, Uri.fromFile(f).toString()));
                String json = gson.toJson(imageList);
                editor.putString("image", json);
                editor.apply();
                Toast.makeText(this, "Added image captured", Toast.LENGTH_SHORT).show();
                loadImage();
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Create the File where the photo should go
        File photoFile = createImageFile();
        // Continue only if the File was successfully created
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.asm.logbookapp.fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
        }
    }

    // Create name and path for image file captured
    private File createImageFile() {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) ;
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Save a file: path for use with ACTION_VIEW intents
        if (image != null) {
            currentPhotoPath = image.getAbsolutePath();
        }
        return image;
    }

    // binding imageSlider view with imageAdapter
    private void loadImage() {
        imageAdapter = new ImageAdapter(this, initSlider());
        binding.imageSlider.setAdapter(imageAdapter);
    }

    // Add image list to sharedPreferences
    private void addImage() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        imageList.add(new ImageModel(urlImage, null));
        String json = gson.toJson(imageList);
        editor.putString("image", json);
        editor.apply();
        loadImage();
    }

    // Init image list from sharedPreferences
    private List<ImageModel> initSlider() {
        Gson gson = new Gson();
        String json = sharedPreferences.getString("image", null);
        Type type = new TypeToken<List<ImageModel>>() {
        }.getType();
        imageList = gson.fromJson(json, type);
        if (imageList == null) {
            imageList = new ArrayList<>();
            binding.buttonNext.setEnabled(false);
            binding.buttonPrevious.setEnabled(false);
        } else {
            for (int i = 0; i < imageList.size(); i++) {
                imageList.get(i).getUrlImage();
                imageList.get(i).getUriCaptured();
            }
        }
        return imageList;
    }


    // Check valid url of images
    private boolean isValidURL() {
        // Regex to check valid URL image
        String regex = "((http|https)://)(www.)?"
                + "[a-zA-Z0-9@:%._\\+~#?&//=]"
                + "{2,256}\\.[a-z]"
                + "{2,6}\\b([-a-zA-Z0-9@:%"
                + "._\\+~#?&//=]*)"
                + "\\.(?:jpg|gif|png)";

        // Compile the ReGex
        Pattern p = Pattern.compile(regex);

        // If the string is empty
        // return false
        if (urlImage == null) {
            return false;
        }

        // Find match between given string
        // and regular expression
        // using Pattern.matcher()
        Matcher m = p.matcher(urlImage);

        // Return if the string
        // matched the ReGex
        return m.matches();
    }

    // Check position of image to enable/disable button
    private void invalidateButton() {
        if (imageList.size() < 2) {
            binding.buttonNext.setEnabled(false);
            binding.buttonPrevious.setEnabled(false);
        } else if (currentImagePos == 0) {
            binding.buttonNext.setEnabled(true);
            binding.buttonPrevious.setEnabled(false);
        } else if (currentImagePos == imageAdapter.getItemCount() - 1) {
            binding.buttonNext.setEnabled(false);
            binding.buttonPrevious.setEnabled(true);
        } else {
            binding.buttonNext.setEnabled(true);
            binding.buttonPrevious.setEnabled(true);
        }
    }

    // Init button previous and next
    private void initButton() {
        currentImagePos = 0;
        binding.buttonPrevious.setOnClickListener(view -> {
            currentImagePos--;
            binding.imageSlider.setCurrentItem(currentImagePos);
        });
        binding.buttonNext.setOnClickListener(view -> {
            currentImagePos++;
            binding.imageSlider.setCurrentItem(currentImagePos);
        });
    }

}