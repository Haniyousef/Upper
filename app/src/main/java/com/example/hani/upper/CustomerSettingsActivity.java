package com.example.hani.upper;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class CustomerSettingsActivity extends AppCompatActivity {
    EditText nameFeild , phoneFeild ;
    Button confirm_btn , back_btn ;
    private FirebaseAuth mAuth ;
    private DatabaseReference mRef ;
    private String userId , name , phone;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_settings);
        nameFeild=(EditText)findViewById(R.id.name_edit);
        phoneFeild=(EditText)findViewById(R.id.phone_edit);
        confirm_btn=(Button)findViewById(R.id.confirm_btn);
        back_btn=(Button) findViewById(R.id.back_btn);

        mAuth=FirebaseAuth.getInstance();
        userId=mAuth.getCurrentUser().getUid();
        mRef= FirebaseDatabase.getInstance().getReference().child("Users").child("Customer").child(userId);

        getCustomerInformation();

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
               }
           }

           @Override
           public void onCancelled(DatabaseError databaseError) {

           }
       });
    }

    private void saveCustomerInformation(){
        name = nameFeild.getText().toString();
        phone=phoneFeild.getText().toString();
        Map info=new HashMap();
        info.put("name",name);
        info.put("phone",phone);
        mRef.updateChildren(info).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if (task.isSuccessful()){
                    Toast.makeText(CustomerSettingsActivity.this, "data saved", Toast.LENGTH_SHORT).show();
                    finish();
                }else {
                    Toast.makeText(CustomerSettingsActivity.this, "faild to save", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
