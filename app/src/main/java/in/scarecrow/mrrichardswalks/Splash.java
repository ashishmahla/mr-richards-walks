package in.scarecrow.mrrichardswalks;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import in.scarecrow.mrrichardswalks.utils.Statics;

public class Splash extends AppCompatActivity {

    public static final int ERROR_DIALOG_REQUEST = 3476;
    private static final String TAG = "Splash";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (isServiceOk()) {
            launchMainActivity();
        } else {
            findViewById(R.id.pb_splash).setVisibility(View.GONE);
        }
    }

    private void launchMainActivity() {
        Handler handler = new Handler();
        Runnable r = new Runnable() {
            public void run() {
                startActivity(new Intent(Splash.this, MainActivity.class));
                Splash.this.finish();
            }
        };
        handler.postDelayed(r, 1000);
    }

    public boolean isServiceOk() {
        int apiAvail = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(Splash.this);

        if (apiAvail == ConnectionResult.SUCCESS) {
            // Google Play Services is OK
            Log.d(TAG, "isServiceOk: Google Play Services is working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(apiAvail)) {
            // some error occurred but it is resolvable
            Log.d(TAG, "isServiceOk: An error occurred but it is resolvable");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(Splash.this, apiAvail, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Log.d(TAG, "isServiceOk: Unresolvable error occurred.");
            Statics.showSnackbar(Splash.this, "Error loading Maps SDK");
        }

        return false;
    }
}
