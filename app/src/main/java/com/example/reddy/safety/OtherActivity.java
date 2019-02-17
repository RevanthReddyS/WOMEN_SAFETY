package com.example.reddy.safety;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Locale;


public class OtherActivity extends AppCompatActivity {
    String mess;
    EditText output;
    GPSTracker gpsTracker;
    private SensorManager mSensorManager;
    private static final String TAG = OtherActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    public TextToSpeech textToSpeech;
    public String source = "en-UK", target = "en";
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private static final String API_KEY = "AIzaSyAtbKyjPMRlB7Lcqa75Mqf03O822ABHJAA";
    public final Handler handler = new Handler();
    FloatingActionButton btnSpeak;
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;

    ArrayList<ComplainPOJO> complaints = new ArrayList<>();
    // ArrayList<User> user=new ArrayList<User>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other);
        btnSpeak = (FloatingActionButton) findViewById(R.id.btnSpeak);
        checkForSmsPermission();

        ListView complaint_list = findViewById(R.id.other_list);
        final ComplainAdpater complainAdpater = new ComplainAdpater(this, complaints);
        complaint_list.setAdapter(complainAdpater);


        btnSpeak.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick (View view){
                promptSpeechInput();
            }
        });


        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.UK);
                }
            }
        });


        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("complaints");
        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                complaints.add(dataSnapshot.getValue(ComplainPOJO.class));
                complainAdpater.notifyDataSetChanged();

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        FloatingActionButton location = findViewById(R.id.btnSpeak);
        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSpeechInput();
            }
        });
        FloatingActionButton fab = findViewById(R.id.addComplaint);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(OtherActivity.this, TakeComplaint.class));
            }
        });


        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {

            @Override
            public void onShake(int count) {
                Log.d(TAG, "in shake");
                Toast.makeText(OtherActivity.this, "Phone Is Shook: Sending message to Emergency Contact. ", Toast.LENGTH_SHORT).show();
                Log.i("ONshake ", "Phone is shaked send the mail and message.");

                checkForSmsPermission();
                // Use SmsManager
                Context context = getApplicationContext();
                gpsTracker = new GPSTracker(context);

                SmsManager smsManager = SmsManager.getDefault();
                String urlWithPrefix = "";
                if (gpsTracker.isGPSEnabled) {
                    String stringLatitude = String.valueOf(gpsTracker.latitude);
                    String stringLongitude = String.valueOf(gpsTracker.longitude);
                    urlWithPrefix = " and I am at https://www.google.co.in/maps/@" + stringLatitude + "," + stringLongitude + ",19z";

                } else {
                    Toast.makeText(context, "Your GPS is OFF", Toast.LENGTH_LONG).show();
                }
                String message = "Emergency";
                message = message + urlWithPrefix;

                smsManager.sendTextMessage("ADD YOUR NUMBER HERE", null, message,
                        null, null);
                Toast.makeText(OtherActivity.this, "message sent", Toast.LENGTH_LONG);
            }
        });

    }




    private void promptSpeechInput() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, source);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say Something");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(), "Sorry! Your device does not support speech input", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Receiving speech input
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String userQuery = result.get(0);
                   CharSequence ob1="help";
                   CharSequence ob2="emergency";
                    CharSequence ob3="save";
                   if(userQuery.contains(ob1)||userQuery.contains(ob2)||userQuery.contains(ob3)){
                       Toast.makeText(OtherActivity.this, "Emergency message is being sent ", Toast.LENGTH_SHORT).show();


                       checkForSmsPermission();
                       // Use SmsManager
                       Context context = getApplicationContext();
                       gpsTracker = new GPSTracker(context);

                       SmsManager smsManager = SmsManager.getDefault();
                       String urlWithPrefix = "";
                       if (gpsTracker.isGPSEnabled) {
                           String stringLatitude = String.valueOf(gpsTracker.latitude);
                           String stringLongitude = String.valueOf(gpsTracker.longitude);
                           urlWithPrefix = " and I am at https://www.google.co.in/maps/@" + stringLatitude + "," + stringLongitude + ",19z";

                       } else {
                           Toast.makeText(context, "Your GPS is OFF", Toast.LENGTH_LONG).show();
                       }
                       String message = "Emergency";
                       message = message + urlWithPrefix;

                       smsManager.sendTextMessage("7993295977", null, message,
                               null, null);
                       Toast.makeText(OtherActivity.this, "message sent", Toast.LENGTH_LONG);
                   }

                   }

                }
                break;
            }

        }







    private void checkForSmsPermission() {
        Log.d(TAG,"in permission");
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    MY_PERMISSIONS_REQUEST_SEND_SMS);
        }
        else
            Toast.makeText(this, "got permissions",
                    Toast.LENGTH_LONG).show();

    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d(TAG,"in on request");
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (permissions[0].equalsIgnoreCase(Manifest.permission.SEND_SMS)
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Log.d(TAG,"permission denied");
                    // Permission denied.
                    Toast.makeText(this, "failed to get permissions",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        // Add the following line to register the Session Manager Listener onResume
        mSensorManager.registerListener(mShakeDetector, mAccelerometer,	SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        // Add the following line to unregister the Sensor Manager onPause
        mSensorManager.unregisterListener(mShakeDetector);
        super.onPause();
    }
    public void Send(String s){
        SmsManager smsManager = SmsManager.getDefault();
        String urlWithPrefix = "";
        if(gpsTracker.isGPSEnabled) {
            String stringLatitude = String.valueOf(gpsTracker.latitude);
            String stringLongitude = String.valueOf(gpsTracker.longitude);
            urlWithPrefix = " and I am at https://www.google.co.in/maps/@" + stringLatitude + "," + stringLongitude + ",19z";

        }else{
            Toast.makeText(getApplicationContext(),"Your GPS is OFF", Toast.LENGTH_LONG).show();
        }
       String message="Emegergency";
       message=message+urlWithPrefix;

        smsManager.sendTextMessage("9490962158", null,message,
                null, null);
        Toast.makeText(OtherActivity.this,"message sent",Toast.LENGTH_LONG);
    }

    @Override
    public void onBackPressed() {

    }
}


