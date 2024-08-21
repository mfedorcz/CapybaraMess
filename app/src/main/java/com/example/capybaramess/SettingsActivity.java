package com.example.capybaramess;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private ImageButton imageButtonName, imageButtonUsername, imageButtonEmail, imageButtonBio;
    private TextView nameTextView, usernameTextView, emailTextView, bioTextView, textViewSelectProfileImage;
    private Uri imageUri;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private Dialog loadingDialog;
    private int nameCharLimit = 20;
    private int usernameCharLimit = 20;
    private int emailCharLimit = 40;
    private int bioCharLimit = 400;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    uploadImage();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        profileImageView = findViewById(R.id.profileImageView);
        textViewSelectProfileImage = findViewById(R.id.textViewSelect);
        imageButtonName = findViewById(R.id.ic_back_name);
        imageButtonUsername = findViewById(R.id.ic_back_username);
        imageButtonEmail = findViewById(R.id.ic_back_email);
        imageButtonBio = findViewById(R.id.ic_back_bio);
        nameTextView = findViewById(R.id.nameField);
        usernameTextView = findViewById(R.id.usernameField);
        emailTextView = findViewById(R.id.emailField);
        bioTextView = findViewById(R.id.bioField);
        Button buttonLogOut = findViewById(R.id.buttonLogOut);

        Glide.with(this)
                .load(R.drawable.default_profile_imgmdpi1)
                .apply(RequestOptions.circleCropTransform())
                .into(profileImageView);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        setupActionBar();

        textViewSelectProfileImage.setOnClickListener(v -> requestImagePermission());

        imageButtonName.setOnClickListener(v -> showNameInputDialog(nameCharLimit, text -> updateUserProfileField("realName", text, nameTextView)));
        imageButtonUsername.setOnClickListener(v -> showNameInputDialog(usernameCharLimit, text -> updateUserProfileField("username", text, usernameTextView)));
        imageButtonEmail.setOnClickListener(v -> showNameInputDialog(emailCharLimit, text -> updateUserProfileField("email", text, emailTextView)));
        imageButtonBio.setOnClickListener(v -> showNameInputDialog(bioCharLimit, text -> updateUserProfileField("bio", text, bioTextView)));

        loadProfileImage();
        loadUserProfileData();

        buttonLogOut.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupActionBar() {
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        View customView = LayoutInflater.from(this).inflate(R.layout.action_bar_main_activity, null);
        TextView titleText = customView.findViewById(R.id.actionbar_title);
        titleText.setText("Settings");
        getSupportActionBar().setCustomView(customView);
        ImageView backIcon = findViewById(R.id.backButton);
        backIcon.setVisibility(View.VISIBLE);
        backIcon.setOnClickListener(v -> finish());
    }

    private void requestImagePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 100);
            } else {
                openFileChooser();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            } else {
                openFileChooser();
            }
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        pickImageLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openFileChooser();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImage() {
        Toast.makeText(SettingsActivity.this, "Uploading profile photo...", Toast.LENGTH_SHORT).show();
        if (imageUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                Bitmap squaredBitmap = cropToSquare(bitmap);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                squaredBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();

                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    StorageReference fileReference = storageReference.child("profiles/" + user.getUid() + "/profile_image.jpg");
                    UploadTask uploadTask = fileReference.putBytes(data);

                    uploadTask.addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        // Update Firestore with downloadUrl
                        updateUserProfileImage(downloadUrl);
                    })).addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                } else {
                    Toast.makeText(SettingsActivity.this, "User not authenticated", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUserProfileImage(String imageUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Update Firestore with the new profile image URL
            DocumentReference userRef = firestore.collection("users").document(user.getUid());
            userRef.update("profileImageUrl", imageUrl)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(SettingsActivity.this, "Profile image updated", Toast.LENGTH_SHORT).show();
                            loadProfileImage();
                        } else {
                            Toast.makeText(SettingsActivity.this, "Failed to update profile image", Toast.LENGTH_SHORT).show();
                        }
                    });

            // Optionally, update Firebase Authentication profile if needed
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setPhotoUri(Uri.parse(imageUrl))
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Toast.makeText(SettingsActivity.this, "Failed to update Firebase profile", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void updateUserProfileField(String field, String text, TextView textView) {
        showLoadingScreen();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            DocumentReference userRef = firestore.collection("users").document(user.getUid());
            userRef.update(field, text)
                    .addOnSuccessListener(aVoid -> {
                        dismissLoadingScreen();
                        textView.setText(text);
                        if (field.equals("realName")) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(text)
                                    .build();
                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(task -> {
                                        if (!task.isSuccessful()) {
                                            Toast.makeText(SettingsActivity.this, "Failed to update Firebase profile", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(e ->{
                        Toast.makeText(SettingsActivity.this, "Failed to update " + field, Toast.LENGTH_SHORT).show();
                        dismissLoadingScreen();
                    });
        }
    }

    private void loadProfileImage() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            DocumentReference userRef = firestore.collection("users").document(user.getUid());
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String imageUrl = documentSnapshot.getString("profileImageUrl");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(this)
                                .load(imageUrl)
                                .apply(RequestOptions.circleCropTransform())
                                .into(profileImageView);
                    }
                }
            }).addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Failed to load profile image", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadUserProfileData() {
        showLoadingScreen();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            DocumentReference userRef = firestore.collection("users").document(user.getUid());
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                dismissLoadingScreen();
                if (documentSnapshot.exists()) {
                    if (documentSnapshot.contains("realName"))
                        nameTextView.setText(documentSnapshot.getString("realName"));
                    if (documentSnapshot.contains("username"))
                        usernameTextView.setText(documentSnapshot.getString("username"));
                    if (documentSnapshot.contains("email"))
                        emailTextView.setText(documentSnapshot.getString("email"));
                    if (documentSnapshot.contains("bio"))
                        bioTextView.setText(documentSnapshot.getString("bio"));
                }
            })
                    .addOnFailureListener(e ->{
                        Toast.makeText(SettingsActivity.this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
                        dismissLoadingScreen();
                    });
        }
    }

    private Bitmap cropToSquare(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = Math.min(width, height);
        int newHeight = Math.min(width, height);

        int cropW = (width - newWidth) / 2;
        int cropH = (height - newHeight) / 2;

        return Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight);
    }

    public interface OnTextEnteredListener {
        void onTextEntered(String text);
    }

    private void showNameInputDialog(int limit, OnTextEnteredListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_input, null);
        builder.setView(dialogView);

        final EditText input = dialogView.findViewById(R.id.dialogInput);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(limit)});

        final AlertDialog dialog = builder.create();

        Button buttonOk = dialogView.findViewById(R.id.buttonOk);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        buttonOk.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            listener.onTextEntered(text);
            dialog.dismiss();
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
    private void showLoadingScreen() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setCancelable(false);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_loading, null);

        // Load GIF using Glide
        ImageView loadingImageView = dialogView.findViewById(R.id.loading_image);
        Glide.with(this)
                .asGif()
                .load(R.drawable.loading_animation)
                .into(loadingImageView);

        builder.setView(dialogView);

        loadingDialog = builder.create();

        // Make dialog background transparent
        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        loadingDialog.show();
    }

    private void dismissLoadingScreen() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}
