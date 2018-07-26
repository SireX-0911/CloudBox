package is.com.cryptobayt;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.NoSuchPaddingException;

import is.com.cryptobayt.crypt.AES;
import is.com.cryptobayt.crypt.MyBase64;
import is.com.cryptobayt.crypt.TokenParser;
import is.com.cryptobayt.entity.FileEntity;

public class ViewBucket extends AppCompatActivity {
    RecyclerView rvFileList;
    ArrayList<FileEntity> fileEntities;
    FileAdapter fileAdapter;
    TextView buckedName;
    private FirebaseDatabase dataset = FirebaseDatabase.getInstance();
    private DatabaseReference rootRef = dataset.getReference();
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference STORAGEREF = storage.getReference();
    String token = "";
    String buckedNameStr = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_bucket);
        getPermision();
        fileEntities = new ArrayList<>();
        buckedName = findViewById(R.id.buckedName);
        rvFileList = findViewById(R.id.rvFileList);
        rvFileList.setLayoutManager(new GridLayoutManager(this, 4));
        fileAdapter = new FileAdapter();
        rvFileList.setAdapter(fileAdapter);

        if (getIntent().getExtras() != null)
            token = getIntent().getExtras().getString("token");

        rootRef.child("Rooms/" + TokenParser.getRoomID(token)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot filesSnap : dataSnapshot.child("files").getChildren()) {
                    FileEntity fileE = filesSnap.getValue(FileEntity.class);
                    fileE.setFileUid(filesSnap.getKey());
                    fileEntities.add(fileE);
                }
                buckedNameStr = dataSnapshot.child("name").getValue(String.class);
                buckedName.setText(buckedNameStr);

                fileAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


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
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            holder.filename.setText(fileEntities.get(position).getName());
            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    File createFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/crypto/" + buckedNameStr + "/");
                    if (!createFile.exists())
                        createFile.mkdir();
                    final File checkHaveOrNot = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/crypto/" + buckedNameStr + "/" + fileEntities.get(position).getName());
                    if (checkHaveOrNot.exists()) {

                        Toast.makeText(getApplicationContext(), "File located: " + checkHaveOrNot.getParent(), Toast.LENGTH_SHORT).show();

                        return;
                    }
                    final File crypteFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/crypto/" + buckedNameStr + "/x" + fileEntities.get(position).getName());
                    showProgressDialog();
                    STORAGEREF.child(fileEntities.get(position).getFileUid()).getFile(crypteFile).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                            try {
                                AES.decrypt(crypteFile, checkHaveOrNot, MyBase64.base64Decode(TokenParser.getAESKey(token)));
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            } catch (NoSuchPaddingException e) {
                                e.printStackTrace();
                            } catch (InvalidKeyException e) {
                                e.printStackTrace();
                            }
                            if (checkHaveOrNot.exists()) {
                                crypteFile.delete();
                                hideProgressDialog();
                                Toast.makeText(getApplicationContext(), "File Download to: " + checkHaveOrNot.getParent(), Toast.LENGTH_SHORT).show();
                            }
                            notifyDataSetChanged();
                        }
                    });

                }
            });


            File checkHaveOrNot = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/crypto/" + buckedNameStr + "/" + fileEntities.get(position).getName());
            if (checkHaveOrNot.exists()) {
                holder.imageIcon.setImageResource(R.drawable.filedown);
                fileEntities.get(position).setRealAbsolyutPath(checkHaveOrNot.getAbsolutePath());
            } else {
                holder.imageIcon.setImageResource(R.drawable.file);
                fileEntities.get(position).setRealAbsolyutPath("");
            }


        }

        @Override
        public int getItemCount() {
            return fileEntities.size();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView filename;
        ImageView imageIcon;
        View view;

        public ViewHolder(View v) {
            super(v);
            view = v;
            filename = v.findViewById(R.id.filename);
            imageIcon = v.findViewById(R.id.imageIcon);
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

    private ProgressDialog mProgressDialog;

    public void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("Downloading");
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }

    private String fileExt(String url) {
        if (url.indexOf("?") > -1) {
            url = url.substring(0, url.indexOf("?"));
        }
        if (url.lastIndexOf(".") == -1) {
            return null;
        } else {
            String ext = url.substring(url.lastIndexOf(".") + 1);
            if (ext.indexOf("%") > -1) {
                ext = ext.substring(0, ext.indexOf("%"));
            }
            if (ext.indexOf("/") > -1) {
                ext = ext.substring(0, ext.indexOf("/"));
            }
            return ext.toLowerCase();

        }
    }
}
