package com.pat.gardadrone;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.graphics.Point;
import android.os.AsyncTask;
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
    private volatile boolean  fly;
    private Object lock;
    CopterClient ba = new CopterClient(this);
    private FlyTask flyTask;
    protected boolean gotStream;
    private GeoCoordinates currentDroneLocaion;

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
                flyTask = new FlyTask();
                flyTask.execute();
            }
        });
        //land the drone
        end.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                synchronized (lock) {
                    fly = false;
                }
                //return drone to launch point
                ba.RTL();
                //land drone
                ba.Land();
                //lock drone
                ba.doARM(false);
            }
        });

        ba.SetOnDataReceiveEvent(new DataReceiveEvent() {

            @Override
            public void Attitude(float arg0, float arg1, float arg2) {
                // TODO Auto-generated method stub
            }

            @Override
            public void Channels(short arg0, short arg1, short arg2,
                                 short arg3, short arg4, short arg5, short arg6, short arg7) {
                // TODO Auto-generated method stub
            }

            @Override
            public void GpsStatus(double arg0, double arg1, int arg2, int arg3) {
                currentDroneLocaion.setLatitude(arg0);
                currentDroneLocaion.setLongitude(arg1);
            }

            @Override
            public void HUDInfo(float arg0, float arg1, float arg2, float arg3,
                                int arg4, int arg5) {
                // TODO Auto-generated method stub
            }

            @Override
            public void Heartbeat(ac2modes arg0, boolean arg1) {
                // TODO Auto-generated method stub
                if (gotStream == false) {
                    gotStream = true;
                    ba.getCopterData();
                }
            }

            @Override
            public void SysStatus(float arg0) {
                // TODO Auto-generated method stub
            }

        });
    }

    private class FlyTask extends AsyncTask<Void, Void, Void> {

        // 1
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        //3
        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onCancelled(Void aVoid) {
            super.onCancelled(aVoid);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        // 2
        @Override
        protected Void doInBackground(Void... params) {
            ArrayList<GeoCoordinates> path = getAllpoints(lan, lng, 20, 8);
            while (true) {
                    //fly 8 points around the home points
                    for (int i = 0; i < path.size(); i++) {
                        //if drone at fist point in the path fly to next point in the path
                        if(currentDroneLocaion.equals(path.get(i))) {
                            ba.FlyTo(path.get(i + 1).getLatitude(), path.get(i + 1).getLatitude(), 20);
                        }
                        //fly drone to first point in the path
                        else {
                            ba.FlyTo(path.get(i).getLatitude(), path.get(i).getLongitude(), 20);
                        }
                    }
                    synchronized (lock) {
                        if (!fly) {
                            break;
                        }
                    }
            }
            return null;
        }
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
    private ArrayList<GeoCoordinates> getAllpoints(double lan, double lng, int range, int num) {
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