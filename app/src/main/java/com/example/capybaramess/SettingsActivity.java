package com.example.capybaramess;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SettingsActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private ImageButton imageButtonName, imageButtonUsername, imageButtonEmail, imageButtonBio;
    private TextView nameTextView, usernameTextView, emailTextView, bioTextView, textViewSelectProfileImage;
    private Uri imageUri;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FirebaseAuth mAuth;
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
                        // Update user profile with downloadUrl
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
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setPhotoUri(Uri.parse(imageUrl))
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(SettingsActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                            loadProfileImage();
                        }
                    });
        }
    }

    private void updateUserProfileField(String field, String text, TextView textView) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            StorageReference profileRef = storageReference.child("profiles/" + user.getUid() + "/profile.json");
            final long ONE_MEGABYTE = 1024 * 1024;
            profileRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(bytes -> {
                String jsonText = new String(bytes, StandardCharsets.UTF_8);
                try {
                    JSONObject userProfile = new JSONObject(jsonText);
                    userProfile.put(field, text);
                    uploadJsonData(user.getUid(), userProfile, textView, field, text);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }).addOnFailureListener(exception -> {
                if (((StorageException) exception).getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                    // Handle the case where profile.json doesn't exist yet
                    try {
                        JSONObject userProfile = new JSONObject();
                        userProfile.put(field, text);
                        uploadJsonData(user.getUid(), userProfile, textView, field, text);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(SettingsActivity.this, "Failed to update " + field, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void uploadJsonData(String uid, JSONObject jsonObject, TextView textView, String field, String text) {
        StorageReference jsonReference = storageReference.child("profiles/" + uid + "/profile.json");
        byte[] jsonData = jsonObject.toString().getBytes();
        jsonReference.putBytes(jsonData)
                .addOnSuccessListener(taskSnapshot -> {
                    textView.setText(text);
                    if (field.equals("realName")) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(text)
                                    .build();
                            user.updateProfile(profileUpdates);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Failed to save profile", Toast.LENGTH_SHORT).show());
    }

    private void loadProfileImage() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            StorageReference profileRef = storageReference.child("profiles/" + user.getUid() + "/profile_image.jpg");
            final long ONE_MEGABYTE = 1024 * 1024;
            profileRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(bytes -> {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                Glide.with(this)
                        .load(bitmap)
                        .apply(RequestOptions.circleCropTransform())
                        .into(profileImageView);
            }).addOnFailureListener(exception -> {
                if (((StorageException) exception).getErrorCode() != StorageException.ERROR_OBJECT_NOT_FOUND) {
                    // Handle any other errors
                    Toast.makeText(SettingsActivity.this, "Failed to load profile image", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadUserProfileData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            StorageReference profileRef = storageReference.child("profiles/" + user.getUid() + "/profile.json");
            final long ONE_MEGABYTE = 1024 * 1024;
            profileRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(bytes -> {
                String jsonText = new String(bytes, StandardCharsets.UTF_8);
                try {
                    JSONObject userProfile = new JSONObject(jsonText);
                    if (userProfile.has("realName")) nameTextView.setText(userProfile.getString("realName"));
                    if (userProfile.has("username")) usernameTextView.setText(userProfile.getString("username"));
                    if (userProfile.has("email")) emailTextView.setText(userProfile.getString("email"));
                    if (userProfile.has("bio")) bioTextView.setText(userProfile.getString("bio"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }).addOnFailureListener(exception -> {
                if (((StorageException) exception).getErrorCode() != StorageException.ERROR_OBJECT_NOT_FOUND) {
                    // Handle any other errors
                    Toast.makeText(SettingsActivity.this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
                }
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
        builder.setTitle("Enter Name");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(limit)});
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String text = input.getText().toString();
            listener.onTextEntered(text);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}
