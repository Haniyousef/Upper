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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CustomerLoginActivity extends AppCompatActivity {
    EditText mPass , mEmail ;
    Button mLogin , mRegister ;
    FirebaseAuth firebaseAuth ;
    FirebaseAuth.AuthStateListener authStateListener ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        firebaseAuth=FirebaseAuth.getInstance();
        authStateListener= new FirebaseAuth.AuthStateListener() {  // that is for login at first time only and donot show this activity every time
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();   // get information about current loged in user
                if (user != null){
                    startActivity(new Intent(CustomerLoginActivity.this,CustomerMapActivity.class));
                    finish();
                    return;
                }
            }
        };
        mEmail=(EditText)findViewById(R.id.email_edit);
        mPass=(EditText)findViewById(R.id.pass_edit);
        mLogin=(Button)findViewById(R.id.login_btn);
        mRegister=(Button)findViewById(R.id.register_btn);

        mRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String email=mEmail.getText().toString();
                final String pass=mPass.getText().toString();

                firebaseAuth.createUserWithEmailAndPassword(email,pass).addOnCompleteListener(CustomerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()){
                            Toast.makeText(CustomerLoginActivity.this, "Register Failed", Toast.LENGTH_SHORT).show();
                        }else {
                            String user_id=firebaseAuth.getCurrentUser().getUid();   // get current user id from registeration
                            DatabaseReference ref= FirebaseDatabase.getInstance().getReference().child("Users").child("Customer").child(user_id);
                            ref.setValue(true);
                        }
                    }
                });
            }
        });


        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String email=mEmail.getText().toString();
                final String pass=mPass.getText().toString();
                firebaseAuth.signInWithEmailAndPassword(email,pass).addOnCompleteListener(CustomerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()){
                            Toast.makeText(CustomerLoginActivity.this, "Register Failed", Toast.LENGTH_SHORT).show();
                        }
                        // if successful will perform authStateListener in onCreat

                    }
                });
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseAuth.removeAuthStateListener(authStateListener);
    }
}
