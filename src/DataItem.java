import java.io.Serializable;
import java.util.UUID;

public class DataItem implements Serializable,Comparable<DataItem> {
    public UUID id;
    public DataType type;
    public String data_s1;
    public String data_s2;
    public int data_i;
    public int time;

    //Constructor for transaction
    public DataItem(DataType type, String data_s1, String data_s2, int data_i, int time){
        this.id = UUID.randomUUID();
        this.type = type;
        this.data_s1 = data_s1;
        this.data_s2 = data_s2;
        this.data_i = data_i;
        this.time = time;
    }
    //Constructor for add/remove account
    public DataItem(DataType type, String data_s1, int time){
        this.id = UUID.randomUUID();
        this.type = type;
        this.data_s1 = data_s1;
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
            str = String.format(str, id.toString(), type.name(), data_s1, data_s2, data_i, time);
        }  
        else {
            str = "Id:%s, Type:%s, Account:%s, Time:%d";
            str = String.format(str, id.toString(), type.name(), data_s1, time);
        }
        return str;
    }
}
