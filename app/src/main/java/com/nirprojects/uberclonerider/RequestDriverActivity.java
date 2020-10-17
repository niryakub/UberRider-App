package com.nirprojects.uberclonerider;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.util.EventLog;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.maps.android.ui.IconGenerator;
import com.nirprojects.uberclonerider.Common.Common;
import com.nirprojects.uberclonerider.Model.EventBus.SelectPlaceEvent;
import com.nirprojects.uberclonerider.Remote.IGoogleAPI;
import com.nirprojects.uberclonerider.Remote.RetrofitClient;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class RequestDriverActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "RequestDriverActivity" ;
    private GoogleMap mMap;

    private SelectPlaceEvent selectPlaceEvent;

    //Routes
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private IGoogleAPI iGoogleAPI;
    private Polyline blackPolyline,greyPolyline;
    private PolylineOptions polylineOptions,blackPolylineOptions;
    private List<LatLng> polylineList = new ArrayList<>();

    private Marker originMarker,destinationMarker;

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this); //Registers the given subscriber to receive events.
        //subscribers have event handling methods that must be annotated by Subscribe.
        //The Subscribe annotation also allows configuration like ThreadMode and priority.
    }

    @Override
    protected void onStop() {
        compositeDisposable.clear();
        super.onStop();
        if(EventBus.getDefault().hasSubscriberForEvent(SelectPlaceEvent.class))
            EventBus.getDefault().removeStickyEvent(SelectPlaceEvent.class);
        EventBus.getDefault().unregister(this);
    }

    //sticky: If true, delivers the most recent sticky event (posted with EventBus.postSticky(Object)) to this subscriber (if event available).
   //threadMode : Each subscriber method has a thread mode, which determines in which thread the method is to be called by EventBus.
    // ..EventBus takes care of threading independently from the posting thread
    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public void onSelectPlaceEvent(SelectPlaceEvent event)
    {
        selectPlaceEvent = event;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_driver);

        init();



        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void init() {
        iGoogleAPI = RetrofitClient.getInstance().create(IGoogleAPI.class);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG,"onMapReady()");
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true); //Enables or disables the my-location button
        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override //applying logics to it...
            public boolean onMyLocationButtonClick() {

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectPlaceEvent.getOrigin(),18f));

                return true;
            }
        });

        drawPath(selectPlaceEvent);
        //Layout button
        View locationButton = ((View)findViewById(Integer.parseInt("1")).getParent())
                .findViewById(Integer.parseInt("2"));
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        //Right buttom
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP,0);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
        params.setMargins(0,0,0,250); // move view so it won't clash ui-ly with zoom-controls..

        mMap.getUiSettings().setZoomControlsEnabled(true);
        //Apply customized-GoogleMap-Style
        try {
            boolean success = googleMap.setMapStyle((MapStyleOptions.loadRawResourceStyle(this, R.raw.uber_maps_style)));
            if (!success)
                Toast.makeText(this, "Load map style failed", Toast.LENGTH_SHORT).show();
        } catch(Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void drawPath(SelectPlaceEvent selectPlaceEvent) {
        Log.d(TAG,"drawPath()");
        //Request API
        compositeDisposable.add(iGoogleAPI.getDirections("driving",
                "less_driving",
                selectPlaceEvent.getOriginString(),selectPlaceEvent.getDestinationString(),
                getString(R.string.google_api_key))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(returnResult -> {
                    Log.d("API_RETURN",returnResult);

                    try{
                        Log.d(TAG,"drawPath()->try..");
                        //Parse the retreived-json of the whole locations that build the route....
                        JSONObject jsonObject = new JSONObject(returnResult);
                        JSONArray jsonArray = jsonObject.getJSONArray("routes");
                        for(int i=0; i<jsonArray.length(); i++)
                        {
                            JSONObject route = jsonArray.getJSONObject(i);
                            JSONObject poly = route.getJSONObject("overview_polyline");
                            String polyline = poly.getString("points");
                            polylineList = Common.decodePoly(polyline);
                        }
                        //set the polyline attributes (the route's line..)
                        polylineOptions = new PolylineOptions();
                        polylineOptions.color(Color.GRAY);
                        polylineOptions.width(12);
                        polylineOptions.startCap(new SquareCap()); //set starting vertice-cap
                        polylineOptions.jointType(JointType.ROUND); //Sets the joint type for all vertices of the polyline except the start and end vertices.
                        polylineOptions.addAll(polylineList); // apply all the dots creating the line..
                        greyPolyline = mMap.addPolyline(polylineOptions); //applies the polyline to map.

                        blackPolylineOptions = new PolylineOptions();
                        blackPolylineOptions.color(Color.BLACK);
                        blackPolylineOptions.width(5);
                        blackPolylineOptions.startCap(new SquareCap());
                        blackPolylineOptions.jointType(JointType.ROUND);
                        blackPolylineOptions.addAll(polylineList);
                        blackPolyline = mMap.addPolyline(blackPolylineOptions);

                        //Animator
                        ValueAnimator valueAnimator = ValueAnimator.ofInt(0,100);
                        valueAnimator.setDuration(1100); //Sets the length of the animation.
                        valueAnimator.setRepeatCount(ValueAnimator.INFINITE);//sets how many times the animation should be repeated.
                        valueAnimator.setInterpolator(new LinearInterpolator()); //run in linear-motion!
                        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            //Adds a listener to the set of listeners that are sent update events through the life of an animation.
                            @Override
                            public void onAnimationUpdate(ValueAnimator value) {
                                List<LatLng> points = greyPolyline.getPoints();
                                int percentValue = (int)value.getAnimatedValue();//calculate the percent of the animation that is accomplished to this point..
                                int size = points.size();
                                int newPoints = (int)(size*(percentValue/100.0f));
                                List<LatLng> p =points.subList(0,newPoints);
                                blackPolyline.setPoints(p);
                            }
                        });

                        valueAnimator.start(); //Starts this animation.

                        LatLngBounds latLngBounds = new LatLngBounds.Builder()
                                .include(selectPlaceEvent.getOrigin())
                                .include(selectPlaceEvent.getDestination())
                                .build();

                        //Add car icon for origin..
                        JSONObject object = jsonArray.getJSONObject(0);
                        JSONArray legs = object.getJSONArray("legs");
                        JSONObject legObjects = legs.getJSONObject(0);

                        JSONObject time = legObjects.getJSONObject("duration");
                        String duration = time.getString("text");

                        String start_address = legObjects.getString("start_address");
                        String end_address = legObjects.getString("end_address");

                        addOriginMarker(duration,start_address);

                        addDestinationMarker(end_address);

                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds,160));
                        mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.getCameraPosition().zoom-1));




                    } catch(Exception e){
                        //Snackbar.make(getView(),e.getMessage(),Snackbar.LENGTH_SHORT).show();
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
        );
    }

    private void addDestinationMarker(String end_address) {
        Log.d(TAG,"addDestinationMarker()");
        //Apply customized marker to Destination-Marker
        View view = getLayoutInflater().inflate(R.layout.destination_info_windows,null);

        TextView txt_destination= (TextView)view.findViewById(R.id.txt_destination);
        txt_destination.setText(Common.formatAddress(end_address));

        //Create Icon for marker
        IconGenerator generator = new IconGenerator(this);
        generator.setContentView(view);
        generator.setBackground(new ColorDrawable(Color.TRANSPARENT));
        Bitmap icon = generator.makeIcon();

        destinationMarker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(icon))
                .position(selectPlaceEvent.getDestination()));

    }

    private void addOriginMarker(String duration, String start_address) {
        Log.d(TAG,"addOriginMarker");
        //Apply customized marker to Origin-Marker
        View view = getLayoutInflater().inflate(R.layout.origin_info_windows,null);

        TextView txt_time = (TextView)view.findViewById(R.id.txt_time);
        TextView txt_origin = (TextView)view.findViewById(R.id.txt_origin);

        txt_time.setText(Common.formatDuration(duration));
        txt_origin.setText(Common.formatAddress(start_address));

        //Create Icon for marker
        IconGenerator generator = new IconGenerator(this);
        generator.setContentView(view);
        generator.setBackground(new ColorDrawable(Color.TRANSPARENT));
        Bitmap icon = generator.makeIcon();

        originMarker = mMap.addMarker(new MarkerOptions()
        .icon(BitmapDescriptorFactory.fromBitmap(icon))
        .position(selectPlaceEvent.getOrigin()));

    }
}