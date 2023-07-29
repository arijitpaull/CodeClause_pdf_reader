package com.example.pdfreadercodeclause;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.pdfreadercodeclause.model.FileinModel;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    EditText edit;
    Button uploadBTn;

    private PDFView pdfView;
    private Button viewPdfBtn;

    StorageReference storageReference;
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pdfView = findViewById(R.id.pdfView);
        viewPdfBtn = findViewById(R.id.btnViewPdf);
        viewPdfBtn.setVisibility(View.GONE);

        edit = findViewById(R.id.editText);
        uploadBTn = findViewById(R.id.btn);

        storageReference = FirebaseStorage.getInstance().getReference();
        databaseReference = FirebaseDatabase.getInstance().getReference("pdfs");

        uploadBTn.setEnabled(false);

        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectPDF();
            }
        });

    }

    private void selectPDF() {
        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Pdf files"),101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101 && resultCode == RESULT_OK && data != null && data.getData() != null){
            Uri uri =data.getData();

            String uriString  = uri.toString();
            File myFile = new File(uriString);
            String path = myFile.getAbsolutePath();
            String displayName = null;

            if (uriString.startsWith("content://")){
                Cursor cursor = null;
                try {
                    cursor = this.getContentResolver().query(uri,null,null,null,null);
                    if (cursor != null && cursor.moveToFirst()){
                        displayName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    cursor.close();
                }
            } else  if (uriString.startsWith("file://")){
                displayName = myFile.getName();
            }

            uploadBTn.setEnabled(true);
            edit.setText(displayName);

            uploadBTn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    uploadPDF(data.getData());
                }
            });

            viewPdfBtn.setVisibility(View.VISIBLE);
            viewPdfBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Call a method to display the selected PDF in the PDF View
                    displaySelectedPDF(data.getData());
                }
            });


        }

    }

    private void displaySelectedPDF(Uri data) {
        try {
            // Load the selected PDF into the PDF View
            pdfView.fromUri(data)
                    .defaultPage(0)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .scrollHandle(new DefaultScrollHandle(this))
                    .load();

            // Hide the other views and show the PDF View
            edit.setVisibility(View.GONE);
            uploadBTn.setVisibility(View.GONE);
            viewPdfBtn.setVisibility(View.GONE);
            pdfView.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadPDF(Uri data) {
        final ProgressDialog  pd = new ProgressDialog(this);
        pd.setTitle("File Uploading..");
        pd.show();

        final StorageReference reference = storageReference.child("uploads/"+ System.currentTimeMillis() + ".pdf");
        reference.putFile(data)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isComplete());
                        Uri uri = uriTask.getResult();

                        FileinModel fileinModel = new FileinModel(edit.getText().toString(), uri.toString()); //get the views from the model class
                        databaseReference.child(databaseReference.push().getKey()).setValue(fileinModel);// push the value into the realtime database
                        Toast.makeText(MainActivity.this, "File Uploaded Successfully!!", Toast.LENGTH_SHORT).show();
                        pd.dismiss();

                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        float percent = (100 * snapshot.getBytesTransferred())/ snapshot.getTotalByteCount();
                        pd.setMessage("Uploaded : "+ (int) percent + "%");
                    }
                });

    }



    public void retrievePDFs(View view) {
        startActivity(new Intent(MainActivity.this, RetrievePDFActivity.class));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void cancelViewPdf(View view) {
        // Hide the PDF View and show the other views
        pdfView.setVisibility(View.GONE);
        edit.setVisibility(View.VISIBLE);
        uploadBTn.setVisibility(View.VISIBLE);
        viewPdfBtn.setVisibility(View.VISIBLE);
    }
}