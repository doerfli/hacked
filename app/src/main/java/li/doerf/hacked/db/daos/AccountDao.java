package li.doerf.hacked.db.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import li.doerf.hacked.db.entities.Account;

@Dao
public interface AccountDao {

    @Insert
    List<Long> insert(Account... entities);

    @Update
    void update(Account entity);

    @Delete
    int delete(Account entity);

    @Query("SELECT * FROM accounts")
    List<Account> getAll();

    @Query("SELECT * FROM accounts ORDER BY is_hacked DESC, name")
    LiveData<List<Account>> getAllLD();

    @Query("SELECT * FROM accounts where _id=:anId")
    List<Account> findById(Long anId);

    @Query("SELECT * FROM accounts where name=:name")
    List<Account> findByName(String name);

    @Query("SELECT count(*) FROM accounts where name=:aName")
    Integer countByName(String aName);

    @Query("SELECT * FROM accounts ORDER BY last_checked DESC LIMIT 1")
    LiveData<Account> getLastChecked();
}
