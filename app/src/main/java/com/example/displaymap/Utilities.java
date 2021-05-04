package com.example.displaymap;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo;

import static androidx.core.content.ContextCompat.getSystemService;

public final class Utilities {
    public static boolean isConnectingToInternet(Context context)
    {

        boolean have_WIFI= false;
        boolean have_MobileData = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] networkInfos = connectivityManager.getAllNetworkInfo();
        for(NetworkInfo info:networkInfos){
            if (info.getTypeName().equalsIgnoreCase("WIFI"))
                if (info.isConnected())
                    have_WIFI=true;
            if (info.getTypeName().equalsIgnoreCase("MOBILE DATA"))
                if (info.isConnected())
                    have_MobileData=true;
        }

        //System.out.println("***  have_WIFI=" + have_WIFI + " have mobile data=" + have_MobileData);
        return have_WIFI||have_MobileData;

    }
}
