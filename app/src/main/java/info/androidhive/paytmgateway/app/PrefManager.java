package info.androidhive.paytmgateway.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;

public class PrefManager {
    private static PrefManager instance;
    private final SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    public PrefManager(Context context) {
        sharedPreferences = context.getApplicationContext().getSharedPreferences("paytmgateway", Context.MODE_PRIVATE);
    }

    public static PrefManager with(Context context) {
        if (instance == null) {
            instance = new PrefManager(context);
        }
        return instance;
    }

}
