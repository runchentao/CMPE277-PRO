package com.example.metawallet;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.metawallet.databinding.ActivityMainBinding;

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
import org.web3j.crypto.Credentials;
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
import java.math.BigDecimal;
import java.security.Provider;
import java.security.Security;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    Web3j web3;
    File file;
    String Walletname;
    Credentials credentials;

    // UI
    EditText editTextWalletName;
    EditText editTextWalletPassword;
    Button buttonCreate;
    Button buttonLoad;
    TextView textViewOr;
    TextView textViewWalletAddress;
    TextView textViewStatus;
    RelativeLayout onConnected;
    RelativeLayout walletControl;

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

        navigationView.getMenu().findItem(R.id.nav_gallery).setEnabled(false);
        navigationView.getMenu().findItem(R.id.nav_slideshow).setEnabled(false);

        onConnected = findViewById(R.id.on_connected);
        walletControl = findViewById(R.id.wallet_control);
        // Edit Text
        editTextWalletName = findViewById(R.id.wallet_path);
        editTextWalletPassword = findViewById(R.id.password);

        // Text Views
        textViewWalletAddress = findViewById(R.id.wallet_address);
        textViewOr = findViewById(R.id.textview_or);
        textViewStatus = findViewById(R.id.status);

        // Button
        buttonCreate = findViewById(R.id.create_btn);
        buttonLoad = findViewById(R.id.load_btn);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        web3 = Web3j.build(new HttpService("https://ropsten.infura.io/v3/478f7d1e640c4555b852ecf764e1ef38"));
        setupBouncyCastle();

        testNetworkConnection();
    }

    public void createWallet(View v)  {
        EditText Edtpassword = findViewById(R.id.password);
        final String password = Edtpassword.getText().toString();  // this will be your etherium password
        try {
            final String walletFilePath = editTextWalletName.getText().toString();
            file = new File(getFilesDir() + "/" + walletFilePath);

            if (!file.exists()) {
                toast(file.getPath());
                file.mkdirs();
                Walletname = WalletUtils.generateLightNewWalletFile(password, file);
                credentials = WalletUtils.loadCredentials(password, file + "/" + Walletname);

                setWalletAddress(credentials.getAddress());
                hideAfterConnect();
                onConnected();
            }
            else
                toast("Directory already created");
        }
        catch(Exception e){
            toast(e.getMessage());
        }
    }

    public void loadWallet(View v)  {
        EditText Edtpassword = findViewById(R.id.password);
        final String password = Edtpassword.getText().toString();
        try {
            final String walletFilePath = editTextWalletName.getText().toString();
            file = new File(getFilesDir() + "/" + walletFilePath);

            if (!file.exists()) {
               toast("Wallet does not exists");
            }
            else {
                File walletFile = file.listFiles()[0];
                credentials = WalletUtils.loadCredentials(password, walletFile);

                setWalletAddress(credentials.getAddress());
                hideAfterConnect();
                onConnected();
            }
        }
        catch(Exception e){
            toast(e.getMessage());
        }
    }

    public void retrieveWalletBalance (View v)  {
        try {
            EthGetBalance balanceWei = web3.ethGetBalance(credentials.getAddress(), DefaultBlockParameterName.LATEST).sendAsync().get();
            BigDecimal balanceInEther = Convert.fromWei(balanceWei.getBalance().toString(), Convert.Unit.ETHER);

            TextView textViewBalance = findViewById(R.id.wallet_balance);
            textViewBalance.setText( balanceInEther.toString() + " " + getString(R.string.balance_unit));
        }
        catch (Exception e){
            toast(e.getMessage());
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

    public void testNetworkConnection() {
        try {
            Web3ClientVersion clientVersion = web3.web3ClientVersion().sendAsync().get();
            if (!clientVersion.hasError()) {
                toast("Connected to Ethereum network");
            } else {
                toast(clientVersion.getError().getMessage());
            }
        } catch (Exception e) {
            toast(e.getMessage());
        }
    }

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

    public void toast(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    public void hideAfterConnect() {
        editTextWalletName.setVisibility(View.GONE);
        editTextWalletPassword.setVisibility(View.GONE);
        buttonCreate.setVisibility(View.GONE);
        buttonLoad.setVisibility(View.GONE);
        textViewOr.setVisibility(View.GONE);
    }

    public void onConnected(){
        onConnected.setVisibility(View.VISIBLE);
        walletControl.setVisibility(View.VISIBLE);
        textViewStatus.setText(R.string.status_connected);
    }

    public void setWalletAddress(String address){
        String subAddress = address.substring(0,5) + "..." + address.substring(address.length() - 4);
        textViewWalletAddress.setText(subAddress);
    }

    public void copyWalletAddress(View v){
        try{
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("address", credentials.getAddress());
            clipboard.setPrimaryClip(clip);
            toast("Copied");
        }
        catch (Exception e){
            toast(e.getMessage());
        }

    }
}