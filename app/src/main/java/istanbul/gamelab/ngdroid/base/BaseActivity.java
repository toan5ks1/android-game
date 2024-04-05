package istanbul.gamelab.ngdroid.base;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import istanbul.gamelab.ngdroid.core.AppManager;
import istanbul.gamelab.ngdroid.util.Log;
import com.toan5ks1.floppybee.R;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.os.Handler;
import android.widget.Toast;

import com.amazon.device.drm.LicensingService;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.RequestId;
import com.toan5ks1.iap.MySku;
import com.toan5ks1.iap.SampleIapManager;
import com.toan5ks1.iap.SamplePurchasingListener;
import com.toan5ks1.iap.SubscriptionRecord;

public class BaseActivity extends Activity {

    private static final String TAG = BaseActivity.class.getSimpleName();

    private RelativeLayout gamesurface;
    protected AppManager appmanager;
    private SampleIapManager sampleIapManager;
    private boolean isdevelopmentmode, isfreeversion, isgprelease;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupApplicationSpecificOnCreate();
        setupIAPOnCreate();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        makeFullScreen();
        appmanager = new AppManager(this);

        setContentView(R.layout.activity_game);
        gamesurface = (RelativeLayout)findViewById(R.id.gameSurface);
        gamesurface.addView(appmanager);

        isdevelopmentmode = false;
        isfreeversion = true;
        isgprelease = true;
    }

    public void makeFullScreen() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if(Build.VERSION.SDK_INT < 19) { //19 or above api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else {
            //for lower api versions
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        appmanager.onPause();
        sampleIapManager.deactivate();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        makeFullScreen();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        makeFullScreen();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean changed) {
        super.onWindowFocusChanged(changed);
        makeFullScreen();
    }

    protected void setDevelopmentMode(boolean isTestMode) {
        isdevelopmentmode = isTestMode;
        if (isdevelopmentmode) Log.setLogLevel(Log.LOGLEVEL_VERBOSE);
        else Log.setLogLevel(Log.LOGLEVEL_SILENT);
    }

    public boolean isDevelopmentMode() {
        return isdevelopmentmode;
    }

    protected void setFreeVersion(boolean isFullVersion) {
        isfreeversion = isFullVersion;
    }

    public boolean isFreeVersion() {
        return isfreeversion;
    }

    protected void setGPRelease(boolean isGPRelease) {
        isgprelease = isGPRelease;
    }

    public boolean isGPRelease() {
        return isgprelease;
    }

    public void addView(View view) {
        gamesurface.addView(view);
    }

    /**
     * Setup for IAP SDK called from onCreate. Sets up {@link SampleIapManager}
     * to handle InAppPurchasing logic and {@link SamplePurchasingListener} for
     * listening to IAP API callbacks
     */
    private void setupIAPOnCreate() {
        sampleIapManager = new SampleIapManager(this);
        sampleIapManager.activate();
        final SamplePurchasingListener purchasingListener = new SamplePurchasingListener(sampleIapManager);
        android.util.Log.d(TAG, "onCreate: registering PurchasingListener");
        PurchasingService.enablePendingPurchases();
        PurchasingService.registerListener(this.getApplicationContext(), purchasingListener);
        android.util.Log.d(TAG, "IS_SANDBOX_MODE:" + LicensingService.getAppstoreSDKMode());
    }

    /**
     * Calls {@link PurchasingService#getUserData()} to get current Amazon
     * user's data
     * and {@link PurchasingService#getProductData(Set)} to get the product
     * availability
     * and {@link PurchasingService#getPurchaseUpdates(boolean)} to
     * get recent purchase updates
     */
    @Override
    protected void onResume() {
        super.onResume();
        makeFullScreen();
        appmanager.onResume();
        sampleIapManager.activate();
        android.util.Log.d(TAG, "onResume: call getUserData");
        PurchasingService.getUserData();
        android.util.Log.d(TAG, "onResume: call getProductData for skus: " + Arrays.toString(MySku.values()));
        final Set<String> productSkus = new HashSet<>();
        for (final MySku mySku : MySku.values()) {
            productSkus.add(mySku.getSku());
        }
        PurchasingService.getProductData(productSkus);
        android.util.Log.d(TAG, "onResume: getPurchaseUpdates");
        PurchasingService.getPurchaseUpdates(false);
    }

    /**
     * Click handler invoked when user clicks button to buy an orange
     * consumable. This method calls {@link PurchasingService#purchase(String)}
     * with the SKU to initialize the purchase from Amazon Appstore
     */
    public void onBuyOrangeClick() {
        final RequestId requestId = PurchasingService.purchase(MySku.ORANGE.getSku());
        android.util.Log.d(TAG, "onBuyOrangeClick: requestId (" + requestId + ")");
    }

    /**
     * Click handler called when user clicks button to eat an orange consumable.
     */
    public void onEatOrangeClick() {
        try {
            sampleIapManager.eatOrange();
            android.util.Log.d(TAG, "onEatOrangeClick: consuming 1 orange");

            updateOrangesInView(sampleIapManager.getUserIapData().getRemainingOranges(),
                    sampleIapManager.getUserIapData().getConsumedOranges());
        } catch (final RuntimeException e) {
            showMessage("Unknow error when eat Orange");
        }
    }

    public void onBuyMagazineClick() {
        final RequestId requestId = PurchasingService.purchase(MySku.PLUS.getSku());
        android.util.Log.d(TAG, "onBuyMagazineClick: requestId (" + requestId + ")");
    }

    // ///////////////////////////////////////////////////////////////////////////////////////
    // ////////////////////////// Application specific code below
    // ////////////////////////////
    // ///////////////////////////////////////////////////////////////////////////////////////

    protected Handler guiThreadHandler;

//    protected Button buyOrangeButton;
//    protected Button eatOrangeButton;

    protected int numOranges;
    protected int numOrangesConsumed;

    /**
     * Setup application specific things, called from onCreate()
     */
    protected void setupApplicationSpecificOnCreate() {
//        setContentView(R.layout.activity_main);

//        buyOrangeButton = findViewById(R.id.buy_item_button);

//        eatOrangeButton = findViewById(R.id.eat_orange_button);
//        eatOrangeButton.setEnabled(false);

//        numOranges = getRemain();
//        numOrangesConsumed = findViewById(R.id.num_oranges_consumed);

        guiThreadHandler = new Handler();
    }

    public int getRemain() {
        return sampleIapManager.getUserIapData().getRemainingOranges();
    }

    public boolean isSubcribe() {
        List<SubscriptionRecord> records = sampleIapManager.getUserIapData().getSubscriptionRecords();

        if(records == null) {
            return false;
        }

        return !records.isEmpty();
    }

    /**
     * Disable "Buy Orange" button
     */
    public void disableBuyOrangeButton() {
//        buyOrangeButton.setEnabled(false);
    }

    /**
     * Enable "Buy Orange" button
     */
    public void enableBuyOrangeButton() {
//        buyOrangeButton.setEnabled(true);
    }

    /**
     * Update view with how many oranges I have and how many I've consumed.
     *
     * @param haveQuantity how many oranges the user has
     * @param consumedQuantity how many oranges have been consumed by the user
     */
    public void updateOrangesInView(final int haveQuantity, final int consumedQuantity) {
        android.util.Log.d(TAG, "updateOrangesInView with haveQuantity (" + haveQuantity
                + ") and consumedQuantity ("
                + consumedQuantity
                + ")");
        guiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                  numOranges = haveQuantity;
                  numOrangesConsumed = consumedQuantity;

                  if(numOranges > 0){
                      showMessage("Remaining Turns: " + numOranges);
                  }
            }
        });
    }

    public void setMagazineSubsAvail(final boolean productAvailable, final boolean userCanSubscribe) {
        if (productAvailable) {
            if (userCanSubscribe) {
//                showMessage("userCanSubscribe: " + userCanSubscribe);
            } else {
                showMessage("You can't subcribe at this time");
            }
        } else {
            showMessage("The product is temporary unavailable!");
        }

    }

    /**
     * Show message on UI
     *
     * @param message message to show in the UI
     */
    public void showMessage(final String message) {
        Toast.makeText(BaseActivity.this, message, Toast.LENGTH_LONG).show();
    }
}
