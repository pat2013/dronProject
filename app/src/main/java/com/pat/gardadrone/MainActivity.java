package com.pat.gardadrone;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.graphics.Point;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import com.ehang.gcs_amap.comms.CopterClient;
import com.ehang.gcs_amap.comms.CopterClient.ac2modes;
import com.ehang.gcs_amap.comms.CopterClient.BluetoothEvent;
import com.ehang.gcs_amap.comms.CopterClient.DataReceiveEvent;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.pat.gardadrone.GeoCoordinates;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends ActionBarActivity {
    private Button setHome;
    private Button initiative;
    private Button start;
    private Button end;
    private EditText latlng;
    private String string;
    private String[] result;
    private double lan;
    private double lng;
    private boolean fly;
    CopterClient ba = new CopterClient(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setHome = (Button) findViewById(R.id.button);
        initiative = (Button) findViewById(R.id.button2);
        start = (Button) findViewById(R.id.button3);
        end = (Button) findViewById(R.id.button4);

        //connect with drone
        initiative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //connect to bluetooth device
                ba.connect("98:D3:31:05:02:D0");
                //unlock drone
                ba.SetOnBluetoothConnected(new BluetoothEvent() {
                    @Override
                    public void DisConnect() {
                        popMessage("Bluetooth disconnected!");
                    }
                    @Override
                    public void Connect() {
                        ba.doARM(true);
                        popMessage("Bluetooth connected!");
                    }
                });
                //drone takeoff
                if (CopterClient.armed) {
                    ba.takeoff();
                }
            }
        });

        //set home address
        setHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                latlng = (EditText) findViewById(R.id.editText);
                //check input string
                if(latlng.getText().toString().isEmpty()||ifVailidCoordinates(string)){
                    popMessage("Invalid input");
                    //input invalid drone maintains the current location
                    ba.SetLoiter();
                }
                else{
                    string = latlng.getText().toString();
                    result = string.split(",");
                    lan = changeStringToDouble(result[0]);
                    lng = changeStringToDouble(result[1]);
                    popMessage("set home (" + lan +" " + lng + ")");
                }
            }
        });

        //start watching house
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(String.valueOf(lan), String.valueOf(lng));
                while(fly) {
                    //fly 8 points around the home points
                   for(GeoCoordinates point : getAllpoints(lan, lng, 0.1, 8)) {
                       ba.FlyTo(point.getLatitude(), point.getLongitude(), 20);
                   }
                }
            }
        });
        //land the drone
        end.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fly = false;
                //return drone to launch point
                ba.RTL();
                //land drone
                ba.Land();
                //lock drone
                ba.doARM(false);
            }
        });
    }

    // toast maker
    private void popMessage(String s) {
        Context context = getApplicationContext();
        CharSequence text = s;
        int duration = Toast.LENGTH_SHORT;
        Toast.makeText(context, text, duration).show();

    }
    //check latitude/longitude coordinates
    private boolean ifVailidCoordinates(String s) {
        if(s == null || s.length() == 0) {
            return false;
        }
        Pattern p = Pattern.compile("^([-+]?\\d{1,2}([.]\\d+)?),\\s*([-+]?\\d{1,3}([.]\\d+)?)$");
        Matcher m = p.matcher(s);
        if(m.find()) {
            return true;
        }
        return false;
    }
    //string to double
    private double changeStringToDouble(String s) {
        return Double.valueOf(s);
    }
    //generate 8 points around home address
    private ArrayList<GeoCoordinates> getAllpoints(double lan, double lng, double range, int num) {
        int degreesPerPoint = 360 / num;
        // Keep track of the angle from centre to radius
        int currentAngle = 0;
        // The points on the radius will be lat+x, long+y
        double x;
        double y;
        // Track the points we generate to return at the end
        ArrayList<GeoCoordinates> points = new ArrayList<GeoCoordinates>();
        for(int i=0; i < num; i++) {
            // X point will be cosine of angle * radius (range)
            x = Math.cos(currentAngle) * range;
            // Y point will be sin * range
            y = Math.sin(currentAngle) * range;
            GeoCoordinates newPoint = new GeoCoordinates(lan+x, lng+y);
            // save to our results array
            points.add(newPoint);
            // Shift our angle around for the next point
            currentAngle += degreesPerPoint;
        }
        // Return the points we've generated
        return points;
    }

    @Override
    protected void onDestroy() {
        ba.Disconnect();
        ba.onDestroy();
        super.onDestroy();
    }

}