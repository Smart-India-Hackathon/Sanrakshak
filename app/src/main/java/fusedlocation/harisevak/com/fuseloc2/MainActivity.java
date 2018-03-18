package fusedlocation.harisevak.com.fuseloc2;
//Jai Shree Ram.
import android.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    FirebaseDatabase database;
    DatabaseReference myRefLat;
    DatabaseReference myRefLong;
    DatabaseReference myLocId;
    DatabaseReference guardDetails;

    RelativeLayout pingBtnLayout;
    LinearLayout pingInterfaceLayout;

    private static final int MY_PERMISSION_REQUEST_CODE = 7171;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 7172;
    private Button btnGetCoordinates, btnLocation;
    private boolean mRequestingLocationUpdates = false;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private Button btnGoBackPing;
    private ImageButton emergencyBtn;

    private static int UPDATE_INTERVAL = 5000; //ms
    private static int FASTEST_INTERVAL = 3000; //ms
    private static int DISPLACEMENT = 2; //m
    public static int i=0;
    public double latitude;
    public double longitude;

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:

                if(event.isLongPress()==true) {  //start new activity B
                    Toast.makeText(getApplicationContext(),"SOS Signal Sent",Toast.LENGTH_LONG).show();
                    Intent myIntent = new Intent(MainActivity.this, EmergencyActivity.class);
                    MainActivity.this.startActivity(myIntent);
                    return true;
                }

            default:
                return super.dispatchKeyEvent(event);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPlayServices()) {
                        buildGoogleApiClient();
                    }
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnGetCoordinates = (Button) findViewById(R.id.btnGetCoordinates);
        btnLocation = (Button) findViewById(R.id.btnTrackLocation);
        btnGoBackPing= findViewById(R.id.btnGoBackPing);
        pingBtnLayout= (RelativeLayout) findViewById(R.id.pingBtnLayout);
        pingInterfaceLayout= (LinearLayout) findViewById(R.id.pingInterfaceLayout);
        emergencyBtn= findViewById(R.id.emergencyBtn);

        pingInterfaceLayout.setVisibility(View.GONE);


        emergencyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"SOS Signal Sent",Toast.LENGTH_LONG).show();
                Intent myIntent = new Intent(MainActivity.this, EmergencyActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        });


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, MY_PERMISSION_REQUEST_CODE);
        } else {
            if (checkPlayServices()) {
                buildGoogleApiClient();
                CreateLocationRequet();
            }
        }

        btnGetCoordinates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                pingInterfaceLayout.setVisibility(View.VISIBLE);
                pingBtnLayout.setVisibility(View.GONE);
                displayLocation();
            }
        });

        btnGoBackPing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pingBtnLayout.setVisibility((View.VISIBLE));
                pingInterfaceLayout.setVisibility(View.GONE);
            }
        });

        btnLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePerioicUpdates();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mGoogleApiClient!=null){
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        if(mGoogleApiClient !=null){
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    private void togglePerioicUpdates() {
        if(!mRequestingLocationUpdates){
            btnLocation.setText("End Patrolling");
            mRequestingLocationUpdates= true;
            startLocationUpdates();
        }
        else{
            btnLocation.setText(("Resume Patrolling"));
            mRequestingLocationUpdates= false;
            stopLocationUpdates();
        }
    }

    public void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
            updateFirebase();
            i++;
        }
    }

    public void updateFirebase(){
        database= FirebaseDatabase.getInstance();
        guardDetails= database.getReference("Guard1/guardDetails/g_id");
        guardDetails.setValue("1");
        myRefLat= database.getReference("Guard1/locations/"+i+"/lat");
        myRefLat.setValue(latitude);
        myRefLong= database.getReference("Guard1/locations/"+i+"/lng");
        myRefLong.setValue(longitude);
        myLocId= database.getReference("Guard1/locations/"+i+"/id");
        myLocId.setValue(i);
    }

    private void CreateLocationRequet() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(), "This Application is not supported", Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void stopLocationUpdates(){
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        if(mRequestingLocationUpdates)
            startLocationUpdates();
    }


    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation= location;
        displayLocation();
    }

}
