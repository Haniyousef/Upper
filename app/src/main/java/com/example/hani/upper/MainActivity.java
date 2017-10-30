package com.example.hani.upper;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    Button driverBtn , customerBtn ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        driverBtn=(Button)findViewById(R.id.driver_btn);
        customerBtn=(Button)findViewById(R.id.customer_btn);
        driverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,DriverLoginActivity.class));
                finish();
                return;
            }
        });


        customerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,CustomerLoginActivity.class));
                finish();
                return;
            }
        });
    }
}
