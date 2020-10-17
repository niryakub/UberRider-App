package com.nirprojects.uberclonerider.ui.home;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.nirprojects.uberclonerider.Callback.IFirebaseDriverInfoListener;
import com.nirprojects.uberclonerider.Callback.IFirebaseFailedListener;
import com.nirprojects.uberclonerider.Common.Common;
import com.nirprojects.uberclonerider.Model.AnimationModel;
import com.nirprojects.uberclonerider.Model.DriverGeoModel;
import com.nirprojects.uberclonerider.Model.DriverInfoModel;
import com.nirprojects.uberclonerider.Model.EventBus.SelectPlaceEvent;
import com.nirprojects.uberclonerider.Model.GeoQueryModel;
import com.nirprojects.uberclonerider.R;
import com.nirprojects.uberclonerider.Remote.IGoogleAPI;
import com.nirprojects.uberclonerider.Remote.RetrofitClient;
import com.nirprojects.uberclonerider.RequestDriverActivity;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class HomeFragment extends Fragment implements OnMapReadyCallback, IFirebaseFailedListener, IFirebaseDriverInfoListener {
    private static final String TAG = "HomeFragment";

    @BindView(R.id.activity_main)
    SlidingUpPanelLayout slidingUpPanelLayout;
    @BindView(R.id.txt_welcome)
    TextView txt_welcome;

    private AutocompleteSupportFragment autocompleteSupportFragment;

    private HomeViewModel homeViewModel;

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;

    //Location
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    //Load Driver
    private double distance = 1.0; //default radius in km
    private static final double LIMIT_RANGE = 10.0; // in km..
    private Location previousLocation, currentLocation; //will be used to calculate distance..

    private boolean firstTime = true;

    //Listeners
    IFirebaseDriverInfoListener iFirebaseDriverInfoListener;
    IFirebaseFailedListener ifirebaseFailedListener;
    private String cityName;

    //
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private IGoogleAPI iGoogleAPI;


    @Override
    public void onStop() {
        Log.d(TAG, "onStop()");
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
    }


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        init();
        initViews(root);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this); //triggers onMapReady upon finishing.
        return root;
    }

    private void initViews(View root) {
        ButterKnife.bind(this, root); //BindView annotated fields and methods in the specified target using the source View as the view root

        Common.setWelcomeMessage(txt_welcome);
    }

    private void init() {
        Log.d(TAG, "onInit()");

        //initialize auto-complete search places..
        Places.initialize(getContext(), getString(R.string.google_maps_key));//initialize Places-service to be associated with our API-key..
        autocompleteSupportFragment = (AutocompleteSupportFragment) getChildFragmentManager()
                .findFragmentById(R.id.autocomplete_fragment);
        autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.ADDRESS, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteSupportFragment.setHint(getString(R.string.where_to));
        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                //Place is an object representing the place-location selected..
                //here we apply logic: upon selection, apply current-location-to-destination estimated route!
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(getView(),getString(R.string.permission_require),Snackbar.LENGTH_LONG).show();
                    return;
                }
                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {

                        LatLng origin = new LatLng(location.getLatitude(),location.getLongitude());
                        LatLng destination = new LatLng(place.getLatLng().latitude,place.getLatLng().longitude);

                        startActivity(new Intent(getContext(), RequestDriverActivity.class));
                        EventBus.getDefault().postSticky(new SelectPlaceEvent(origin,destination)); //Posts the given event to the event bus and holds on to the event (because it is sticky).
                    }
                });
            }

            @Override
            public void onError(@NonNull Status status) {
                Snackbar.make(getView(),""+status.getStatusMessage(),Snackbar.LENGTH_LONG).show();
            }
        });


        iGoogleAPI = RetrofitClient.getInstance().create(IGoogleAPI.class); //initialize Retrofit and establishes end-points..

        ifirebaseFailedListener = this;
        iFirebaseDriverInfoListener = this;

        //apply the locationRequest attributes for:fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        //this will determine the way we periodicly request Location from device.
        locationRequest = new LocationRequest();
        locationRequest.setSmallestDisplacement(10f); //Set the minimum displacement between location updates in METERS
        locationRequest.setInterval(5000); //meaning, every 5 seconds, we grab device's location...
        locationRequest.setFastestInterval(3000); // Explicitly set the fastest interval for location updates, in milliseconds.
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            //TODO: This LocationCallback is invoked by  fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            // according to locationReuqest attributes, thus in our case, it's invoked approximately every 5  seconds...
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                LatLng newPosition = new LatLng(locationResult.getLastLocation().getLatitude(),
                        locationResult.getLastLocation().getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition, 18f));

                //if user has changed location, calculate and load driver again..
                if (firstTime) {
                    previousLocation = currentLocation = locationResult.getLastLocation();
                    firstTime = false;

                    setRestrictPlacesInCountry(locationResult.getLastLocation());
                } else { //save last location, adjust current one..
                    previousLocation = currentLocation;
                    currentLocation = locationResult.getLastLocation();
                }

                if (previousLocation.distanceTo(currentLocation) / 1000 <= LIMIT_RANGE) //if not Over-ranged (.distanceTo() = Returns the approximate distance in meters between this location and the given location.)
                    loadAvailableDrivers();
                else {
                    //Do nothing..
                }
            }
        };

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != //required for the below statement..
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //Requests location updates with a callback on the specified Looper thread.
        //note: This call will keep the Google Play services connection active,
        // so need to make sure to call removeLocationUpdates(LocationCallback) when you no longer need it,
        //Callbacks for LocationCallback will be made on the specified thread,
        // which must already be a prepared looper thread.
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

        loadAvailableDrivers();
    }

    private void setRestrictPlacesInCountry(Location location) {
        try {
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            List<Address> addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if(addressList.size() > 0)
                autocompleteSupportFragment.setCountry(addressList.get(0).getCountryCode());

        } catch (IOException e){
            e.printStackTrace();
        }

    }

    private void loadAvailableDrivers() {

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(getView(), getString(R.string.permission_require), Snackbar.LENGTH_SHORT).show();
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        //At the moment, loads all the drivers within the same city as Rider.
                        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                        List<Address> addressList;
                        try{
                            addressList = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
                            if(addressList.size() > 0)
                                 cityName = addressList.get(0).getLocality(); // grab city name according to physical location..
                            if(! TextUtils.isEmpty(cityName)) {
                                //query+obtain ref..
                                DatabaseReference driver_location_ref = FirebaseDatabase.getInstance()
                                        .getReference(Common.DRIVERS_LOCATION_REFERENCES)
                                        .child(cityName); // Holds reference to the city the Rider is currently located within!


                                //A GeoFire object is used to read and write geo location data to your Firebase database and to create queries. To create a new GeoFire instance you need to attach it to a Firebase database reference.
                                GeoFire gf = new GeoFire(driver_location_ref);
                                //GeoFire allows you to query all keys within a geographic area using GeoQuery objects.
                                // As the locations for keys change, the query is updated in realtime and fires events letting you know if any relevant keys have moved.
                                // GeoQuery parameters can be updated later to change the size and center of the queried area.
                                GeoQuery geoQuery = gf.queryAtLocation(new GeoLocation(location.getLatitude(),
                                        location.getLongitude()), distance); //intialized to device's last location, and radius=distance..
                                geoQuery.removeAllListeners(); //we'll add customized..

                                geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                                    @Override
                                    //onKeyEntered() : Called if a key entered the search area of the GeoQuery.
                                    //This method is called for every key currently in the search area at the time of adding the listener.
                                    //This method is once per key, and is only called again if onKeyExited was called in the meantime.
                                    public void onKeyEntered(String key, GeoLocation location) {
                                        Common.driversFound.add(new DriverGeoModel(key, location)); // add it to the list of already found drivers within radius..
                                    }

                                    @Override
                                    public void onKeyExited(String key) {

                                    }

                                    @Override
                                    public void onKeyMoved(String key, GeoLocation location) {

                                    }

                                    @Override
                                    //Called once all initial GeoFire data has been loaded and the relevant events have been fired for this query.
                                    //Every time the query criteria is updated,
                                    //this observer will be called after the updated query has fired the appropriate key entered or key exited events.
                                    public void onGeoQueryReady() {
                                        if (distance <= LIMIT_RANGE) {
                                            distance++;
                                            loadAvailableDrivers(); //continue search within new adjusted distance..
                                        } else {
                                            distance = 1.0; //reset distance..
                                            addDriverMarker(); //apply all found drivers onto the Rider's ma
                                        }
                                    }

                                    @Override
                                    public void onGeoQueryError(DatabaseError error) {
                                        Snackbar.make(getView(), error.getMessage(), Snackbar.LENGTH_SHORT).show();
                                    }
                                });

                                //listen to new driver within city and range
                                driver_location_ref.addChildEventListener(new ChildEventListener() {
                                    @Override
                                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                                        //incase of new driver within range, this method is invoked!
                                        GeoQueryModel geoQueryModel = snapshot.getValue(GeoQueryModel.class);
                                        GeoLocation geoLocation = new GeoLocation(geoQueryModel.getL().get(0),
                                                geoQueryModel.getL().get(1)); //grab lang' and long' from db..
                                        DriverGeoModel driverGeoModel = new DriverGeoModel(snapshot.getKey(), geoLocation);
                                        Location newDriverLocation = new Location("");
                                        newDriverLocation.setLatitude(geoLocation.latitude);
                                        newDriverLocation.setLongitude(geoLocation.longitude);
                                        float newDistance = location.distanceTo(newDriverLocation) / 1000; //holds distance in km between rider and new driver
                                        if (newDistance <= LIMIT_RANGE) //incase driver is really within radius..
                                            findDriverByKey(driverGeoModel); //add driver to map..
                                    }

                                    @Override
                                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                                    }

                                    @Override
                                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                                    }

                                    @Override
                                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }
                            else
                            {
                                Snackbar.make(getView(),getString(R.string.city_name_empty),Snackbar.LENGTH_LONG).show();
                            }


                        } catch (IOException e) {
                            e.printStackTrace();
                            Snackbar.make(getView(),e.getMessage(),Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    private void addDriverMarker() { //applies all Drivers-in-rider's-range markers within the rider's map
        if(Common.driversFound.size()>0)//if found any drivers..
        {
            Observable.fromIterable(Common.driversFound)
                    .subscribeOn(Schedulers.newThread()) //Asynchronously subscribes Observers to this ObservableSource on the specified Scheduler.
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(driverGeoModel -> {
                        //On next
                        findDriverByKey(driverGeoModel);
                    },throwable -> {
                        Snackbar.make(getView(),throwable.getMessage(),Snackbar.LENGTH_SHORT).show();
                    },()->{

                    });

        }
        else
        {
            Snackbar.make(getView(),getString(R.string.drivers_not_found),Snackbar.LENGTH_SHORT).show();
        }
    }

    private void findDriverByKey(DriverGeoModel driverGeoModel) {
        FirebaseDatabase.getInstance()
                .getReference(Common.DRIVERS_INFO_REFERENCE)
                .child(driverGeoModel.getKey()) //grab a reference to DriverInfo according to it's userId..
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChildren()) //meaning, if exists...
                        {
                            driverGeoModel.setDriverInfoModel(dataSnapshot.getValue(DriverInfoModel.class));
                            iFirebaseDriverInfoListener.onDriverInfoLoadSuccess(driverGeoModel);
                        }
                        else
                            ifirebaseFailedListener.onFirebaseLoadFailed(getString(R.string.not_found_key)+driverGeoModel.getKey());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        ifirebaseFailedListener.onFirebaseLoadFailed(databaseError.getMessage());
                    }
                });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        //Request permission to open maps..
        Dexter.withContext(getContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        if (ActivityCompat.checkSelfPermission(getContext(),
                                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) !=
                                        PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true); //Enables or disables the my-location button
                        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                            @Override //applying logics to it...
                            public boolean onMyLocationButtonClick() {
                                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    return false;
                                }
                                fusedLocationProviderClient.getLastLocation()
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                                            @Override
                                            public void onSuccess(Location location) {
                                                LatLng userLatLng = new LatLng(location.getLatitude(),location.getLongitude());
                                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng,18f)); //Animates the movement of the camera from the current position to the position defined in the update.
                                            }
                                        });

                                return true;
                            }
                        });

                        //Layout button
                        View locationButton = ((View)mapFragment.getView().findViewById(Integer.parseInt("1")).getParent())
                                .findViewById(Integer.parseInt("2"));
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
                        //Right buttom
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP,0);
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
                        params.setMargins(0,0,0,250); // move view so it won't clash ui-ly with zoom-controls..
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Snackbar.make(getView(),permissionDeniedResponse.getPermissionName()+"need enable",Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                    }
                })
                .check();

        mMap.getUiSettings().setZoomControlsEnabled(true);
        //Apply customized-GoogleMap-Style
        try {
            boolean success = googleMap.setMapStyle((MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.uber_maps_style)));
            if (!success)
                Snackbar.make(getView(), "Load map style failed", Snackbar.LENGTH_SHORT).show();
        } catch(Exception e){
            Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
        }


    }

    @Override
    public void onFirebaseLoadFailed(String message) {
        Snackbar.make(getView(),message,Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDriverInfoLoadSuccess(DriverGeoModel driverGeoModel) {
        //if we already have a marker associated with this key, we dont set again..
        if(!Common.markerList.containsKey(driverGeoModel.getKey())){//if not..
            //add marker to markerList & apply that marker within our Map.
            Common.markerList.put(driverGeoModel.getKey(),
                    mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(driverGeoModel.getGeoLocation().latitude,
                            driverGeoModel.getGeoLocation().longitude))
                    .flat(true)
                    .title(Common.buildName(driverGeoModel.getDriverInfoModel().getFirstName(),
                            driverGeoModel.getDriverInfoModel().getLastName()))
                    .snippet(driverGeoModel.getDriverInfoModel().getPhoneNumber())//sets marker body..
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.car))));
        }

        if(!TextUtils.isEmpty(cityName)) //i
        {
            DatabaseReference driverLocation = FirebaseDatabase.getInstance()
                    .getReference(Common.DRIVERS_LOCATION_REFERENCES)
                    .child(cityName)
                    .child(driverGeoModel.getKey());

            driverLocation.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(!snapshot.hasChildren())
                    {
                        //This is the case where driver which is already within map, isn't within DB anymore (due to range/inactive)
                        //thus we also need to remove marker from map wherein within-db at DriverLocations/City/... he's already removed.
                        if(Common.markerList.get(driverGeoModel.getKey()) != null)
                            Common.markerList.get(driverGeoModel.getKey()).remove(); //remove marker...
                        Common.markerList.remove(driverGeoModel.getKey());//remove marker info from hash-map.
                        Common.driverLocationSubscribe.remove(driverGeoModel.getKey()); //remove driver information too..
                        driverLocation.removeEventListener(this);
                    }
                    else //incase still exists within DB.. (meaning in range & active)
                    {
                        if(Common.markerList.get(driverGeoModel.getKey()) != null)
                        {
                            GeoQueryModel geoQueryModel = snapshot.getValue(GeoQueryModel.class);//grab latest location-data.
                            AnimationModel animationModel = new AnimationModel(false,geoQueryModel);
                            if(Common.driverLocationSubscribe.get(driverGeoModel.getKey())!=null)
                            {
                                Marker currentMarker = Common.markerList.get(driverGeoModel.getKey());
                                AnimationModel oldPosition = Common.driverLocationSubscribe.get(driverGeoModel.getKey());

                                String from = new StringBuilder() //previous location
                                        .append(oldPosition.getGeoQueryModel().getL().get(0))
                                        .append(",")
                                        .append(oldPosition.getGeoQueryModel().getL().get(1))
                                        .toString();

                                String to = new StringBuilder() //current-latest location
                                        .append(animationModel.getGeoQueryModel().getL().get(0))
                                        .append(",")
                                        .append(animationModel.getGeoQueryModel().getL().get(1))
                                        .toString();

                                moveMarkerAnimation(driverGeoModel.getKey(),animationModel,currentMarker,from,to);
                            }
                            else
                            {
                                //First-time location storage within Common class.
                                Common.driverLocationSubscribe.put(driverGeoModel.getKey(),animationModel);
                            }
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Snackbar.make(getView(),error.getMessage(),Snackbar.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void moveMarkerAnimation(String key, AnimationModel animationModel, Marker currentMarker, String from, String to) {
        //moves the Driver's marker from previous-location to current-latest-location.
        if(!animationModel.isRun())
        {
            //Request API
            compositeDisposable.add(iGoogleAPI.getDirections("driving",
                    "less_driving",
                    from,to,
                    getString(R.string.google_api_key))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(returnResult -> {
                Log.d("API_RETURN",returnResult);

                try{
                    //Parse the retreived-json of the whole locations that build the route....
                    JSONObject jsonObject = new JSONObject(returnResult);
                    JSONArray jsonArray = jsonObject.getJSONArray("routes");
                    for(int i=0; i<jsonArray.length(); i++)
                    {
                        JSONObject route = jsonArray.getJSONObject(i);
                        JSONObject poly = route.getJSONObject("overview_polyline");
                        String polyline = poly.getString("points");
                        //polylineList = Common.decodePoly(polyline);
                        animationModel.setPolylineList(Common.decodePoly(polyline));
                    }

                    //Moving
                    //index = -1;
                    //next = 1;
                    animationModel.setIndex(-1);
                    animationModel.setNext(1);

                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            if(animationModel.getPolylineList() != null && animationModel.getPolylineList().size() > 1)
                            {
                                if(animationModel.getIndex() < animationModel.getPolylineList().size() -2 ){
                                    //index++;
                                    animationModel.setIndex(animationModel.getIndex()+1);
                                    //next = index+1;
                                    animationModel.setNext(animationModel.getIndex()+1);
                                    //start = polylineList.get(index);
                                    animationModel.setStart(animationModel.getPolylineList().get(animationModel.getNext()));
                                    //end=polylineList.get(next);
                                    animationModel.setEnd(animationModel.getPolylineList().get(animationModel.getNext()));
                                }

                                ValueAnimator valueAnimator = ValueAnimator.ofInt(0,1);
                                valueAnimator.setDuration(3000); //Sets the length of the animation.
                                valueAnimator.setInterpolator(new LinearInterpolator()); //run in linear-motion!
                                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    //Adds a listener to the set of listeners that are sent update events through the life of an animation.
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator value) {
                                        //v = value.getAnimatedFraction(); //Returns the current animation fraction, which is the elapsed/interpolated fraction used in the most recent frame update on the animation.
                                        animationModel.setV(value.getAnimatedFraction());
                                        //lat = v*end.latitude + (1-v)*start.latitude;
                                        animationModel.setLat(animationModel.getV()*animationModel.getEnd().latitude +
                                                (1-animationModel.getV())
                                                        *animationModel.getStart().latitude);
                                        //lng = v*end.longitude + (1-v)*start.longitude;
                                        animationModel.setLng(animationModel.getV()*animationModel.getEnd().longitude +
                                                (1-animationModel.getV())
                                                        *animationModel.getStart().longitude);

                                        LatLng newPos = new LatLng(animationModel.getLat(),animationModel.getLng());
                                        currentMarker.setPosition(newPos);
                                        currentMarker.setAnchor(0.5f,0.5f);
                                        currentMarker.setRotation(Common.getBearing(animationModel.getStart(),newPos)); //rotates the marker's icon

                                    }
                                });

                                valueAnimator.start(); //Starts this animation.
                                if(animationModel.getIndex() < animationModel.getPolylineList().size()-2) //if reached destination
                                    animationModel.getHandler().postDelayed(this,1500); //run once again.
                                else if(animationModel.getIndex() < animationModel.getPolylineList().size()-1) // Done..
                                {
                                    animationModel.setRun(false);
                                    Common.driverLocationSubscribe.put(key,animationModel); // update data..
                                }
                            }
                        }
                    };

                    //Run the handler!
                    animationModel.getHandler().postDelayed(runnable,1500); //Causes the Runnable r to be added to the message queue, to be run after the specified amount of time elapses

                } catch(Exception e){
                    Snackbar.make(getView(),e.getMessage(),Snackbar.LENGTH_SHORT).show();
                }
            })
            );
        }
    }
}