package com.example.hani.upper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DriverSettingsActivity extends AppCompatActivity {

    EditText nameFeild , phoneFeild , carFeild ;
    Button confirm_btn , back_btn ;
    private FirebaseAuth mAuth ;
    private DatabaseReference mRef ;
    private String userId , name , phone , car , service, profileImageUrl;
    private ImageView profile ;
    private Uri resultUri ;
    RadioGroup radioGroup ;
    RadioButton radioButton ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_settings);
        nameFeild=(EditText)findViewById(R.id.name_edit);
        phoneFeild=(EditText)findViewById(R.id.phone_edit);
        carFeild=(EditText)findViewById(R.id.car_edit);
        confirm_btn=(Button)findViewById(R.id.confirm_btn);
        back_btn=(Button) findViewById(R.id.back_btn);
        profile=(ImageView) findViewById(R.id.image_profile);
        radioGroup=(RadioGroup)findViewById(R.id.radio_group);

        mAuth=FirebaseAuth.getInstance();
        userId=mAuth.getCurrentUser().getUid();
        mRef= FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId);

        getCustomerInformation();

        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,1);
            }
        });

        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //   startActivity(new Intent(CustomerSettingsActivity.this,CustomerMapActivity.class));
                finish();
                return;
            }
        });

        confirm_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCustomerInformation();
            }
        });
    }

    private void getCustomerInformation(){
        mRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0) {
                    Map<String,Object> map = (Map<String,Object>) dataSnapshot.getValue();
                    if (map.get("name")!= null){
                        name=map.get("name").toString();
                        nameFeild.setText(name);
                    }
                    if (map.get("phone") != null){
                        phone = map.get("phone").toString();
                        phoneFeild.setText(phone);
                    }
                    if (map.get("car") != null){
                        car = map.get("car").toString();
                        carFeild.setText(car);
                    }
                    if (map.get("service") != null){
                        service = map.get("service").toString();
                        switch (service){
                            case "uberx" :
                                radioGroup.check(R.id.uberx);
                                break;
                            case "uberBlack" :
                                radioGroup.check(R.id.uberBlack);
                                break;
                            case "uberxl" :
                                radioGroup.check(R.id.uberxl);
                                break;
                        }
                    }
                    if (map.get("imageProfile") != null){
                        profileImageUrl = map.get("imageProfile").toString();
                        Glide.with(DriverSettingsActivity.this).load(profileImageUrl).into(profile);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void saveCustomerInformation(){
        int selectedId=radioGroup.getCheckedRadioButtonId();
        radioButton=(RadioButton)findViewById(selectedId);
        if (radioButton.getText().toString() == null){
            return;
        }

        name = nameFeild.getText().toString();
        phone=phoneFeild.getText().toString();
        car=carFeild.getText().toString();
        service=radioButton.getText().toString();
        Map info=new HashMap();
        info.put("name",name);
        info.put("phone",phone);
        info.put("car",car);
        info.put("service",service);
        mRef.updateChildren(info).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if (task.isSuccessful()){
                    Toast.makeText(DriverSettingsActivity.this, "data saved", Toast.LENGTH_SHORT).show();
                    finish();
                }else {
                    Toast.makeText(DriverSettingsActivity.this, "faild to save", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // save image
        if (resultUri!=null){
            StorageReference storage= FirebaseStorage.getInstance().getReference().child("profile_images").child(userId);
            Bitmap bitmap=null ;
            try {
                bitmap= MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(),resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,20,baos);
            byte[] data =baos.toByteArray();
            final UploadTask task=storage.putBytes(data);

            task.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(DriverSettingsActivity.this,"error upload image", Toast.LENGTH_SHORT).show();
                }
            });

            task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri img_url= taskSnapshot.getDownloadUrl();
                    Map new_image=new HashMap();
                    new_image.put("imageProfile",img_url.toString());
                    mRef.updateChildren(new_image);
                    finish();
                    return;
                }
            });

        }else{
            Toast.makeText(this,"error upload image", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==1 && resultCode== Activity.RESULT_OK){
            final Uri imageUri=data.getData();
            resultUri=imageUri ;
            profile.setImageURI(resultUri);
        }
    }
}
