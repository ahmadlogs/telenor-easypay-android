package com.example.telenoreasypayandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class PaymentActivity extends AppCompatActivity {
    private WebView mWebView;

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    private final String STORE_ID   = "ENTER_STORE_ID";
    private final String HASH_KEY   = "ENTER_HASH_KEY";

    private static final String POST_BACK_URL1 = "http://localhost/easypay/order_confirm.php";
    private static final String POST_BACK_URL2 = "http://localhost/easypay/order_complete.php";

    //Live
    private final String TRANSACTION_POST_URL1   = "https://easypay.easypaisa.com.pk/easypay/Index.jsf";
    private final String TRANSACTION_POST_URL2   = "https://easypay.easypaisa.com.pk/easypay/Confirm.jsf";

    //Sandbox Testing
    //private final String TRANSACTION_POST_URL1   = "https://easypaystg.easypaisa.com.pk/easypay/Index.jsf";
    //private final String TRANSACTION_POST_URL2   = "https://easypaystg.easypaisa.com.pk/easypay/Confirm.jsf";
    //////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String price="";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        mWebView = (WebView) findViewById(R.id.activity_payment_webview);
        // Enable Javascript
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        mWebView.setWebViewClient(new MyWebViewClient());
        webSettings.setDomStorageEnabled(true);

        Intent intentData = getIntent();
        price = intentData.getStringExtra("price");

        Date Date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYYMMDD HHMMSS");
        String DateString = dateFormat.format(Date);
        String orderRefNumString = "T" + DateString;
        System.out.println("AhmadLogs: orderRefNum : " +orderRefNumString);

        // Convert Date to Calendar
        Calendar c = Calendar.getInstance();
        c.setTime(Date);
        c.add(Calendar.HOUR, 1);

        // Convert calendar back to Date
        Date currentDateHourPlusOne = c.getTime();
        String expiryDateString = dateFormat.format(currentDateHourPlusOne);
        System.out.println("AhmadLogs: expiryDateString : " +expiryDateString);


        String storeId = STORE_ID;
        String amount = price;
        String postBackURL = POST_BACK_URL1;
        String orderRefNum = orderRefNumString;
        String expiryDate = expiryDateString;
        String autoRedirect = "1";
        String paymentMethod = "MA_PAYMENT_METHOD";
        //OTC_PAYMENT_METHOD
        //MA_PAYMENT_METHOD
        //CC_PAYMENT_METHOD

        //hash generate
        String sortedString = "";
        sortedString += amount + "&";
        sortedString += autoRedirect + "&";
        sortedString += expiryDate + "&";
        sortedString += orderRefNum + "&";
        sortedString += paymentMethod + "&";
        sortedString += postBackURL + "&";
        sortedString += storeId;
        //sortedString = amount=10&expiryDate=20150101 151515&orderRefNum=11001&postBackURL=http://localhost:9081/local/status.php&storeId=28

        String merchantHashedReq = get_hash(sortedString, HASH_KEY);

        String postData = "";

        try {
            postData += URLEncoder.encode("storeId=", "UTF-8")
                    + URLEncoder.encode(storeId, "UTF-8") + "&";

            postData += URLEncoder.encode("amount=", "UTF-8")
                    + URLEncoder.encode(amount, "UTF-8") + "&";

            postData += URLEncoder.encode("postBackURL=", "UTF-8")
                    + URLEncoder.encode(postBackURL, "UTF-8") + "&";

            postData += URLEncoder.encode("orderRefNum=", "UTF-8")
                    + URLEncoder.encode(orderRefNum, "UTF-8") + "&";

            postData += URLEncoder.encode("expiryDate=", "UTF-8")
                    + URLEncoder.encode(expiryDate, "UTF-8") + "&";

            postData += URLEncoder.encode("merchantHashedReq=", "UTF-8")
                    + URLEncoder.encode(merchantHashedReq, "UTF-8") + "&";

            postData += URLEncoder.encode("autoRedirect=", "UTF-8")
                    + URLEncoder.encode(autoRedirect, "UTF-8") + "&";

            postData += URLEncoder.encode("paymentMethod=", "UTF-8")
                    + URLEncoder.encode(paymentMethod, "UTF-8");


        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        mWebView.postUrl(TRANSACTION_POST_URL1, postData.getBytes());
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            System.out.println("AhmadLogs: onPageStarted - url : " +url);
            //http://localhost/easypay/order_confirm.php?auth_token=s234f5gH7jFb5d
            //http://localhost/easypay/order_complete.php?status=000&desc=completed&orderRefNumber=123

            String responseSplit[] = url.split("\\?");
            String redirect_url = responseSplit[0];
            String response = responseSplit[1];

            System.out.println("AhmadLogs: onPageStarted - redirect_url : " +redirect_url);
            System.out.println("AhmadLogs: onPageStarted - response : " +response);

            if(redirect_url.equals(POST_BACK_URL1)) {
                System.out.println("AhmadLogs: return url1 cancelling");
                view.stopLoading();

                String auth_tokenString = "";
                String[] values = response.split("&");
                for (String pair : values) {
                    String[] nameValue = pair.split("=");
                    if (nameValue.length == 2) {
                        System.out.println("AhmadLogs: Name:" + nameValue[0] + " value:" + nameValue[1]);

                        if (nameValue[0] == "auth_token") {
                            auth_tokenString = nameValue[1];
                            break;
                        }
                    }
                }

                String postData = "";
                postData += "auth_token=" + auth_tokenString + "&";
                postData += "postBackURL=" + POST_BACK_URL2;

                mWebView.postUrl(TRANSACTION_POST_URL2, postData.getBytes());
            }

            else if(redirect_url.equals(POST_BACK_URL2)) {
                //http://localhost/easypay/order_complete.php?status=000&desc=completed&orderRefNumber=123
                System.out.println("AhmadLogs: return url2 cancelling");
                view.stopLoading();

                Intent i = new Intent(PaymentActivity.this, MainActivity.class);
                String[] values = response.split("&");
                for (String pair : values) {
                    String[] nameValue = pair.split("=");
                    if (nameValue.length == 2) {
                        System.out.println("AhmadLogs: Name:" + nameValue[0] + " value:" + nameValue[1]);
                        i.putExtra(nameValue[0], nameValue[1]);
                    }
                }
                setResult(RESULT_OK, i);
                finish();
                return;
            }

            super.onPageStarted(view, url, favicon);
        }
    }

    public static String get_hash(String data, String key) {
        String hashString = "";
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedValue = cipher.doFinal(data.getBytes());
            hashString = Base64.encodeToString(encryptedValue, Base64.DEFAULT);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        return hashString;
    }
}