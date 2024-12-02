import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.Enumeration;

public class DiscoveryListener extends Thread {
    public Node node;

    public DiscoveryListener(Node node){
       this.node = node;
    }

    public void run(){
        System.out.println("Starting discovery listening on port 4999");
            try
            {
                MulticastSocket ms = new MulticastSocket();
                InetSocketAddress group_ip = new InetSocketAddress(InetAddress.getByName("230.1.2.3"),4999);
                NetworkInterface nif = NetworkInterface.getByIndex(1);
                ms.joinGroup(group_ip, nif);                
                System.out.println(NetworkInterface.getNetworkInterfaces().toString());

                DatagramPacket packet;
                while(true)
                {
                    byte[] byte_message = new byte[65535];
                    packet = new DatagramPacket(byte_message, byte_message.length);
                    ms.receive(packet);
                    InetAddress remote_ip = packet.getAddress();
                    node.receive(byte_message,remote_ip);
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
    }
}
