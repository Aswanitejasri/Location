package sample.cdac.com.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.TelephonyManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sample.cdac.com.location.Server.CellData;
import sample.cdac.com.location.Server.RetrofitApi;
import sample.cdac.com.location.Server.RetrofitClient;
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;

    private TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultTextView = findViewById(R.id.resultTextView);

        if (checkPermissions()) {
            retrieveTelephonyInfo();
            sendDataToServer();
        } else {
            requestPermissions();
        }
    }



    private boolean checkPermissions() {
        int resultFineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int resultCoarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int resultPhoneState = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
        return resultFineLocation == PackageManager.PERMISSION_GRANTED &&
                resultCoarseLocation == PackageManager.PERMISSION_GRANTED &&
                resultPhoneState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE},
                PERMISSION_REQUEST_CODE
        );
    }

    private void retrieveTelephonyInfo() {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

            StringBuilder resultBuilder = new StringBuilder();

            // Get the signal strength
            int signalStrength = getSignalStrength(telephonyManager);
            resultBuilder.append("Signal Strength: ").append(signalStrength).append(" dBm\n");

            // Get the network type
            String networkType = getNetworkType(telephonyManager);
            resultBuilder.append("Network Type: ").append(networkType).append("\n");

            // Get the cell ID and TAC
            String cellId = getCellId(telephonyManager);
            resultBuilder.append("Cell ID: ").append(cellId).append("\n");

            String tac = getTac(telephonyManager);
            resultBuilder.append("TAC: ").append(tac).append("\n");

            // Get the MCC and MNC
            String mcc = getMcc(telephonyManager);
            String mnc = getMnc(telephonyManager);
            resultBuilder.append("MCC: ").append(mcc).append("\n");
            resultBuilder.append("MNC: ").append(mnc).append("\n");

            // Get the network operator name
            String networkOperatorName = getNetworkOperatorName(telephonyManager);
            resultBuilder.append("Operator Name: ").append(networkOperatorName).append("\n");

            // Get the GPS location and reverse geocode
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = null;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            // Append GPS location information if available
            if (location != null) {
                resultBuilder.append("Latitude: ").append(location.getLatitude()).append("\n");
                resultBuilder.append("Longitude: ").append(location.getLongitude()).append("\n");

                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (!addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        String readableAddress = address.getAddressLine(0);
                        resultBuilder.append("Readable Address: ").append(readableAddress).append("\n");
                    } else {
                        resultBuilder.append("Readable Address: N/A\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Set the current date and time
            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            SimpleDateFormat timeFormatter = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
            String currentDate = dateFormatter.format(new Date());
            String currentTime = timeFormatter.format(new Date());
            resultBuilder.append("Date: ").append(currentDate).append("\n");
            resultBuilder.append("Time: ").append(currentTime).append("\n");

            // Get the device name from Settings.Global
            String deviceName = Settings.Global.getString(getContentResolver(), Settings.Global.DEVICE_NAME);
            resultBuilder.append("Device Name: ").append(deviceName).append("\n");

            // Get the Android ID
            String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            resultBuilder.append("Android ID: ").append(androidId).append("\n");

            resultTextView.setText(resultBuilder.toString());

        } catch (SecurityException e) {
            e.printStackTrace();
            // Handle the SecurityException here, such as showing an error message
        }
    }

    private int getSignalStrength(TelephonyManager telephonyManager) {
        if (telephonyManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
                if (cellInfoList != null) {
                    for (CellInfo cellInfo : cellInfoList) {
                        if (cellInfo instanceof CellInfoGsm) {
                            return ((CellInfoGsm) cellInfo).getCellSignalStrength().getDbm();
                        } else if (cellInfo instanceof CellInfoLte) {
                            return ((CellInfoLte) cellInfo).getCellSignalStrength().getDbm();
                        }
                    }
                }
            }
        }
        return 0; // Default value when signal strength is not available
    }

    private String getNetworkType(TelephonyManager telephonyManager) {
        if (telephonyManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                return "Permission not granted";
            }
            int networkType = telephonyManager.getVoiceNetworkType();
            switch (networkType) {
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return "LTE";
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    return "GPRS";
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    return "EDGE";
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    return "UMTS";
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                    return "HSDPA";
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                    return "HSUPA";
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    return "HSPA";
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    return "CDMA";
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    return "EVDO_0";
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    return "EVDO_A";
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                    return "EVDO_B";
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    return "1xRTT";
                case TelephonyManager.NETWORK_TYPE_IWLAN:
                    return "IWLAN";
                case TelephonyManager.NETWORK_TYPE_NR:
                    return "5G NR";
                default:
                    return "Unknown";
            }
        }
        return "N/A";
    }

    private String getCellId(TelephonyManager telephonyManager) {
        if (telephonyManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
                if (cellInfoList != null) {
                    for (CellInfo cellInfo : cellInfoList) {
                        if (cellInfo instanceof CellInfoGsm) {
                            CellInfoGsm cellInfoGsm = (CellInfoGsm) cellInfo;
                            return String.valueOf(cellInfoGsm.getCellIdentity().getCid());
                        } else if (cellInfo instanceof CellInfoLte) {
                            CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
                            return String.valueOf(cellInfoLte.getCellIdentity().getCi());
                        }
                    }
                }
            }
        }
        return "N/A"; // Return a default value when cell ID or TAC is empty or not available
    }

    private String getTac(TelephonyManager telephonyManager) {
        if (telephonyManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
                if (cellInfoList != null) {
                    for (CellInfo cellInfo : cellInfoList) {
                        if (cellInfo instanceof CellInfoGsm) {
                            CellInfoGsm cellInfoGsm = (CellInfoGsm) cellInfo;
                            return String.valueOf(cellInfoGsm.getCellIdentity().getLac());
                        } else if (cellInfo instanceof CellInfoLte) {
                            CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
                            return String.valueOf(cellInfoLte.getCellIdentity().getTac());
                        }
                    }
                }
            }
        }
        return "N/A"; // Return a default value when cell ID or TAC is empty or not available
    }

    private String getMcc(TelephonyManager telephonyManager) {
        if (telephonyManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
                if (cellInfoList != null) {
                    for (CellInfo cellInfo : cellInfoList) {
                        if (cellInfo instanceof CellInfoGsm) {
                            return String.valueOf(((CellInfoGsm) cellInfo).getCellIdentity().getMcc());
                        } else if (cellInfo instanceof CellInfoLte) {
                            return String.valueOf(((CellInfoLte) cellInfo).getCellIdentity().getMcc());
                        }
                    }
                }
            }
        }
        return "N/A"; // Return a default value when MCC is empty or not available
    }

    private String getMnc(TelephonyManager telephonyManager) {
        if (telephonyManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
                if (cellInfoList != null) {
                    for (CellInfo cellInfo : cellInfoList) {
                        if (cellInfo instanceof CellInfoGsm) {
                            return String.valueOf(((CellInfoGsm) cellInfo).getCellIdentity().getMnc());
                        } else if (cellInfo instanceof CellInfoLte) {
                            return String.valueOf(((CellInfoLte) cellInfo).getCellIdentity().getMnc());
                        }
                    }
                }
            }
        }
        return "N/A"; // Return a default value when MNC is empty or not available
    }


    private String getNetworkOperatorName(TelephonyManager telephonyManager) {
        if (telephonyManager != null) {
            return telephonyManager.getNetworkOperatorName();
        }
        return "N/A";
    }

    // Handle permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                retrieveTelephonyInfo();
            } else {
                // Permission denied by the user
                // Handle accordingly, e.g., show a message or disable functionality
            }
        }
    }
    private void sendDataToServer() {
        RetrofitApi retrofitApi = RetrofitClient.getClient().create(RetrofitApi.class);

        CellData cellData = new CellData();
        // Set properties of the CellData object

        Call<CellData> call = retrofitApi.saveCellData(cellData);
        call.enqueue(new Callback<CellData>() {
            @Override
            public void onResponse(Call<CellData> call, Response<CellData> response) {
                if (response.isSuccessful()) {
                    CellData savedCellData = response.body();
                    // Handle the response as needed
                } else {
                    // Handle unsuccessful response
                }
            }

            @Override
            public void onFailure(Call<CellData> call, Throwable t) {
                // Handle failure
            }
        });
    }
}
