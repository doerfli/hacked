package li.doerf.hacked.db.daos;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import li.doerf.hacked.db.entities.Account;

@Dao
public interface AccountDao {

    @Insert
    List<Long> insert(Account... entities);

    @Update
    int update(Account entity);

    @Delete
    int delete(Account entity);

    @Query("SELECT * FROM accounts")
    List<Account> getAll();

    @Query("SELECT * FROM accounts")
    LiveData<List<Account>> getAllLD();

    @Query("SELECT * FROM accounts where _id=:anId")
    Account findById(Long anId);

//    @Query("SELECT * FROM accounts where name=:aName")
//    Account findByName(String aName);

    @Query("SELECT count(*) FROM accounts where name=:aName")
    Integer countByName(String aName);

}
