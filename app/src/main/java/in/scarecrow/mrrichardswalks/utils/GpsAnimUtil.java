package in.scarecrow.mrrichardswalks.utils;

import android.graphics.Color;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import in.scarecrow.mrrichardswalks.R;

public class GpsAnimUtil {

    public static final int GPS_STATUS_OFFLINE = -21;
    public static final int GPS_STATUS_SEARCHING = -19;
    public static final int GPS_STATUS_LOCATED = -17;

    public static void setGpsStatus(ImageView gpsView, int status) {
        switch (status) {
            case GPS_STATUS_LOCATED:
                gpsView.clearAnimation();
                gpsView.setAlpha(1.0f);
                gpsView.setColorFilter(Color.argb(255, 66, 133, 244));
                gpsView.setImageDrawable(gpsView.getContext().getResources().getDrawable(R.drawable.ic_gps_location));
                break;

            case GPS_STATUS_SEARCHING:
                gpsView.clearAnimation();
                gpsView.setColorFilter(Color.argb(255, 66, 133, 244));
                gpsView.setImageDrawable(gpsView.getContext().getResources().getDrawable(R.drawable.ic_gps_location));

                Animation anim = new AlphaAnimation(0.0f, 1.0f);
                anim.setDuration(500);
                anim.setStartOffset(200);
                anim.setRepeatMode(Animation.REVERSE);
                anim.setRepeatCount(Animation.INFINITE);
                gpsView.startAnimation(anim);
                break;

            case GPS_STATUS_OFFLINE:
                gpsView.clearAnimation();
                gpsView.setAlpha(1.0f);
                gpsView.setColorFilter(Color.BLACK);
                gpsView.setImageDrawable(gpsView.getContext().getResources().getDrawable(R.drawable.ic_gps_off));
                break;
        }
    }
}