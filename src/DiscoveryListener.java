import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;

public class DiscoveryListener extends Thread {
    public Node node;

    public DiscoveryListener(Node node){
       this.node = node;
    }

    public void run(){
        System.out.println("Starting discovery listening on port 4999");
            try
            {
                MulticastSocket socket = new MulticastSocket(4999);
                InetAddress group_ip = InetAddress.getByName("230.0.0.1");
                socket.joinGroup(group_ip);
                DatagramPacket packet;
                while(true)
                {
                    byte[] byte_message = new byte[65535];
                    packet = new DatagramPacket(byte_message, byte_message.length);
                    socket.receive(packet);
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
