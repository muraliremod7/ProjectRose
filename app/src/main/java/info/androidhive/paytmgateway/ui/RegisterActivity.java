package info.androidhive.paytmgateway.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.valdesekamdem.library.mdtoast.MDToast;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import info.androidhive.paytmgateway.R;
import info.androidhive.paytmgateway.helper.ShowToast;
import info.androidhive.paytmgateway.services.GetAddressIntentService;

public class RegisterActivity extends BaseActivity {

    @BindView(R.id.input_name)
    EditText inputName;

    @BindView(R.id.input_email)
    EditText inputEmail;

    @BindView(R.id.input_mobile)
    EditText inputMobile;

    @BindView(R.id.input_rc)
    EditText inputRC;

    @BindView(R.id.input_state)
    EditText inputState;

    @BindView(R.id.input_city)
    EditText inputCity;

    @BindView(R.id.input_pincode)
    EditText inputPincode;

    @BindView(R.id.input_password)
    EditText inputPassword;

    @BindView(R.id.input_cpassword)
    EditText inputcPassword;

    @BindView(R.id.loader)
    AVLoadingIndicatorView loader;

    private ShowToast showToast;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2;
    private LocationAddressResultReceiver addressResultReceiver;
    private Location currentLocation;
    private LocationCallback locationCallback;
    private String fulladdress;
    private boolean first = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        makeFullScreen();
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        showToast = new ShowToast(this);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        changeStatusBarColor(ContextCompat.getColor(this, R.color.colorAccent));
        hideToolbar();
        LocationManager locationManager = (LocationManager) Objects.requireNonNull(this).getSystemService(LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            getLocationDetails();
        }else{
            showSettingsAlert();
        }

    }
    @Override
    public int getLayoutId() {
        return R.layout.activity_register;
    }

    @OnClick(R.id.btn_register)
    void onRegisterClick() {
        String name = inputName.getText().toString().replace("","");
        String email = inputEmail.getText().toString();
        String mobile = inputMobile.getText().toString();
        String rcnum = inputRC.getText().toString();
        String state = inputState.getText().toString();
        String city = inputCity.getText().toString();
        String pincode = inputPincode.getText().toString();
        String password = inputPassword.getText().toString();
        String cpassword = inputcPassword.getText().toString();
        if(!password.equals(cpassword)){
            showToast.showWarningToast("Password and confirm password should be same");
        }
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)||mobile.isEmpty()||rcnum.isEmpty()||state.isEmpty()||city.isEmpty()||pincode.isEmpty()||password.isEmpty()||cpassword.isEmpty()) {
            showToast.showWarningToast("Please fill the form !");
        }
        if(!isValidMobile(mobile)){
            showToast.showWarningToast("Enter Valid Mobile Number");
        }
        if(!isValidMail(email)){
            showToast.showWarningToast("Enter Valid Email");
        }

        registerUser(name,email,mobile,state,city,pincode,rcnum,password,fulladdress);
        loader.setVisibility(View.VISIBLE);

    }
    @OnClick(R.id.insurance_copy)
    void OninsuranceCopy(){

    }
    @OnClick(R.id.proofof_identification)
    void OnProofOfIdentification(){

    }
    private void registerUser(String name, String email, String mobile, String state, String city, String pincode, String rcnum, String password, String fulladdress) {

    }

    @OnClick(R.id.btn_login_account)
    void onCreateAccountClick() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onCreateAccountClick();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    private boolean isValidMobile(String phone) {
        return android.util.Patterns.PHONE.matcher(phone).matches();
    }
    private boolean isValidMail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private class LocationAddressResultReceiver extends ResultReceiver {
        LocationAddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            if (resultCode == 0) {
                //Last Location can be null for various reasons
                //for example the api is called first time
                //so retry till location is set
                //since intent service runs on background thread, it doesn't block main thread
                Log.d("Address", "Location null retrying");
                getAddress();
            }

            if (resultCode == 1) {
                Toast.makeText(RegisterActivity.this,
                        "Address not found, " ,
                        Toast.LENGTH_SHORT).show();
            }
            Address address = resultData.getParcelable("address_result");
            assert address != null;
            String state = address.getAdminArea();
            String city = address.getLocality();
            String place = address.getSubLocality();
            String pincode = address.getPostalCode();
            if(first){

                inputState.setText(state);
                inputCity.setText(city);
                inputPincode.setText(pincode);
                fulladdress = resultData.getString("address");
                first = false;
            }
        }
    }
    @SuppressWarnings("MissingPermission")
    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            @SuppressLint("RestrictedApi")
            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setInterval(2000);
            locationRequest.setFastestInterval(1000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    null);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
    @SuppressWarnings("MissingPermission")
    private void getAddress() {

        if (!Geocoder.isPresent()) {
            Toast.makeText(RegisterActivity.this,
                    "Can't find current address, ",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, GetAddressIntentService.class);
        intent.putExtra("add_receiver", addressResultReceiver);
        intent.putExtra("add_location", currentLocation);
        startService(intent);
    }
    private void getLocationDetails() {
        addressResultReceiver = new LocationAddressResultReceiver(new Handler());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
            @Override
            public void onLocationResult(LocationResult locationResult) {
                currentLocation = locationResult.getLocations().get(0);
                String latlan = currentLocation.getLatitude()+","+currentLocation.getLongitude();
                getAddress();
            }
        };
        startLocationUpdates();
    }
}
