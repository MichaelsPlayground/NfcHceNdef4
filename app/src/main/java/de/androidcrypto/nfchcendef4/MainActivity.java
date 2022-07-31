package de.androidcrypto.nfchcendef4;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    // https://stackoverflow.com/questions/56168943/android-hce-card-emulation-not-calling-processcommandapdu

    private NfcAdapter nfcAdapter;
    private byte[] mNdefRecordFile;
    Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mIntent = new Intent(this, MyHostApduService.class);
        this.startService(mIntent);

    }
}