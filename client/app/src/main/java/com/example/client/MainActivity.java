package com.example.client;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ImageView clickToUploadImg;
    private Button uploadButton, galleryButton;
    private EditText urlEditText;
    private TextView objectSummary;

    private Bitmap bitmap;
    private Uri imageUri;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        clickToUploadImg = findViewById(R.id.clickToUploadImg);
        uploadButton = findViewById(R.id.btnUpload);
        galleryButton = findViewById(R.id.btnGalleryUpload);
        urlEditText = findViewById(R.id.editText);
        objectSummary = findViewById(R.id.objectSummary);

        checkPermissions();

        ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        File imgFile = new File(currentPhotoPath);
                        if (imgFile.exists()) {
                            bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                            clickToUploadImg.setImageBitmap(bitmap);
                        } else {
                            Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        try {
                            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                            clickToUploadImg.setImageBitmap(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Failed to load image from gallery", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        clickToUploadImg.setOnClickListener(v -> {
            try {
                File imageFile = createImageFile();
                imageUri = FileProvider.getUriForFile(this, "com.example.client.fileprovider", imageFile);

                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                cameraLauncher.launch(cameraIntent);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            }
        });

        galleryButton.setOnClickListener(v -> {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(galleryIntent);
        });

        uploadButton.setOnClickListener(v -> {
            if (bitmap != null) {
                uploadImage();
            } else {
                Toast.makeText(this, "Please select or capture an image first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void uploadImage() {
        String uploadUrl = urlEditText.getText().toString() + "/upload";

        if (uploadUrl.isEmpty()) {
            Toast.makeText(this, "Please enter the server URL", Toast.LENGTH_SHORT).show();
            return;
        }

        String base64Image = encodeImageToBase64(bitmap);

        StringRequest uploadRequest = new StringRequest(Request.Method.POST, uploadUrl,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String originalImagePath = jsonResponse.getString("original_image_path");
                        String filename = new File(originalImagePath).getName();
                        Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                        processImage(filename);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing upload response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("UploadError", error.toString());
                    Toast.makeText(this, "Upload failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("image", base64Image);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(uploadRequest);
    }

    private void processImage(String filename) {
        String processUrl = urlEditText.getText().toString() + "/process";

        StringRequest processRequest = new StringRequest(Request.Method.POST, processUrl,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String message = jsonResponse.getString("message");
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                        if (message.equals("Image processed successfully")) {
                            JSONArray detectedObjects = jsonResponse.getJSONArray("detected_objects");
                            String processedImageBase64 = jsonResponse.getString("processed_image");
                            byte[] decodedString = Base64.decode(processedImageBase64, Base64.DEFAULT);
                            Bitmap processedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                            String savedImagePath = saveImageToLocal(processedBitmap, "processed_image.jpg");
                            if (savedImagePath != null) {
                                Toast.makeText(this, "Image downloaded to: " + savedImagePath, Toast.LENGTH_LONG).show();
                                showImageInDefaultViewer(savedImagePath);
                            }

                            clickToUploadImg.setImageBitmap(processedBitmap);

                            StringBuilder summaryBuilder = new StringBuilder("Detected Objects:\n");
                            for (int i = 0; i < detectedObjects.length(); i++) {
                                JSONObject obj = detectedObjects.getJSONObject(i);
                                String objClass = obj.getString("class");
                                double confidence = obj.getDouble("confidence");
                                summaryBuilder.append("- ").append(objClass).append(" (Confidence: ").append(confidence).append(")\n");
                            }

                            objectSummary.setVisibility(View.VISIBLE);
                            objectSummary.setText(summaryBuilder.toString());
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing process response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("ProcessError", error.toString());
                    Toast.makeText(this, "Processing failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("filename", filename);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(processRequest);
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    private String saveImageToLocal(Bitmap bitmap, String fileName) {
        File directory = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ProcessedImages");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File imageFile = new File(directory, fileName);
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void showImageInDefaultViewer(String imagePath) {
        File imageFile = new File(imagePath);
        Uri imageUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", imageFile);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(imageUri, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, "View Image"));
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
        }
    }
}
