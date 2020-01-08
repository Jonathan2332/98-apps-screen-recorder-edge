package a98apps.recorderedge.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import a98apps.recorderedge.R;
import a98apps.recorderedge.util.ThemeMode;

public class DonateActivity extends AppCompatActivity implements PurchasesUpdatedListener {

    private BillingClient billingClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeMode.checkTheme(this, false, true);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate_layout);

        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.ic_favorite_34dp);

        getSupportActionBar().setTitle(R.string.text_donate);

        final ProgressBar progressBar = findViewById(R.id.loader);

        final List<String> skuList = new ArrayList<>();

        skuList.add("donate.1");
        skuList.add("donate.2");
        skuList.add("donate.5");
        skuList.add("donate.10");
        skuList.add("donate.20");
        skuList.add("donate.30");
        skuList.add("donate.40");
        skuList.add("donate.50");
        skuList.add("donate.100");

        billingClient = BillingClient.newBuilder(this).setListener(this).enablePendingPurchases().build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult)
            {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK)
                {
                    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                    params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
                    billingClient.querySkuDetailsAsync(params.build(), new SkuDetailsResponseListener() {
                        @Override
                        public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList)
                        {
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null)
                            {
                                progressBar.setVisibility(View.GONE);
                                for (SkuDetails skuDetails : skuDetailsList)
                                {
                                    setButtons(skuDetails);
                                }
                            }
                        }
                    });
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                progressBar.setVisibility(View.GONE);
            }
        });
    }
    private void setButtons(final SkuDetails skuDetails)
    {
        Button button = findViewById(getIdButton(skuDetails.getSku()));
        String text = getString(R.string.text_supports_me) + skuDetails.getPrice();
        button.setText(text);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                BillingFlowParams flowParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build();
                billingClient.launchBillingFlow(DonateActivity.this, flowParams);
            }
        });
    }
    private int getIdButton(String sku)
    {
        switch (sku)
        {
            case "donate.1":
                return R.id.donate_1;
            case "donate.2":
                return R.id.donate_2;
            case "donate.5":
                return R.id.donate_5;
            case "donate.10":
                return R.id.donate_10;
            case "donate.20":
                return R.id.donate_20;
            case "donate.30":
                return R.id.donate_30;
            case "donate.40":
                return R.id.donate_40;
            case "donate.50":
                return R.id.donate_50;
            default:
                return R.id.donate_100;
        }
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> list) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null)
        {
            Toast.makeText(this, getString(R.string.text_thank_you), Toast.LENGTH_LONG).show();
        }
    }
}
