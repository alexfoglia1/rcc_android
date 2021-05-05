package com.alexfoglia.rcc;
import java.net.*;
import java.nio.*;

public class HealthStatus extends Thread
{
    private static final int  HLT_PORT   = 1111;
    private static final byte  HLT_MSG_ID = 0x05;
    private static final byte HLT_WHOAMI = 0x02;
    private static final int  HLT_SIZE   = 0x05;
    
    private static final double HEALTH_STATUS_RATE = 10.0;
    
    private String raspberryAddr;
    private DatagramSocket sock;
    private byte[] healthStatusBytes;
    private DatagramPacket p;
    private boolean isInit;
    public HealthStatus(String raspberryAddr)
    {
        this.raspberryAddr = raspberryAddr;
        this.healthStatusBytes = new byte[HLT_SIZE];
        this.isInit = false;
    }
    
    public boolean initHealthStatus()
    {
        try
        {
            this.sock = new DatagramSocket(HLT_PORT);
            
            this.healthStatusBytes[0] = HLT_MSG_ID;
            this.healthStatusBytes[4] = HLT_WHOAMI;
            this.p = new DatagramPacket(this.healthStatusBytes,
                                        this.healthStatusBytes.length,
                                        InetAddress.getByName(this.raspberryAddr),
                                        HealthStatus.HLT_PORT);
            
            this.isInit = true;
        }
        catch(Exception e)
        {
            this.isInit = false;
        }
        
        return this.isInit;
    }
    
    public void run()
    {
        if(!isInit)
        {
            return;
        }
        
        long dT_millis = (long)((1.0 / HEALTH_STATUS_RATE) * 1e3);
        while(true)
        {
            try
            {
                this.sock.send(this.p);
                Thread.sleep(dT_millis);
            }
            catch(Exception e)
            {
                return;
            }
        }
    }
}
