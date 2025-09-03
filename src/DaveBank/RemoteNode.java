import java.io.Serializable;
import java.net.InetAddress;
import java.util.Objects;

public class RemoteNode implements Serializable{
    public String name;
    public InetAddress ip;
    public int port;
    
    @Override
    public String toString() {
        String str = "";
        if (ip == null){
            str = String.format("Name:%s, ip:%s, port:%d", name, "localhost", port);
        }
        else {
            str = String.format("Name:%s, ip:%s, port:%d", name, ip.toString(), port);
        }
        return str;
    }
     @Override
    public boolean equals(Object object){
        if (! (object instanceof RemoteNode))
            return false;
        RemoteNode that = (RemoteNode)object;
        if (this.ip == null && that.ip == null){
            return true;
        }
        else if (this.ip == null || that.ip == null){
            return false;
        }
        else{
            return this.ip.equals(that.ip) && this.port == that.port;
        }
    }

    @Override
    public int hashCode() {
        if (this.ip != null){
            return Objects.hash(this.ip,this.port);
        }
        else{
            return Objects.hash(this.port);
        }
    }
}
