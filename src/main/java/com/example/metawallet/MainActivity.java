package com.example.metawallet;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.example.metawallet.ui.home.HomeFragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.metawallet.databinding.ActivityMainBinding;
import com.jacksonandroidnetworking.JacksonParserFactory;

import java.io.File;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONArray;
import org.w3c.dom.Text;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.Provider;
import java.security.Security;

import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private ActionBarDrawerToggle toggle;

    Web3j web3;
    File file;
    String Walletname;
    Credentials credentials;
    String walletName;
    String eth_usd_spot_rate;
    Double eth_balance;
    Double usd_balance;

    // UI
    EditText editTextWalletName;
    EditText editTextWalletPassword;
    Button buttonCreate;
    Button buttonLoad;
    TextView textViewWalletName;
    TextView textViewWalletAddress;
    TextView textViewStatus;
    //RelativeLayout onConnected;
    RelativeLayout walletControl;
    //ScrollView home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        toggle = new ActionBarDrawerToggle(this, drawer, binding.appBarMain.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }
        };
        drawer.setDrawerListener(toggle);

        // ------------------------------------------------------------------------------------
        //onConnected = findViewById(R.id.on_connected);
        //walletControl = findViewById(R.id.wallet_control);
        // Edit Text
        editTextWalletName = findViewById(R.id.wallet_path);
        editTextWalletPassword = findViewById(R.id.password);

        // Text Views
        textViewWalletName = findViewById(R.id.wallet_name_text_view);
        textViewWalletAddress = findViewById(R.id.wallet_address);
//        textViewStatus = findViewById(R.id.status);

        // Button
        buttonCreate = findViewById(R.id.create_btn);
        buttonLoad = findViewById(R.id.load_wallet_btn);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // For making API calls
        AndroidNetworking.initialize(getApplicationContext());

        // Adding an Network Interceptor for Debugging purpose :
        OkHttpClient okHttpClient = new OkHttpClient() .newBuilder().build();
        AndroidNetworking.initialize(getApplicationContext(),okHttpClient);

        //AndroidNetworking.setParserFactory(new JacksonParserFactory(4));

        AndroidNetworking.get("https://api.coinbase.com/v2/prices/ETH-USD/spot")
                .setPriority(Priority.LOW)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {

                    }
                    @Override
                    public void onError(ANError error) {
                        eth_usd_spot_rate = error.getMessage();
                        String[] temp = eth_usd_spot_rate.split("\"");
                        try {
                            eth_usd_spot_rate = temp[13];
                        }
                        catch (Exception e){
                            toast("rate loaded");
                            eth_usd_spot_rate = "4572";
                        }
                    }
                });


        web3 = Web3j.build(new HttpService("https://ropsten.infura.io/v3/478f7d1e640c4555b852ecf764e1ef38"));
        setupBouncyCastle();
    }

    public void createWallet(View v)  {
        EditText Edtpassword = findViewById(R.id.password);
        EditText editTextWalletPath = findViewById(R.id.wallet_path);

        final String password = Edtpassword.getText().toString();
        final String walletFilePath = editTextWalletPath.getText().toString();
        try {

            file = new File(getFilesDir() + "/" + walletFilePath);

            if (!file.exists()) {
                file.mkdirs();
                Walletname = WalletUtils.generateLightNewWalletFile(password, file);
                credentials = WalletUtils.loadCredentials(password, file + "/" + Walletname);
                walletName = walletFilePath;
                toast("Wallet Created");
            }
            else
                toast("Directory already created");
        }
        catch(Exception e){
            toast(e.getMessage());
        }
    }


    public void sendTransaction(View v) {
//        if(credentials == null){
//            toast("Wallet is not connected");
//            return;
//        }
        EditText sendAmount = findViewById(R.id.send_amount);
        EditText receiver = findViewById(R.id.receiver_address);
        String receiverAddress = receiver.getText().toString();

        BigDecimal value = new BigDecimal(sendAmount.getText().toString());
        try{
            TransactionReceipt receipt = Transfer.sendFunds(web3, credentials,receiverAddress,value, Convert.Unit.ETHER).send();
            Toast.makeText(this, "Transaction successful: " +receipt.getTransactionHash(), Toast.LENGTH_LONG).show();
        }
        catch(Exception e){
            toast("No have sufficient ETH balance");
        }
    }

    public void loadWallet(View v)  {
        EditText Edtpassword = findViewById(R.id.load_wallet_password);
        EditText EdtWalletName = findViewById(R.id.load_wallet_path);

        final String password = Edtpassword.getText().toString();
        try {
            final String walletFilePath = EdtWalletName.getText().toString();
            file = new File(getFilesDir() + "/" + walletFilePath);

            if (!file.exists()) {
               toast("Wallet does not exists");
            }
            else {
                File walletFile = file.listFiles()[0];
                credentials = WalletUtils.loadCredentials(password, walletFile);
                walletName = EdtWalletName.getText().toString();
                toast("Wallet loaded");
            }
        }
        catch(Exception e){
            //toast(e.getMessage());
        }
    }

    public void retrieveWalletBalance (View v)  {
        try {
            if(credentials != null){
                EthGetBalance balanceWei = web3.ethGetBalance(credentials.getAddress(), DefaultBlockParameterName.LATEST).sendAsync().get();
                BigDecimal balanceInEther = Convert.fromWei(balanceWei.getBalance().toString(), Convert.Unit.ETHER);

                TextView textViewBalance = findViewById(R.id.wallet_balance);
                textViewBalance.setText( balanceInEther.toString() + " " + getString(R.string.balance_unit));

                setWalletAddress(credentials.getAddress());

            }
        }
        catch (Exception e){
            toast(e.getMessage());
        }
    }

    public void retrieveWalletBalance2 ()  {
        try {
            if(credentials != null){
                EthGetBalance balanceWei = web3.ethGetBalance(credentials.getAddress(), DefaultBlockParameterName.LATEST).sendAsync().get();
                BigDecimal balanceInEther = Convert.fromWei(balanceWei.getBalance().toString(), Convert.Unit.ETHER);
                eth_balance = balanceInEther.doubleValue();

                TextView textViewBalance = findViewById(R.id.wallet_balance);
                textViewBalance.setText( balanceInEther.toString() + " " + getString(R.string.balance_unit));

                setWalletAddress(credentials.getAddress());

                TextView textViewWalletName = findViewById(R.id.wallet_name_text_view);
                textViewWalletName.setText(walletName);

                TextView totBalance = findViewById(R.id.total_usd_balance);
                TextView ethTousdBalance = findViewById(R.id.eth_usd_balance);
                Double rate = Double.parseDouble(eth_usd_spot_rate);
                if(eth_balance > 0.000000){
                    usd_balance = eth_balance * rate;
                    totBalance.setText(String.format("$ %.2f", usd_balance));
                    ethTousdBalance.setText(String.format("$ %.2f", usd_balance));
                }
            }
        }
        catch (Exception e){
            //toast(e.getMessage());
        }
    }

    private void setupBouncyCastle() {
        final Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (provider == null) {
            return;
        }
        if (provider.getClass().equals(BouncyCastleProvider.class)) {
            return;
        }
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }


//    public void testNetworkConnection() {
//        try {
//            Web3ClientVersion clientVersion = web3.web3ClientVersion().sendAsync().get();
//            if (!clientVersion.hasError()) {
//                toast("Connected to Ethereum network");
//            } else {
//                toast(clientVersion.getError().getMessage());
//            }
//        } catch (Exception e) {
//            toast(e.getMessage());
//        }
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public void setWalletAddress(String address){
        TextView wallet_address_view = findViewById(R.id.wallet_address);
        String subAddress = address.substring(0,5) + "..." + address.substring(address.length() - 4);
        wallet_address_view.setText(subAddress);
    }

    public void copyWalletAddress(View v){
        try{
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("address", credentials.getAddress());
            clipboard.setPrimaryClip(clip);
            toast("Wallet Address Copied");
        }
        catch (Exception e){
            //toast(e.getMessage(), "OK");
        }
    }

    public void toast(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }
}