package gr.kalymnos.skemelio.wifip2pseperatemodulestest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import gr.kalymnos.skemelio.wifip2pseperatemodulestest.client.ClientActivity;
import gr.kalymnos.skemelio.wifip2pseperatemodulestest.server.ServerActivity;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MALAKIA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onServerClick(View view){
        Intent intent = new Intent(this,ServerActivity.class);
        startActivity(intent);
    }

    public void onClientClick(View view){
        Intent intent = new Intent(this,ClientActivity.class);
        startActivity(intent);
    }
}
