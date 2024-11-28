import java.io.Serializable;
import java.net.InetAddress;

public class RemoteNode implements Serializable{
    public String name;
    public InetAddress ip;
    public int port;
    
    @Override
    public String toString() {
        String str = "";
        if (ip == null){
            str = String.format("Node name:%s, ip:%s, port:%d", name, "localhost", port);
        }
        else {
            str = String.format("Node name:%s, ip:%s, port:%d", name, ip.toString(), port);
        }
        return str;
    }
}
