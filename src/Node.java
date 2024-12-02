import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
    public List<DataItem> data_items = new CopyOnWriteArrayList<DataItem>();
    public List<RemoteNode> remote_nodes = new ArrayList<RemoteNode>();
    private KeyboardListener keyboard_listener = new KeyboardListener(this);
    private NetworkListener network_listener;
    private DiscoveryListener discovery_listener;
    private HashMap<RemoteNode, Integer> integrity_hashes = new HashMap<RemoteNode, Integer>();
    private boolean waiting = false;
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

    public Set<Account> getAccounts() {
        Set<Account> accounts = getAccountsBalances().keySet();
        return accounts;
    }

    public HashMap<Account, Integer> getAccountsBalances() {
        sortDataItems();
        HashMap<Account, Integer> account_balance = new HashMap<Account, Integer>();
        Account EXTERNAL = new Account(UUID.nameUUIDFromBytes("EXTERNAL".getBytes()));
        account_balance.put(EXTERNAL,Integer.MAX_VALUE); //pseudo-account from which an account can receive funds

        for(DataItem data_item : data_items){
            Account payer = data_item.account1; //or account to be added/deleted
            Account payee = data_item.account2;
            int amount = data_item.amount;

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
            if (payer != EXTERNAL){
                account_balance.put(payer, account_balance.get(payer) - amount);
            }
            if (payee != EXTERNAL){
                account_balance.put(payee, account_balance.get(payee) + amount);
            }
        }
        account_balance.remove(EXTERNAL);
        return account_balance;
    }

    public void sortDataItems(){
        Collections.sort(data_items);
    }

    public void addAccountToNetwork(String account_name, AccountType account_type){
        time += 1;
        Account account = new Account(account_name, account_type);
        DataItem data_item = new DataItem(DataType.ADD_ACCOUNT,account,time);
        data_items.add(data_item);
        Message message = new Message(MessageType.DATAITEM, self_remote_node, data_item);
        sendMessageToNetwork(message);
        time += 1;
    }

    public void removeAccountFromNetwork(String id){         
        if(id.equals("EXTERNAL")){
            System.out.println("Account id cannot be EXTERNAL");
            return;
        }   
        time += 1;
        Account account = new Account(UUID.fromString(id));
        DataItem data_item = new DataItem(DataType.REMOVE_ACCOUNT, account, time);
        data_items.add(data_item);
        Message message = new Message(MessageType.DATAITEM, self_remote_node, data_item);
        sendMessageToNetwork(message);
        time += 1;
    }

    public void addTransactionToNetwork(String payer_id, String payee_id, int amount){
        Account payer;
        Account payee;
        try{
            if (payer_id.equals("EXTERNAL")){
                payer = new Account(UUID.nameUUIDFromBytes(payer_id.getBytes()));
            }
            else {
                payer = new Account(UUID.fromString(payer_id));
            }
            if (payee_id.equals("EXTERNAL")){
                payee = new Account(UUID.nameUUIDFromBytes(payee_id.getBytes()));
            }
            else {
                payee = new Account(UUID.fromString(payee_id));
            }
        }
        catch(Exception e){
            System.out.println("Invalid data");
            return;
        }
        time += 1;
        DataItem data_item = new DataItem(DataType.TRANSACTION, payer, payee, amount, time);
        data_items.add(data_item);
        Message message = new Message(MessageType.DATAITEM, self_remote_node, data_item);
        sendMessageToNetwork(message);
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
        remote_nodes.clear();
        System.out.println("Disconnected");
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

    public void discover(){
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        Message message = new Message(MessageType.DISCOVER, self_remote_node);
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
                InetAddress group = InetAddress.getByName("224.0.0.0");
                DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, group, 4999);
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
                    //System.out.println("Dataitem received:");
                    //System.out.println(message.data_item.toString());
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
                    break;
                case DISCOVER:
                    if (message.sending_node.ip != null){
                        System.out.println("Discover request");
                        Message message2 = new Message(MessageType.DISCOVER_RESPOND, self_remote_node);
                        sendMessageToNode(message2, message.sending_node.ip, message.sending_node.port);
                    }
                    break;
                case DISCOVER_RESPOND:
                    System.out.println(String.format("Response from: %s",message.sending_node.toString()));
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
                        waiting = false;
                        System.out.println("All hashes received, starting check");
                        integrityCheckFinalize();
                    }
                    break;
                case CHECK_INTEGRITY:
                    System.out.println("Check integrity received");
                    Thread waitThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            checkNetworkIntegrity();
                        }
                    });  
                    waitThread.start();
                    break;
            }
        }
        catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
        }
    }

    public void checkNetworkIntegrity(){
        if (remote_nodes.size() == 0){
            System.out.println("Not connected to a network");
            return;
        }
        Message message = new Message(MessageType.DATAITEMS_HASH_REQUEST, self_remote_node);
        sendMessageToNetwork(message);
        System.out.println("Waiting for response.");
        this.waiting = true;
        try{
            TimeUnit.SECONDS.sleep((long)3);
        }
        catch(InterruptedException e){
            return;
        }
        //Some nodes not responding, remove them from the network
        if (this.waiting == true){
            ArrayList<RemoteNode> nodes_received = new ArrayList<RemoteNode>(integrity_hashes.keySet());
            List<RemoteNode>  non_responsive_nodes = new ArrayList<RemoteNode>(remote_nodes);
            non_responsive_nodes.removeAll(nodes_received);
            System.out.println("Nodes not responding:");
            System.out.println(non_responsive_nodes.toString());
            remote_nodes.removeAll(non_responsive_nodes);
            for (RemoteNode non_responsive_node : non_responsive_nodes){
                Message message2 = new Message(MessageType.REMOVE_NODE, this.self_remote_node, non_responsive_node);
                sendMessageToNetwork(message2);
            }
        }
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
                    try {
                        TimeUnit.SECONDS.sleep((long)1);
                    } catch (Exception e) {
                    }
                    
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
        integrity_hashes.clear();
    }

    public void clearDataitems(){
        time = 0;
        data_items.clear();
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

    public void addRandomDataitem(){
        Random r = new Random();
        int randomNum = r.nextInt(10);
        if(randomNum > 8 && getAccounts().size() > 0){
            ArrayList<Account> accounts = new ArrayList<Account>(getAccounts());
            String randomAccountId = accounts.get(r.nextInt(accounts.size())).id.toString();
            removeAccountFromNetwork(randomAccountId);
        }
        else if (randomNum > 3 && getAccounts().size() > 0){
            ArrayList<Account> accounts = new ArrayList<Account>(getAccounts());
            List<String> id_list = accounts.stream().map((account) -> account.id.toString()).collect(Collectors.toList());
            id_list.add("EXTERNAL");
            String randomPayerId = id_list.get(r.nextInt(id_list.size()));
            String randomPayeeId = id_list.get(r.nextInt(id_list.size()));
            addTransactionToNetwork(randomPayerId, randomPayeeId, r.nextInt(9999)+1);
        }
        else if (getAccounts().size() < 20){
            char c = (char)(r.nextInt(26) + 'a');
            addAccountToNetwork(Character.toString(c),AccountType.values()[r.nextInt(3)]);
        }
    }

    public Node(String name, int listening_port){
        this.self_remote_node = new RemoteNode();
        this.self_remote_node.name = name;
        this.self_remote_node.port = listening_port;
        this.network_listener = new NetworkListener(this,listening_port);
        this.discovery_listener = new DiscoveryListener(this);
        this.network_listener.start();
        this.discovery_listener.start();
        this.keyboard_listener.start();
    }
}
