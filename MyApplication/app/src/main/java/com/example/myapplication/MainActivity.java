package com.example.myapplication;

import android.graphics.Color;
import android.hardware.Sensor;
import android.content.Context;
import android.widget.ImageView;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;
import android.hardware.SensorEvent;

public class MainActivity extends AppCompatActivity {
    private SensorManager sensorManger;
    private Sensor sensor;
    private SensorEventListener sensorlistener;
    ImageView image;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManger = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor =  sensorManger.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (sensor == null){
            image = (ImageView) findViewById(R.id.view1);
            Toast.makeText(this, "Hello World!", Toast.LENGTH_SHORT).show();
            finish();
        }
        sensorlistener = new SensorEventListener(){
            @Override
            public void onSensorChanged(SensorEvent sensorevent){

                if(sensorevent.values[2]>0.5f){
                    getWindow().getDecorView().setBackgroundColor(Color.BLUE);
                }
                else if(sensorevent.values[2]<-0.5f){
                    getWindow().getDecorView().setBackgroundColor(Color.YELLOW);

                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int i){

            }
        };
    }
    @Override
    protected void onResume(){
        super.onResume();
        sensorManger.registerListener(sensorlistener ,sensor, SensorManager.SENSOR_DELAY_FASTEST );
    }
    @Override
    protected void onPause(){
        super.onPause();
        sensorManger.unregisterListener(sensorlistener );
    }

}