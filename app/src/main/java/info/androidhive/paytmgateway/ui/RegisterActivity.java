package info.androidhive.paytmgateway.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import info.androidhive.paytmgateway.R;
import info.androidhive.paytmgateway.helper.ImagePickerActivity;
import info.androidhive.paytmgateway.helper.ShowToast;
import info.androidhive.paytmgateway.services.GetAddressIntentService;
import timber.log.Timber;

public class RegisterActivity extends BaseActivity {
    private static final int REQUEST_IMAGE = 100;
    @BindView(R.id.input_name) EditText inputName;
    @BindView(R.id.input_email) EditText inputEmail;
    @BindView(R.id.input_mobile) EditText inputMobile;
    @BindView(R.id.input_rc) EditText inputRC;
    @BindView(R.id.input_state) EditText inputState;
    @BindView(R.id.input_city) EditText inputCity;
    @BindView(R.id.input_pincode) EditText inputPincode;
    @BindView(R.id.input_password) EditText inputPassword;
    @BindView(R.id.input_cpassword) EditText inputcPassword;

    @BindView(R.id.fronttext) TextView frontText;
    @BindView(R.id.backtext) TextView backText;
    @BindView(R.id.insurancecopytest) TextView insurancecopyText;
    @BindView(R.id.loader) AVLoadingIndicatorView loader;
    private static String type = "";

    private ShowToast showToast;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2;
    private LocationAddressResultReceiver addressResultReceiver;
    private Location currentLocation;
    private LocationCallback locationCallback;
    private String fulladdress;
    private boolean first = true;
    private File insurancecopyfile,poidentificationfrontfile,poidentificationbackfile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        makeFullScreen();
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        showToast = new ShowToast(this);
        changeStatusBarColor(ContextCompat.getColor(this, R.color.colorAccent));
        hideToolbar();
        LocationManager locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
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
        @SuppressLint("HardwareIds")
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
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

        registerUser(name,email,mobile,state,city,pincode,rcnum,password,fulladdress,androidId);
        loader.setVisibility(View.VISIBLE);

    }
    @OnClick(R.id.insurance_copy)
    void OninsuranceCopy(){
        type = "1";
        showimagePicker();
    }
    @OnClick(R.id.proofof_identification_front)
    void OnProofOfIdentificationfront(){
        type = "2";
        showimagePicker();
    }
    @OnClick(R.id.proofof_identification_back)
    void OnProofOfIdentificationback(){
        type = "3";
        showimagePicker();
    }
    private void registerUser(String name, String email, String mobile, String state, String city, String pincode, String rcnum, String password, String fulladdress,String androidId) {

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
        if (item.getItemId() == android.R.id.home) {
            onCreateAccountClick();
        }
        return super.onOptionsItemSelected(item);
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
                Timber.d("Location null retrying");
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
    private void showimagePicker() {
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            showImagePickerOptions();
                        }

                        if (report.isAnyPermissionPermanentlyDenied()) {
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }
    private void showImagePickerOptions() {
        ImagePickerActivity.showImagePickerOptions(this, new ImagePickerActivity.PickerOptionListener() {
            @Override
            public void onTakeCameraSelected() {
                launchCameraIntent();
            }

            @Override
            public void onChooseGallerySelected() {
                launchGalleryIntent();
            }
        });
    }
    private void launchCameraIntent() {
        Intent intent = new Intent(this, ImagePickerActivity.class);
        intent.putExtra(ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION, ImagePickerActivity.REQUEST_IMAGE_CAPTURE);

        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true);
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1); // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1);

        // setting maximum bitmap width and height
        intent.putExtra(ImagePickerActivity.INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT, true);
        intent.putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_WIDTH, 1000);
        intent.putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_HEIGHT, 1000);
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    private void launchGalleryIntent() {
        Intent intent = new Intent(this, ImagePickerActivity.class);
        intent.putExtra(ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION, ImagePickerActivity.REQUEST_GALLERY_IMAGE);

        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true);
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1); // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1);
        startActivityForResult(intent, REQUEST_IMAGE);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = Objects.requireNonNull(data).getParcelableExtra("path");
                if(type.equals("1")){
                    insurancecopyfile = new File(uri.getPath());
                    String filename = getFileName(uri);
                    insurancecopyText.setVisibility(View.VISIBLE);
                    insurancecopyText.setText(filename);
                }
                if(type.equals("2")){
                    poidentificationfrontfile = new File(uri.getPath());
                    String filename = getFileName(uri);
                    frontText.setVisibility(View.VISIBLE);
                    frontText.setText(filename);
                }
                if(type.equals("3")){
                    poidentificationbackfile = new File(uri.getPath());
                    String filename = getFileName(uri);
                    backText.setVisibility(View.VISIBLE);
                    backText.setText(filename);
                }
                try {
                    // You can update this bitmap to your server
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(Objects.requireNonNull(this).getContentResolver(), uri);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(this));
        builder.setTitle(getString(R.string.dialog_permission_title));
        builder.setMessage(getString(R.string.dialog_permission_message));
        builder.setPositiveButton(getString(R.string.go_to_settings), (dialog, which) -> {
            dialog.cancel();
            openSettings();
        });
        builder.setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> dialog.cancel());
        builder.show();

    }

    // navigating user to app settings
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", Objects.requireNonNull(this).getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }
    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission not granted, " +
                                "restart the app if you want the feature",
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }
    }
}
