package com.alexfoglia.rcc;
import android.app.*;
import android.os.*;
import java.net.*;
import java.nio.*;
import android.widget.*;
import android.graphics.*;
import java.io.*;
import android.content.*;
import android.view.*;
import java.util.*;
import org.opencv.android.*;
import android.util.*;
import org.opencv.core.*;
import org.opencv.imgcodecs.*;
import org.opencv.imgproc.*;
import android.net.*;

public class CarActivity extends Activity
{
	private static String raspberryAddr;
	private static final int DATA_PORT = 1234;
	private static final int VIDEO_PORT = 4321;
	private static final int MAX_DATAGRAM_LENGTH = 60000;
	private static final int joystickXYMessageId = 6;
	private static final int joystickThrottleMessageId = 7;
	private static final int joystickBrakeMessageId = 8;
	private static final int joystickMessageIdIndex = 0;
	private static final int joystickMessageValueIndex = 4;
	private static final int joystickXYMessageLength = 6;
	private static final int joystickThrottleBreakMessageLength = 5;
	private static boolean validIP (String ip)
	{
		try {
			if ( ip == null || ip.isEmpty() ) {
				return false;
			}

			String[] parts = ip.split( "\\." );
			if ( parts.length != 4 ) {
				return false;
			}

			for ( String s : parts ) {
				int i = Integer.parseInt( s );
				if ( (i < 0) || (i > 255) ) {
					return false;
				}
			}
			if ( ip.endsWith(".") ) {
				return false;
			}
			
			return true;
		} catch (NumberFormatException nfe) {
			return false;
		}
	}
	
	public static boolean SetRaspberryAddress(String addr)
	{
		if(validIP(addr))
		{
	        raspberryAddr = addr;
	        return true;
		}
		else
		{
			return false;
		}
	}
	
	private DatagramSocket videosock, datasock;
	private android.graphics.Point size = new android.graphics.Point();
	private InetAddress daddr;
	
	private byte[] recvImage(DatagramSocket sock)
	{
		try
		{
			 byte[] entireUdpData = new byte[MAX_DATAGRAM_LENGTH];
			 DatagramPacket p = new DatagramPacket(entireUdpData, entireUdpData.length);
		     sock.receive(p);
			 
			 byte[] imageLengthUdpData = new byte[4];
			 imageLengthUdpData[3] = entireUdpData[0];
			 imageLengthUdpData[2] = entireUdpData[1];
			 int imageLengthValue = ByteBuffer.wrap(imageLengthUdpData).getInt();
			
			 int dataFieldOffset = 0x02;
			 byte[] imageBytesUdpData = new byte[imageLengthValue];
			 for(int i = 0; i < imageLengthValue; i++)
			 {
				 imageBytesUdpData[i] = entireUdpData[dataFieldOffset + i];
			 }
			 
			 return imageBytesUdpData;
		}
		catch(Exception e)
		{
			displayMessage(e.toString());
			return null;
		}
	}
	
	private void sendWrapper(final DatagramPacket p)
	{
	    new Thread(new Runnable()
	    {
	        public void run()
	        {
	            try
		        {
			        datasock.send(p); 
		        }
		        catch(Exception e)
		        {
			        displayMessage(e.toString());
		        }
	        }
	    }).start();
	  
	}
	
	private void sendJoystickCommandToBoard(byte x_axis, byte y_axis)
	{
	    byte[] bufx, bufy;
		bufx = new byte[joystickXYMessageLength];
		bufy = new byte[joystickThrottleBreakMessageLength];
				
		bufx[joystickMessageIdIndex] = joystickXYMessageId;
		bufx[joystickMessageValueIndex] = x_axis;
		bufy[joystickMessageIdIndex] = y_axis < 0 ? (byte) joystickBrakeMessageId : (byte)joystickThrottleMessageId;
		bufy[joystickMessageValueIndex] = y_axis < 0 ? (byte)(-2*y_axis):(byte)(2*y_axis);
				
		DatagramPacket joystickXY = new DatagramPacket(bufx, bufx.length, daddr, DATA_PORT);
		DatagramPacket joystickThrottle = new DatagramPacket(bufy, bufy.length, daddr, DATA_PORT);
		sendWrapper(joystickXY);
		sendWrapper(joystickThrottle);
		displayMessage("OUT: heading(" + x_axis + ") throttle(" + (2*y_axis) + ")");
	}
	
	private void refreshImage(byte[] jpgBytes)
	{
	    if(jpgBytes != null)
		{
			ImageView image = (ImageView) findViewById(R.id.imageView1);
			try 
			{
				Bitmap bmp = BitmapFactory.decodeByteArray(jpgBytes, 0, jpgBytes.length);
				image.setImageBitmap(Bitmap.createScaledBitmap(bmp, size.x, 3*size.y/4, false));
			}
			catch (Exception e)
			{
				displayMessage(e.toString());
			}
		}
	}
	
	private void displayMessage(String err)
	{
		TextView t = (TextView) findViewById(R.id.carActivityUserPrompt);
		t.setText(err);
	}
	
	public void onBreak(View v)
	{
        sendJoystickCommandToBoard((byte)0, (byte)0);
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.car);
		try
		{
			videosock = new DatagramSocket(VIDEO_PORT);
			datasock = new DatagramSocket(DATA_PORT);
			daddr = InetAddress.getByName(raspberryAddr);
		}
		catch(Exception e)
		{
			displayMessage(e.toString());
			return;
		}
		
	    Display display;
		display = getWindowManager().getDefaultDisplay();
		display.getSize(size);
		
		HealthStatus hs = new HealthStatus(raspberryAddr);
		if(!hs.initHealthStatus())
		{
		    displayMessage("Cannot start health status");
		    return;
		}
		else
		{
		    hs.start();
		}
		
		new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
			        while (true)
				    {
						final byte[] jpgBytes = recvImage(videosock);
						runOnUiThread(new Runnable()
						{
							public void run()
							{
								refreshImage(jpgBytes);
							}
						});
					}
			    } catch(Exception e)
				{
					displayMessage(e.toString());
					return;
				}
			}
		}).start();
		
		sendJoystickCommandToBoard((byte)0, (byte)0);
    }

	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		final float xPercentage = event.getX()/size.x;
		final float yPercentage = 1.0f - event.getY()/(size.y);
		final int int8MinValue = -128;
		final int int8ValueRange = 255;
		
		int x = int8MinValue + (int)(xPercentage * int8ValueRange);
		byte bx = event.getAction() == MotionEvent.ACTION_UP ? 0 : (byte)x;
		
		int y = int8MinValue + (int)(yPercentage * int8ValueRange);
		byte by = event.getAction() == MotionEvent.ACTION_UP ? 0 : (byte)y;
		
		final byte x_axis = bx;
		final byte y_axis = by;
		
		sendJoystickCommandToBoard(x_axis, y_axis);

		return super.onTouchEvent(event);       
	}
	
}
