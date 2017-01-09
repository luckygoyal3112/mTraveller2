package com.example.luckygoyal.mtraveller2;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.example.luckygoyal.mtraveller2.DBHelper.LocationDBHelper;
import com.example.luckygoyal.mtraveller2.DBHelper.PlaceDBHelper;
import com.example.luckygoyal.mtraveller2.GCMClientApp.QuickstartPreferences;
import com.example.luckygoyal.mtraveller2.timelineTab.sendNotificationNewLoc;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Lucky Goyal on 10/16/2015.
 */
public  class CurrentPlace implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
        private GoogleApiClient mGoogleApiClient;
        public static final String TAG = CurrentPlace.class.getSimpleName();

    String currentPlace = "";
    String currentPlaceAdre = "";
    String currentPlaceF = "";

    static Location mLastLocation;
    private Context context;

    public CurrentPlace(Location currentBestLocation, Context context) {
        this.context=context;
    }

    public CurrentPlace() {

    }
  public String getPlace(Location location, Context context) {
             mLastLocation = location;
                Log.d(TAG, "Location: " + location.toString());
                    if (mGoogleApiClient == null) {
                        mGoogleApiClient = new GoogleApiClient.Builder(context)
                                .addConnectionCallbacks(this)
                                .addOnConnectionFailedListener(this)
                                .addApi(Places.GEO_DATA_API)
                                .addApi(Places.PLACE_DETECTION_API)
                                .build();
                        mGoogleApiClient.connect();
                    }
                    Log.d(TAG, "Google Api Client connecting.. .");
                    return currentPlace;
            }
        @Override
        public void onConnected(Bundle bundle) {
            Log.d(TAG, "Google Api Client connected.");

            if (mGoogleApiClient != null) {
                Log.d(TAG, "Getting nearby places...");

                PendingResult<PlaceLikelihoodBuffer> result =
                        Places.PlaceDetectionApi.getCurrentPlace(mGoogleApiClient, null);

                result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                    @Override
                    public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                        Log.d(TAG, "Got results: " + likelyPlaces.getCount() + " place found.");

                        for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                            Log.i(TAG, String.format("Place 'b%s' has likelihood: %g",
                                    placeLikelihood.getPlace().getName(),
                                    placeLikelihood.getLikelihood()));
                        }
                        PlaceLikelihood placeLikelihood = likelyPlaces.get(0);
                        if (placeLikelihood != null && placeLikelihood.getPlace() != null && !TextUtils.isEmpty(placeLikelihood.getPlace().getName()))
                            currentPlace = (String) placeLikelihood.getPlace().getName();
                        currentPlaceAdre = (String) placeLikelihood.getPlace().getAddress();
                        int prox = (int) (placeLikelihood.getLikelihood() * 100);
                        String currentPlaceId = (placeLikelihood.getPlace().getId());
                        String dateSelected = getTodayDate();
                        //storeplace(currentPlaceId, );
                        Log.d(TAG, "Most likely place: " + currentPlace);
                        Log.d(TAG, "Address: " + currentPlaceAdre);
                        Log.d(TAG, "Id: " + currentPlaceId);
                        if (placeLikelihood != null)
                            Log.d(TAG, "Percent change of being there: " + (int) (placeLikelihood.getLikelihood() * 100));

                        if (prox >= 25 ) {
                            /*currentPlaceF = String.format("%s: %s ", currentPlace, currentPlaceAdre);
                                   String.format("%s %s Prx:%d Lat %s Lng %s", currentPlace, currentPlaceAdre);
                                    , prox, mLastLocation.getLatitude(),
                                    mLastLocation.getLongitude());*/
                            storeplace(currentPlaceId, mLastLocation.getLatitude(),
                                    mLastLocation.getLongitude(),
                                    dateSelected, currentPlace,currentPlaceAdre );
                        }else {
                            Log.d(TAG, "No current place data availiable " + prox);
                            currentPlaceF = String.format("No Proximate place");
                            currentPlaceF = null;
                        }
                        likelyPlaces.release();
                        Log.d(TAG, "Likely places released");
                        updateloc(currentPlace, currentPlaceAdre, context, mLastLocation.getLatitude(), mLastLocation.getLongitude(), currentPlaceId);
                    }
                });
            }
             //return currentPlace;
        }
    private void storeplace(String currentPlaceId,double lat, double lng, String sysdate,  String strAdrName, String strAdrFull) {
        Log.d(TAG, "PLace id is" + currentPlaceId);
        PlaceDBHelper placeDb = new PlaceDBHelper(context);
        long rowId = placeDb.insertPlace( currentPlaceId, lat, lng,sysdate,strAdrName,strAdrFull );
        Log.d(TAG, "Place Id Stored "+  currentPlaceId);
    }
    private void updateloc(String strAdrP, String strAdrfull, Context context, double lat, double lng, String currentPlaceId) {
        if (strAdrP == null){
           // strAdrP = String.format("No Proximate place");
            Log.d(TAG, "No Place found. Taking location data");
            strAdrP = getAddress(context, lat, lng);
        }
        //String strAdrA = getAddress(context, lat, lng);
        //String strAdr = strAdrP + " " + strAdrA;

        String dateSelected = getTodayDate();
        String uts = getTodayTime();
        Log.d(TAG, "Place Name is" + strAdrP);
        Log.d(TAG, "PLace Address is" + strAdrfull);
        Log.d(TAG, "Date is " + dateSelected);
        Log.d(TAG, "Time is " + uts);
        if (strAdrP != null) {

            LocationDBHelper locDb = new LocationDBHelper(this.context);
            //SQLiteDatabase newDB = locDb.getWritableDatabase();

            // Attach The Data From DataBase Into ListView Using Cursor Adapter
            //Cursor locationCursor = locDb.getAllLocations();
            long rowId = locDb.insertLocation(lat, lng, uts, dateSelected, strAdrP, strAdrfull,currentPlaceId);
            Log.d(TAG, "rowId is "+ rowId);
            if (rowId>0){
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                String contactName = sharedPreferences.getString(QuickstartPreferences.CONTACT_NAME, null);
                String MobilePhone = sharedPreferences.getString(QuickstartPreferences.MOBILE_PHONE, null);
                Log.d(TAG, "About to send GCM broadcast for the new location");
                sendNotificationNewLoc sendLoc = new sendNotificationNewLoc(context);
                sendLoc.handleNewloc(context, contactName, MobilePhone, dateSelected, uts, strAdrP, strAdrfull, lat, lng);
            }
        } else {
            Log.d(TAG, "Address not found via Geocoding");
        }

        //long rowdId = insertLocation(lat, lng, String uts, String strAdr, String dateSelected)
    }

    @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "Connection suspended.");

        }
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {

        }
    public String getAddress(Context context, double lat, double lng) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        //, Locale);.getDefault());
        StringBuilder strReturnedAddress = null;
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                strReturnedAddress = new StringBuilder();
                //Log.d(TAG, "feature name is" + returnedAddress.formatted_address());
                Log.d(TAG, "feature name is" + returnedAddress.getThoroughfare());
                Log.d(TAG, "feature name is" + returnedAddress.getSubThoroughfare());
                //strReturnedAddress.append(returnedAddress.getFeatureName());
                for (int i = 0; i < returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
                }
                strReturnedAddress.append(returnedAddress.getCountryName());
                /*strReturnedAddress.append("Secnd set");
                strReturnedAddress.append(returnedAddress.getLocality()).append(",");
                strReturnedAddress.append(returnedAddress.getAdminArea()).append(",");
                strReturnedAddress.append(returnedAddress.getCountryName()).append(",");
                //strReturnedAddress.append(returnedAddress.getCountryCode());*/
                return strReturnedAddress.toString();
            } else {
                Log.d(TAG, "No Address returned!");
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.d(TAG, "Canont get Address!");
        }

        return null;
    }
    private String getTodayDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        String selectedDate = dateFormat.format(new Date()); // Find todays date
        return selectedDate;
    }
    private String getTodayTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        String uts = dateFormat.format(new Date()); // Find todays date
        return uts;
    }
}
