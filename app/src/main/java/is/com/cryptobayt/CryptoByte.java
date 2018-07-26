package is.com.cryptobayt;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import is.com.cryptobayt.crypt.AES;
import is.com.cryptobayt.crypt.Md5;
import is.com.cryptobayt.crypt.MyBase64;
import is.com.cryptobayt.utils.FileOperations;
import is.com.cryptobayt.utils.SignInGoogleMoneyHold;

public class CryptoByte extends AppCompatActivity {
    String TAG = "CryptoByteTest";
    public SignInGoogleMoneyHold reg;
    LinearLayout llEmail;
    EditText etEmail,etChoiseKey;
    Uri keyFileUri;
    TextView createRoom,tvSearchRoom;

    private FirebaseDatabase dataset= FirebaseDatabase.getInstance();
    private DatabaseReference rootRef = dataset.getReference();
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference STORAGEREF = storage.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crypto_byte);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getPermision();
        llEmail = findViewById(R.id.llEmail);
        etEmail = findViewById(R.id.etEmail);
        etChoiseKey = findViewById(R.id.etChoiseKey);
        createRoom = findViewById(R.id.createRoom);
        tvSearchRoom = findViewById(R.id.tvSearchRoom);

        reg = new SignInGoogleMoneyHold(this, new SignInGoogleMoneyHold.UpdateSucsess() {
            @Override
            public void updateToSucsess() {
                succesLogin();
            }

            @Override
            public void updateToFailed() {
                failedLogin();
            }
        });
        if(FirebaseAuth.getInstance().getCurrentUser()!=null){
            etEmail.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        }
        etChoiseKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                etChoiseKey.setError(null);
                showFileChooser();
            }
        });
        etEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                etEmail.setError(null);
                if(FirebaseAuth.getInstance().getCurrentUser()==null){
                    reg.regitUser();
                }else {
                    reg.revokeAccess();
                    reg.regitUser();
                }
            }
        });

        createRoom.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if(etEmail.getText().equals("")){
                    etEmail.setError("Sign in");
                    return;
                }
                Intent intent = new Intent(CryptoByte.this,AddFiles.class);
                intent.putExtra("email",etEmail.getText().toString());
                startActivity(intent);
            }
        });
        tvSearchRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(etEmail.getText().equals("")){
                    etEmail.setError("Sign in");
                    return;
                }
                if(keyFileUri ==null)
                {
                    etChoiseKey.setError("Choise File");
                    return;
                }
                if(!keyFileUri.getLastPathSegment().substring(keyFileUri.getLastPathSegment().lastIndexOf(".")).equals(".crytobyte")){
                    etChoiseKey.setError("Choise Only Crytobyte Key");
                    return;
                }
                showProgressDialog();
                String md5 = Md5.calculateMD5(new File(keyFileUri.getPath()));
                String email = etEmail.getText().toString();
                String firebasehash="";
                try {
                    firebasehash= MyBase64.base64Encode(AES.hashByte(md5,email));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "firebasehash: "+firebasehash);
                rootRef.child("keys/"+firebasehash).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.getValue()!=null){
                            File createFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/crypto/");
                            if(!createFile.exists())
                                createFile.mkdir();
                            File toFile = new File(createFile.getAbsolutePath()+"/x"+keyFileUri.getLastPathSegment());

                            try {
                                AES.decrypt(new File(keyFileUri.getPath()),toFile,MyBase64.base64Decode(dataSnapshot.getValue(String.class)));
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            } catch (NoSuchPaddingException e) {
                                e.printStackTrace();
                            } catch (InvalidKeyException e) {
                                e.printStackTrace();
                            }
                            hideProgressDialog();
                            String token= new String(FileOperations.readFromFile(toFile));
                            toFile.delete();
                            Intent intent = new Intent(CryptoByte.this,ViewBucket.class);
                            intent.putExtra("token",token);
                            startActivity(intent);

                        } else {
                            etChoiseKey.setError("Nonexistent folder. KEY NOT CORRECT OR NOT HAVE ACCESS");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        });
        try {
            Log.d(TAG, "md5: "+ MyBase64.base64Encode("Sardor".getBytes()));
            String s= new String(MyBase64.base64Decode(MyBase64.base64Encode("Sardor".getBytes())));
            Log.d(TAG, "md5: "+s);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
    public void succesLogin(){
        if(FirebaseAuth.getInstance().getCurrentUser()!=null)
        etEmail.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());

    }
    public void failedLogin(){
        if(FirebaseAuth.getInstance().getCurrentUser()==null)
            etEmail.setText("");

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SignInGoogleMoneyHold.RC_SIGN_IN){
            reg.regitRequstGet(data);
        }else
        if(requestCode == FILE_SELECT_CODE) {
            if (resultCode == RESULT_OK) {
                String uri = FileOperations.getPath(this,data.getData());
                keyFileUri = Uri.parse(uri);
                Log.d(TAG, "File Path: " + keyFileUri.getLastPathSegment());
                if(!keyFileUri.getLastPathSegment().substring(keyFileUri.getLastPathSegment().lastIndexOf(".")).equals(".crytobyte")){
                    etChoiseKey.setError("Choise Only Crytobyte Key");
                    Toast.makeText(getApplicationContext(),"Choise Only Crytobyte Key",Toast.LENGTH_SHORT).show();
                    return;
                }
                etChoiseKey.setText(keyFileUri.getLastPathSegment());
            }
        }


    }


    private static final int FILE_SELECT_CODE = 120;

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
    private static final int MY_PERMISSIONS_READ_WRITE = 38;

    private void getPermision(){
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions( this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_READ_WRITE);
            } else {
                ActivityCompat.requestPermissions( this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_READ_WRITE);
            }
        }
    }
    private ProgressDialog mProgressDialog;

    public void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("TRYING OPEN FOLDER");
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }
}
