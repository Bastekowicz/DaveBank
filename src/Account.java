import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class Account implements Serializable{
    UUID id;
    String name;
    AccountType account_type;

    public Account(String name,AccountType account_type){
        this.id = UUID.randomUUID();
        this.name = name;
        this.account_type = account_type;
    }
    public Account(UUID id){
        this.id = id;
    }

    @Override
    public String toString(){
        if (this.name == null || this.account_type == null){
            return "id:"+id.toString();
        }
        String str = String.format("id:%s name:%s type:%s",id.toString(),name,account_type.toString());
        return str;
    }

    @Override
    public boolean equals(Object object){
        if (! (object instanceof Account))
            return false;
        Account that = (Account)object;
        return this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
