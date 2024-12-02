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
        String multicastGroupAddress = "230.0.0.0"; // Multicast group address
        int port = 4999; // Port for the multicast

        try (MulticastSocket socket = new MulticastSocket(port)) {
            InetAddress group = InetAddress.getByName(multicastGroupAddress);

            // Join the multicast group
            socket.joinGroup(group);
            System.out.println("Joined multicast group " + multicastGroupAddress);
            DatagramPacket packet;
            while (true){
                byte[] byte_message = new byte[65535];
                packet = new DatagramPacket(byte_message, byte_message.length);
                socket.receive(packet);
                InetAddress remote_ip = packet.getAddress();
                node.receive(byte_message,remote_ip);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

        
