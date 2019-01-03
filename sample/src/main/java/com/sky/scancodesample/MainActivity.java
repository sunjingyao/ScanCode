package com.sky.scancodesample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.skytech.scancode.CaptureActivity;
import com.skytech.scancode.ScanCallback;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onScanQRCode(View v) {
        CaptureActivity.setCallback(new ScanCallback() {
            @Override
            public void result(String uri) {
                Toast.makeText(MainActivity.this, uri, Toast.LENGTH_LONG).show();
                CaptureActivity.getInstance().finish();
            }
        });
        startActivity(new Intent(this, CaptureActivity.class));
    }
}
