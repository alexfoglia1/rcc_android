package com.alexfoglia.rcc;

import android.app.*;
import android.os.*;
import android.widget.*;
import android.view.*;
import android.content.*;
import android.util.*;
import androidx.core.app.*;
import java.util.jar.*;
import android.*; 
import java.security.*;
import android.Manifest.*;
public class MainActivity extends Activity 
{
	private static String[] STORAGE_PERMISSIONS =
	{
		"android.permission.READ_EXTERNAL_STORAGE",
		"android.permission.WRITE_EXTERNAL_STORAGE"
	};
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
  		ActivityCompat.requestPermissions(this, STORAGE_PERMISSIONS, 1);
    }
	
    public void onConnect(View v)
    {
		EditText ip = (EditText) findViewById(R.id.ip_addr);
		String addr = ip.getText().toString();

	        Toast.makeText(getBaseContext(), "Will send commands to: "+addr, Toast.LENGTH_SHORT).show();
	        Intent myIntent = new Intent(this, CarActivity.class);
		if (CarActivity.SetRaspberryAddress(addr))
		{
		    startActivity(myIntent);
		}
		else
		{
		    Toast.makeText(getBaseContext(), "Unvalid address", Toast.LENGTH_SHORT).show();
		}
    }
}
