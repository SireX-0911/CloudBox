package is.com.cryptobayt;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.NoSuchPaddingException;

import is.com.cryptobayt.crypt.AES;
import is.com.cryptobayt.crypt.MyBase64;
import is.com.cryptobayt.crypt.TokenParser;
import is.com.cryptobayt.entity.FileEntity;
import is.com.cryptobayt.utils.FileOperations;

public class AddFiles extends AppCompatActivity {
    RecyclerView rvFileList;
    ArrayList<FileEntity> fileEntities;
    FileAdapter fileAdapter;
    TextView ivAddEmail;
    TextView tvEmails;
    TextView ivAddFile;
    ArrayList<String> emails;
    String currentEmail;
    EditText roomName;
    private FirebaseDatabase dataset = FirebaseDatabase.getInstance();
    private DatabaseReference rootRef = dataset.getReference();
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference STORAGEREF = storage.getReference();
    ImageView beginShow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_files);
        rvFileList = findViewById(R.id.rvFileList);
        ivAddEmail = findViewById(R.id.ivAddEmail);
        tvEmails = findViewById(R.id.tvEmails);
        ivAddFile = findViewById(R.id.ivAddFile);
        beginShow = findViewById(R.id.beginShow);
        roomName = findViewById(R.id.roomName);

        currentEmail = "";
        fileEntities = new ArrayList<>();
        emails = new ArrayList<>();
        if (getIntent().getExtras() != null)
            currentEmail = getIntent().getExtras().getString("email");
        fileAdapter = new FileAdapter();
        emails.add(currentEmail);
        refreshEmails();
        getPermision();
        rvFileList.setLayoutManager(new GridLayoutManager(this, 4));
        rvFileList.setAdapter(fileAdapter);
        ivAddEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(AddFiles.this);
                alertDialog.setTitle("Add Email");

                final EditText input = new EditText(AddFiles.this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                lp.setMargins(dpToPx(16), dpToPx(5), dpToPx(16), 0);
                input.setLayoutParams(lp);
                alertDialog.setView(input);
                alertDialog.setIcon(R.drawable.google_icon);

                alertDialog.setPositiveButton("Add",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (isValidEmail(input.getText().toString())) {
                                    emails.add(input.getText().toString());
                                    refreshEmails();
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(AddFiles.this, "Not Valid Email", Toast.LENGTH_SHORT).show();
                                }

                            }
                        });

                alertDialog.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                alertDialog.show();
            }

        });
        ivAddFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFileChooser();
            }
        });

        beginShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (emails.size() == 0) {
                    Toast.makeText(AddFiles.this, "Add emails which can access for files", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (fileEntities.size() == 0) {
                    Toast.makeText(AddFiles.this, "Add files which you want crypte and upload", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (roomName.getText().toString().equals("")) {
                    roomName.setError("Write name for it folder");
                    return;
                }


                //TODO PROCESS START
                try {
                    startProccess();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public void refreshEmails() {
        String listEmails = "";
        for (String email : emails) {
            listEmails = listEmails + email + ", ";
        }
        tvEmails.setText(listEmails);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECT_CODE) {
            if (resultCode == RESULT_OK) {
                String uri = FileOperations.getPath(this, data.getData());
                File file = new File(uri);
                long file_size = Long.parseLong(String.valueOf(file.length() / 1024));
                Uri keyFileUri = Uri.parse(uri);
                FileEntity fileEntity = new FileEntity(rootRef.push().getKey(), keyFileUri.getLastPathSegment(), file_size);
                fileEntity.setRealAbsolyutPath(uri);
                fileEntities.add(fileEntity);
                fileAdapter.notifyDataSetChanged();
            }
        }
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public class FileAdapter extends RecyclerView.Adapter<ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.file_item, parent, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.filename.setText(fileEntities.get(position).getName());
        }

        @Override
        public int getItemCount() {
            return fileEntities.size();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView filename;

        public ViewHolder(View v) {
            super(v);
            filename = v.findViewById(R.id.filename);
        }
    }


    private static final int FILE_SELECT_CODE = 121;

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }

    }

    public final static boolean isValidEmail(CharSequence target) {
        if (target == null) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    private ProgressDialog mProgressDialog;

    public void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("CRYPTO & UPLOAD");
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }

    byte[] aesKey;
    int uploadedFilesCount = 0;
    String roomID;
    byte[] firebaseKey;
    String md5;

    public void startProccess() throws IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
        showProgressDialog();
        aesKey = AES.getRandomKey();
        firebaseKey = AES.getRandomKey();
        roomID = rootRef.push().getKey();
        // all files encrypte to own folder
        File createFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/crypto/");
        if (!createFile.exists())
            createFile.mkdir();
        for (int i = 0; i < fileEntities.size(); i++) {
            File toFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/crypto/" + fileEntities.get(i).getFileUid());
            AES.encrypt(new File(fileEntities.get(i).getRealAbsolyutPath()), toFile, aesKey);
            fileEntities.get(i).setRealAbsolyutPath(toFile.getAbsolutePath());
        }
        // UploadToFirebase AllFiles
        for (FileEntity fileEntity : fileEntities) {
            File fromFile = new File(fileEntity.getRealAbsolyutPath());
            UploadTask uploadTask = STORAGEREF.child(Uri.fromFile(fromFile).getLastPathSegment()).putFile(Uri.fromFile(fromFile));

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    if (++uploadedFilesCount == fileEntities.size()) {
                        //ALL FILES UPLOAD
                        //Add room Firebase
                        rootRef.child("Rooms/" + roomID + "/name").setValue(roomName.getText().toString());
                        for (final FileEntity fileEn : fileEntities) {
                            rootRef.child("Rooms/" + roomID + "/files/" + fileEn.getFileUid()).setValue(fileEn).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    File file = new File(fileEn.getRealAbsolyutPath());
                                    file.delete();
                                }
                            });
                        }

                        //CreateFile
                        File intoResours = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/crypto/" + roomName.getText().toString() + ".crytobyte");

                        try {
                            md5 = FileOperations.createCryptoFile(intoResours, TokenParser.parseToken(roomID, MyBase64.base64Encode(aesKey)), firebaseKey, AddFiles.this);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (NoSuchPaddingException e) {
                            e.printStackTrace();
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (InvalidKeyException e) {
                            e.printStackTrace();
                        }


                        //ADD emailControll
                        for (String email : emails) {
                            try {
                                rootRef.child("keys/" + MyBase64.base64Encode(AES.hashByte(md5, email))).setValue(MyBase64.base64Encode(firebaseKey));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            }
                        }
                        Toast.makeText(getApplicationContext(), "Success. Your key generate to: " + intoResours.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        hideProgressDialog();
                        finish();
                    }
                }
            });
        }
    }

    private static final int MY_PERMISSIONS_READ_WRITE = 38;

    private void getPermision() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_READ_WRITE);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_READ_WRITE);
            }
        }
    }

}
