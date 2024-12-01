import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class KeyboardListener extends Thread {

    private Node node = null;

    public KeyboardListener(Node n) {
        node = n;
    }

    public void run() {
        System.out.println("Commands: add, remove, transaction, connect, disconnect, integrity, balances, nodes, dataitems, robot");
        Scanner sc = new Scanner(System.in);
        while (true) {
            try {
                String line = sc.nextLine();
                parseLine(line);
            } catch (NoSuchElementException e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    public void parseLine(String line) {
        String[] cmd = line.split(" ");
        if (cmd[0].equalsIgnoreCase("add")) {
            if (cmd.length != 3)
                System.out.println("Usage: add accountname [0,1,2 for regular/student/savings]");
            else {
                try {
                    AccountType account_type = AccountType.values()[Integer.parseInt(cmd[2])];
                    node.addAccountToNetwork(cmd[1],account_type);
                }
                catch(Exception e){
                    System.out.println("Wrong format");
                }
                
            }
        } else if(cmd[0].equalsIgnoreCase("remove")){
            if (cmd.length != 2)
                System.out.println("Usage: remove [account id]");
            else {
                node.removeAccountFromNetwork(cmd[1]);
            }
        }
        else if(cmd[0].equalsIgnoreCase("transaction")){
            if (cmd.length != 4)
                System.out.println("Usage: transaction [payer account id] [payee account id] [amount]");
            else {
                try {
                    int amount = Integer.parseInt(cmd[3]);
                    if (amount <= 0){
                        System.out.println("Amount needs to be a positive integer");
                    }
                    else {
                        node.addTransactionToNetwork(cmd[1], cmd[2], amount);
                    }
                }
                catch (NumberFormatException e){
                    System.out.println("Amount needs to be a positive integer");
                }
                
                
            }
        } 
        else if (cmd[0].equalsIgnoreCase("connect")) {
            if (cmd.length != 3)
                System.out.println("Usage: connect remoteip remoteport");
            else {
                int port = 0;
                try {
                    port = Integer.parseInt(cmd[2]);
                    node.connectToNetwork(InetAddress.getByName(cmd[1]), port);
                }
                catch (NumberFormatException | UnknownHostException e){
                    System.out.println("Could not connect to network");
                    e.printStackTrace();
                }
            }
        } 
        else if (cmd[0].equalsIgnoreCase("disconnect")) {
            if (cmd.length != 1)
                System.out.println("Usage: disconnect");
            else {
                node.disconnectFromNetwork();
            }
        } else if (cmd[0].equalsIgnoreCase("integrity")) {
            if (cmd.length != 1)
                System.out.println("Usage: integrity");
            else {
                node.checkNetworkIntegrity();
            }
        } else if (cmd[0].equalsIgnoreCase("balances")) {
            if (cmd.length != 1)
                System.out.println("Usage: balances");
            else {
                for (Map.Entry<Account, Integer> entry : node.getAccountsBalances().entrySet()) {
                    System.out.println(String.format("%s balance:%s",entry.getKey().toString(), entry.getValue()));
                }
            }
        }else if (cmd[0].equalsIgnoreCase("nodes")) {
            if (cmd.length != 1)
                System.out.println("Usage: nodes");
            else {
                System.out.println("This node: "+node.self_remote_node.toString());
                System.out.println("Other nodes: "+node.remote_nodes.toString());
            }
        }else if (cmd[0].equalsIgnoreCase("dataitems")) {
            if (cmd.length != 1 && cmd.length != 2)
                System.out.println("Usage: dataitems [optional index]");
            else if (cmd.length == 1) {
                for (DataItem data_item : node.data_items){
                    System.out.println(data_item.toString());
                }
            }
            else if (cmd.length == 2){
                try {
                    int index = Integer.parseInt(cmd[1]);
                    System.out.println(String.format("Dataitem %d/%d",index+1,node.data_items.size()));
                    System.out.println(node.data_items.get(index));
                }catch(IndexOutOfBoundsException | NumberFormatException e){
                    System.out.println("Wrong format or index out of bounds");
                }
            }
        }else if (cmd[0].equalsIgnoreCase("clear")) {
            if (cmd.length != 1)
                System.out.println("Usage: clear");
            else {
                node.clearDataitems();
                System.out.println("Dataitems cleared");
            }
        }else if (cmd[0].equalsIgnoreCase("hash")) {
            if (cmd.length != 1)
                System.out.println("Usage: hash");
            else {
                System.out.println("Dataitems Hash: "+ node.getDataItemsHashCode());
            }
        }else if (cmd[0].equalsIgnoreCase("robot")) {
            if (cmd.length != 1)
                System.out.println("Usage: robot");
            else {
                while(checkForKeyPress()){
                    node.addRandomDataitem();
                }
            }
        }else {
            System.out.println("Unknown Command");
        }
    }
    private boolean checkForKeyPress(){
        try {
            return (System.in.available()==0);
        } catch (Exception e) {
            return true;
        }
    }
}
