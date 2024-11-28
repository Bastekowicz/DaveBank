import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class NetworkListener extends Thread {
    public Node node;
    public int listening_port;

    public NetworkListener(Node node,int listening_port){
       this.node = node;
       this.listening_port = listening_port;
    }

    public void run(){
        System.out.println("Starting network listening on port: "+listening_port);
            try
            {
                DatagramSocket socket = new DatagramSocket(listening_port);
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
