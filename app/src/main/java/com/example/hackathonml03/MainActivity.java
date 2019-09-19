package com.example.hackathonml03;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private static final int SELECT_PHOTO_REQUEST_CODE = 100;
    private static final int ASK_PERMISSION_REQUEST_CODE = 101;
    private static final String TAG = MainActivity.class.getName();

    private TextView mTextView;
    private ImageView mImageView;
    private View mLayout;

    private FirebaseVisionLabelDetector mDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTextView = findViewById(R.id.textView);
        mImageView = findViewById(R.id.imageView);
        mLayout = findViewById(R.id.main_layout);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPermissions();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }else if (id == R.id.action_process) {
            processImage();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Snackbar.make(mLayout, R.string.storage_access_required, Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, ASK_PERMISSION_REQUEST_CODE);

                    }

                }).show();

            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, ASK_PERMISSION_REQUEST_CODE);

            }

        } else {

            openGallery();
        }
    }

    private void openGallery() {
        Intent photoPikerIntent = new Intent(Intent.ACTION_PICK);
        photoPikerIntent.setType("image/*");
        startActivityForResult(photoPikerIntent, SELECT_PHOTO_REQUEST_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_PHOTO_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                mImageView.setImageBitmap(bitmap);
                mTextView.setText("");
            } catch (IOException e) {
                e.printStackTrace();

            }

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case ASK_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery();

                } else {


                }
                return;
            }

        }

    }

    private void processImage() {
        if (mImageView.getDrawable() == null) {
            Snackbar.make(mLayout, R.string.select_image, Snackbar.LENGTH_SHORT).show();

        } else {
            FirebaseVisionLabelDetectorOptions options =
                    new FirebaseVisionLabelDetectorOptions.Builder()
                            .setConfidenceThreshold(0.8f)
                            .build();
            Bitmap bitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
            mDetector = FirebaseVision.getInstance().getVisionLabelDetector(options);
            mDetector.detectInImage(image)
                    .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionLabel>>() {
                        @Override
                        public void onSuccess(List<FirebaseVisionLabel> labels) {
                            StringBuilder sb = new StringBuilder();
                            for (FirebaseVisionLabel label : labels) {
                                String text = label.getLabel();
                                String entityId = label.getEntityId();
                                float confidence = label.getConfidence();
                                sb.append("Label: " + text + "; Confidence:" + confidence);

                            }
                            mTextView.setText(sb);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Image labelling failed : " + e);
                        }
                    });

        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mDetector != null) {
            try {
                mDetector.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception thrown while trying to close Image Labeling Detector: " + e);
            }
        }

    }
}
