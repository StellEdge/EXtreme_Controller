package com.nope.sjtu.extremecontroller;

import android.app.Service;
//import android.content.Context;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT=12345;
    private List<Socket> mList=new ArrayList<Socket>();
    private ServerSocket server=null;
    private ExecutorService mExecutorService=null;

    public static void main(String[] args){
        new Server();
    }

    public Server(){
        try{
            server=new ServerSocket(PORT);
            mExecutorService= Executors.newCachedThreadPool();
            System.out.println("executing...\n");
            Socket client=null;
            while(true){
                client=server.accept();
                mList.add(client);
                mExecutorService.execute(new Service(client));
                //System.out.println("new thread 12");
            }
        }catch (Exception e){e.printStackTrace();}
    }

    class Service implements Runnable{
        private Socket socket;
        private BufferedReader in=null;
        private String msg="";

        public Service(Socket socket){
            this.socket=socket;
            try{
                in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                msg = "client:" +this.socket.getInetAddress() + "已连接"
                        +"当前client数量:" +mList.size();
                this.sendmsg();
            }catch(IOException e){e.printStackTrace();}
        }

        @Override
        public void run(){
            try{
                while(true){
                    if((msg=in.readLine())!=null){
                        if(msg.equals("finish")) {
                            System.out.println("~~~~~~~");
                            mList.remove(socket);
                            in.close();
                            msg = "client:" + this.socket.getInetAddress() + "已断开连接"
                                    + "当前client数量:" + mList.size();
                        }
                        else{
                            msg=socket.getInetAddress()+" 发送："+msg;
                            this.sendmsg();
                        }
                    }
                }
            }catch(Exception e){e.printStackTrace();}
        }

        public void sendmsg()
        {
            System.out.println(msg);
            int num = mList.size();
            for(int index = 0;index < num;index++)
            {
                Socket mSocket = mList.get(index);
                PrintWriter pout = null;
                try {
                    pout = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(mSocket.getOutputStream(),"UTF-8")),true);
                    pout.println(msg);
                }catch (IOException e) {e.printStackTrace();}
            }
        }

    }


}
