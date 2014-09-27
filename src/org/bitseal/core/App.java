package org.bitseal.core;

import org.bitseal.crypt.PRNGFixes;

import android.app.Application;
import android.content.Context;

public class App extends Application 
{
    /**
     * Keeps a reference of the application context
     */
    private static Context sContext;

    @Override
    public void onCreate() 
    {
        super.onCreate();
        sContext = getApplicationContext();
        
        PRNGFixes.apply();
    }

    /**
     * Returns the application context. <br><br>
     * 
     * <b>NOTE!!!</b> There is no guarantee that the normal, non-static onCreate() will have been called before
     * this method is called. This means that this method can sometimes return null, particularly if called when the 
     * app has been running for a short time, e.g. during unit testing. 
     *
     * @return application context
     */
    public static Context getContext() 
    {
        return sContext;
    }
}