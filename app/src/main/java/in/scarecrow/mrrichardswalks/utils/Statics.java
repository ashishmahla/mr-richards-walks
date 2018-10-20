package in.scarecrow.mrrichardswalks.utils;

import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class Statics {

    public static void peekSnackbar(AppCompatActivity activity, String msg) {
        Snackbar.make(activity.findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG)
                .setAction("CLOSE", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                    }
                })
                .setActionTextColor(activity.getResources().getColor(android.R.color.holo_red_light))
                .show();
    }

    public static void showSnackbar(AppCompatActivity activity, String msg) {
        Snackbar.make(activity.findViewById(android.R.id.content), msg, Snackbar.LENGTH_INDEFINITE)
                .setAction("CLOSE", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                    }
                })
                .setActionTextColor(activity.getResources().getColor(android.R.color.holo_red_light))
                .show();
    }
}