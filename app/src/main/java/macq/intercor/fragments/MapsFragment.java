package macq.intercor.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

import macq.intercor.R;
import macq.intercor.helpers.Interval;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap map;
    private boolean locationPermissionGranted = false;
    private LatLng latlng;
    private Interval intervalLocUpdate;
    private FusedLocationProviderClient mFusedLocationClient;
    private static final String TAG = MapsFragment.class.getSimpleName();
    private static final float DEFAULT_ZOOM = 15;
    private float zoom = DEFAULT_ZOOM;

    /**********************
     ** OVERRIDE METHODS **
     **********************/

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.maps_fragment, container, false);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        if (view != null) {
            mapView = view.findViewById(R.id.map);
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }
        return view;
    }

    @Override
    public void onDestroy() {
        if(intervalLocUpdate != null) intervalLocUpdate.stop();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        updateLocationUI();
        listenForLocationUpdates();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /*******************
     ** OTHER METHODS **
     *******************/

    public LatLng getCoordinates() { return this.latlng; }

    public void setLocationPermissionGranted(Boolean value) { this.locationPermissionGranted = value; }

    public void updateLocationUI() {
        if (map == null) return;
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
                map.getUiSettings().setCompassEnabled(true);
                map.getUiSettings().setAllGesturesEnabled(true);
                map.getUiSettings().setZoomControlsEnabled(true);

                map.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));
            }
        } catch (SecurityException e) {
            Log.e("Security Exception: %s", e.getMessage());
        }
    }

    private void listenForLocationUpdates() {
        intervalLocUpdate = new Interval(this::updateLastKnownLocation);
        intervalLocUpdate.start();
    }

    private void updateLastKnownLocation() {
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(getActivity(), location -> {
                        if(location != null) {
                            LatLng newLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            setCoordinates(newLatLng);
                            zoom = map.getCameraPosition().zoom;
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, zoom));
                            Log.i(TAG, "Location Updated = "+location.getLatitude()+" -- "+location.getLongitude());
                        }
                    });
        } catch(SecurityException e) {
            Log.e(TAG, "Location Permission not granted", e);
        }
    }

    private void setCoordinates(LatLng latlng) { this.latlng = latlng; }
}
