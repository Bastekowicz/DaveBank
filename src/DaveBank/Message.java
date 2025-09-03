import java.io.Serializable;

public class Message implements Serializable{
    public MessageType type;
    public RemoteNode sending_node;
    public RemoteNode other_node;
    public DataItem data_item;
    public int hash;
    public Message(MessageType type, RemoteNode sending_node, DataItem data_item){
        this.type = type;
        this.sending_node = sending_node;
        this.data_item = data_item;
    }
    public Message(MessageType type, RemoteNode sending_node){
        this.type = type;
        this.sending_node = sending_node;
    }
    public Message(MessageType type, RemoteNode sending_node, RemoteNode other_node){
        this.type = type;
        this.sending_node = sending_node;
        this.other_node = other_node;
    }

}