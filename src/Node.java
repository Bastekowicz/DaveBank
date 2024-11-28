import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;


public class Node {
    public int time = 0;
    public RemoteNode self_remote_node;
    public List<DataItem> data_items = new ArrayList<DataItem>();
    public List<RemoteNode> remote_nodes = new ArrayList<RemoteNode>();
    private KeyboardListener keyboard_listener = new KeyboardListener(this);
    private NetworkListener network_listener;
    private HashMap<RemoteNode, Integer> integrity_hashes = new HashMap<RemoteNode, Integer>();
    public static void main(String[] args){
        System.out.println("Enter:[node name] [port]");
        Scanner sc = new Scanner(System.in);
        String line = sc.nextLine();
        String[] cmd = line.split(" ");
        if (cmd.length != 2)
            System.out.println("Wrong format");
        else {
            try {
                Node node = new Node(cmd[0],Integer.parseInt(cmd[1]));
                return;
            }
            catch (NumberFormatException e){
                System.out.println("Wrong format");
            }
        }
    }

    public List<DataItem> getDataItems(){
        sortDataItems();
        return data_items;
    }

    public List<RemoteNode> getRemoteNodes(){
        return remote_nodes;
    }

    public ArrayList<String> getAccountNames() {
        ArrayList<String> account_names = new ArrayList<String>();
        account_names.addAll(getAccountsBalances().keySet());
        return account_names;
    }

    public HashMap<String, Integer> getAccountsBalances() {
        sortDataItems();
        HashMap<String, Integer> account_balance = new HashMap<String, Integer>();
        account_balance.put("EXTERNAL",Integer.MAX_VALUE); //pseudo-account from which an account can receive funds

        for(DataItem data_item : data_items){
            String payer = data_item.data_s1; //or account to be added/deleted
            String payee = data_item.data_s2;
            int amount = data_item.data_i;

            if (data_item.type == DataType.REMOVE_ACCOUNT){
                account_balance.remove(payer);
                continue;
            }
            if (data_item.type == DataType.ADD_ACCOUNT && account_balance.containsKey(payer)){
                continue;
            }
            if (data_item.type == DataType.ADD_ACCOUNT && !account_balance.containsKey(payer)){
                account_balance.put(payer,0);
                continue;
            }
            if (data_item.type != DataType.TRANSACTION){
                continue;
            }
            if (!account_balance.containsKey(payer) || !account_balance.containsKey(payee)){
                continue;
            }
            if (account_balance.get(payer) < amount){
                continue;
            }
            if (payer != "EXTERNAL"){
                account_balance.put(payer, account_balance.get(payer) - amount);
            }
            if (payee != "EXTERNAL"){
                account_balance.put(payee, account_balance.get(payee) + amount);
            }
        }
        account_balance.remove("EXTERNAL");
        return account_balance;
    }

    public void sortDataItems(){
        Collections.sort(data_items);
    }

    public void addAccountToNetwork(String account_name){
        if(account_name.equals("EXTERNAL")){
            System.out.println("Account name cannot be EXTERNAL");
            return;
        }
        time += 1;
        DataItem data_item = new DataItem(DataType.ADD_ACCOUNT, account_name, time);
        data_items.add(data_item);
        Message message = new Message(MessageType.DATAITEM, self_remote_node, data_item);
        sendMessageToNetwork(message);
        time += 1;
    }

    public void removeAccountFromNetwork(String account_name){
        if(account_name.equals("EXTERNAL")){
            System.out.println("Account name cannot be EXTERNAL");
            return;
        }
            
        time += 1;
        DataItem data_item = new DataItem(DataType.REMOVE_ACCOUNT, account_name, time);
        data_items.add(data_item);
        Message message = new Message(MessageType.DATAITEM, self_remote_node, data_item);
        sendMessageToNetwork(message);
        time += 1;
    }

    public void addTransactionToNetwork(String payer, String payee, int amount){
        time += 1;
        DataItem data_item = new DataItem(DataType.TRANSACTION, payer, payee, amount, time);
        data_items.add(data_item);
        Message message = new Message(MessageType.DATAITEM, self_remote_node, data_item);
        sendMessageToNetwork(message);
        time += 1;
    }

    public void connectToNetwork(InetAddress ip, int port){
        if (remote_nodes.size() > 0){
            System.out.println("Already Connected to Network");
            return;
        }
        if (data_items.size() > 0){
            System.out.println("Clear dataitems before connecting to network");
            return;
        }
        Message message = new Message(MessageType.CONNECT, self_remote_node);
        sendMessageToNode(message, ip, port);
    }

    public void disconnectFromNetwork(){
        if (remote_nodes.size() == 0){
            System.out.println("Not connected to a network");
            return;
        }
        Message message2 = new Message(MessageType.DISCONNECT, self_remote_node);
        sendMessageToNetwork(message2);
    }

    public void checkNetworkIntegrity(){
        if (remote_nodes.size() == 0){
            System.out.println("Not connected to a network");
            return;
        }
        Message message = new Message(MessageType.DATAITEMS_HASH_REQUEST, self_remote_node);
        sendMessageToNetwork(message);
    }

    public void sendMessageToNetwork(Message message){
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        try{
            ObjectOutput oo = new ObjectOutputStream(bStream);
            oo.writeObject(message);
            oo.close();
        }
        catch (Exception e){e.printStackTrace();}
        byte[] serializedMessage = bStream.toByteArray();

        for (RemoteNode remote_node : remote_nodes){
            try
            {
                DatagramSocket ds = new DatagramSocket();
                DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, remote_node.ip, remote_node.port);
                ds.send(packet);
                ds.close();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        

    }

    public void sendMessageToNode(Message message, InetAddress ip, int port){
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        try{
            ObjectOutput oo = new ObjectOutputStream(bStream);
            oo.writeObject(message);
            oo.close();
        }
        catch (Exception e){e.printStackTrace();}
        byte[] serializedMessage = bStream.toByteArray();
        try
            {
                DatagramSocket ds = new DatagramSocket();
                DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, ip, port);
                ds.send(packet);
                ds.close();
            }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void receive(byte[] recBytes,InetAddress ip){
        try{
            ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(recBytes));
            Message message = (Message) iStream.readObject();
            iStream.close();
            message.sending_node.ip = ip;
            switch(message.type){
                case CONNECT:
                    System.out.println("Connect request");
                    handleConnect(message.sending_node);
                    break;
                case DISCONNECT:
                    System.out.println("Disconnect request");
                    remote_nodes.remove(message.sending_node);
                    break;
                case DATAITEM:
                    System.out.println("Dataitem received:");
                    System.out.println(message.data_item.toString());
                    time = Integer.max(message.data_item.time,time) + 1;
                    data_items.add(message.data_item);
                    break;
                case CLEAR_DATAITEMS:
                    System.out.println("Clear dataitems received");
                    clearDataitems();
                    break;
                case ADD_NODE:
                    System.out.println("Add node received");
                    if (!remote_nodes.contains(message.other_node) && self_remote_node.name != message.other_node.name)
                        remote_nodes.add(message.other_node);
                    break;
                case REMOVE_NODE:
                    System.out.println("Remove node received");
                    remote_nodes.remove(message.other_node);
                case DISCOVER:
                    System.out.println("Discover request");
                    break;
                case DATAITEMS_HASH_REQUEST:
                    System.out.println("Hash request");
                    sortDataItems();
                    Message message2 = new Message(MessageType.DATAITEMS_HASH, self_remote_node);
                    message2.hash = getDataItemsHashCode();
                    sendMessageToNode(message2, message.sending_node.ip, message.sending_node.port);
                    break;
                case DATAITEMS_HASH:
                    System.out.println("Hash received");
                    integrity_hashes.put(message.sending_node, message.hash);
                    if (integrity_hashes.size() == remote_nodes.size()){
                        System.out.println("All hashes received, starting check");
                        integrityCheckFinalize();
                    }
                    break;
                case CHECK_INTEGRITY:
                    System.out.println("Check integrity received");
                    checkNetworkIntegrity();
                    break;
            }
        }
        catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
        }
    }

    public void clearDataitems(){
        time = 0;
        data_items.clear();
    }

    public void integrityCheckFinalize(){
        sortDataItems();
        integrity_hashes.put(self_remote_node, getDataItemsHashCode());

        //find most frequent dataitem hash code and generate frequency map for all hashes
        Map<Integer, Integer> freqMap = new HashMap<>();//[hash code] -> [amount of nodes with hashcode]
        int mostFreq = 1;
        int mostFreqCount = -1;
        for (int hash : integrity_hashes.values()) {
            Integer count = freqMap.get(hash);
            freqMap.put(hash, count = (count == null ? 1 : count+1));
            // maintain the most frequent in a single pass.
            if (count > mostFreqCount) {
                mostFreq = hash;
                mostFreqCount = count;
            }
        }

        RemoteNode good_node = self_remote_node;
        if (mostFreqCount != freqMap.get(getDataItemsHashCode())){//if this nodes hashcode is as frequent as the most frequent then prioritise this node
            //if its not find a random node with most frequent hashcode
            for (Map.Entry<RemoteNode, Integer> entry : integrity_hashes.entrySet()) {
                if (mostFreq == entry.getValue()) {
                    good_node = entry.getKey();
                }
            }
        }

        //If this node is the good node coordinate with all other nodes, else tell the remote good node to do all this work.
        if (good_node.equals(self_remote_node)){
            System.out.println("Current node is source of truth");
            int bad_nodes = 0;
            for (Map.Entry<RemoteNode, Integer> entry : integrity_hashes.entrySet()){
                if (entry.getValue() != mostFreq && entry.getKey() != self_remote_node){
                    System.out.println("Correcting node: "+entry.getKey().toString());
                    bad_nodes += 1;
                    Message message = new Message(MessageType.CLEAR_DATAITEMS, self_remote_node);
                    sendMessageToNode(message,entry.getKey().ip,entry.getKey().port);
                    for (DataItem data_item : data_items){
                        Message message2 = new Message(MessageType.DATAITEM,self_remote_node,data_item);
                        sendMessageToNode(message2,entry.getKey().ip,entry.getKey().port);
                    }
                }
            }
            System.out.println("Number of resynced nodes: "+ Integer.toString(bad_nodes));
        }
        else{
            System.out.println("Node: "+good_node.name+" is source of truth");
            Message message = new Message(MessageType.CHECK_INTEGRITY, self_remote_node);
            sendMessageToNode(message, good_node.ip, good_node.port);
        }
    }

    public void handleConnect(RemoteNode node_to_add){
        Message message = new Message(MessageType.ADD_NODE, self_remote_node, node_to_add);
        //send new node info to all network nodes
        sendMessageToNetwork(message);

        //send network nodes to new node
        for (RemoteNode remote_node : remote_nodes){
            Message message2 = new Message(MessageType.ADD_NODE,self_remote_node,remote_node);
            sendMessageToNode(message2, node_to_add.ip, node_to_add.port);
        }

        //send dataitems to new node
        for (DataItem data_item : data_items){
            Message message2 = new Message(MessageType.DATAITEM,self_remote_node,data_item);
            sendMessageToNode(message2, node_to_add.ip, node_to_add.port);
        }
        //add node to array
        remote_nodes.add(node_to_add);

        //add self to new node
        Message message3 = new Message(MessageType.ADD_NODE,self_remote_node,self_remote_node);
        sendMessageToNode(message3, node_to_add.ip, node_to_add.port);
    }

    public int getDataItemsHashCode(){
        List<UUID> id_list = new ArrayList<UUID>();
        for (DataItem data_item: data_items){
            id_list.add(data_item.id);
        }
        return id_list.hashCode();
    }

    public Node(String name, int listening_port){
        this.self_remote_node = new RemoteNode();
        this.self_remote_node.name = name;
        this.self_remote_node.port = listening_port;
        this.network_listener = new NetworkListener(this,listening_port);
        this.network_listener.start();
        this.keyboard_listener.start();
    }
}
