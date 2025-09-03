import java.io.Serializable;
import java.util.UUID;

public class DataItem implements Serializable,Comparable<DataItem> {
    public UUID id = UUID.randomUUID();
    public DataType type;
    public Account account1;
    public Account account2;
    public int amount;
    public int time;

    //Constructor for transaction
    public DataItem(DataType type, Account account1, Account account2, int amount, int time){
        this.type = type;
        this.account1 = account1;
        this.account2 = account2;
        this.amount = amount;
        this.time = time;
    }
    //Constructor for add/remove account
    public DataItem(DataType type, Account account1, int time){
        this.type = type;
        this.account1 = account1;
        this.time = time;
    }

    @Override
    public int compareTo(DataItem data_item2) {
        if (this.time > data_item2.time){
            return 1;
        }
        else if (this.time < data_item2.time){
            return -1;
        }
        else {
            return this.id.compareTo(data_item2.id);//if lamport clock fails use id comparison for consistency across network
        }
    }

    @Override
    public String toString(){
        String str;
        if (type == DataType.TRANSACTION){
            str = "Id:%s, Type:%s, Payer:%s, Payee:%s, Amount:%d, Time:%d";
            str = String.format(str, id.toString(), type.name(), account1.id.toString(), account2.id.toString(), amount, time);
        }  
        else {
            str = "Id:%s, Type:%s, Account:%s, Time:%d";
            str = String.format(str, id.toString(), type.name(), account1.id.toString(), time);
        }
        return str;
    }
}
