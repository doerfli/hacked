package li.doerf.hacked.db.daos;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import li.doerf.hacked.db.entities.Breach;

@Dao
public interface BreachDao {
    @Insert
    List<Long> insert(Breach... entities);

    @Update
    int update(Breach entity);

    @Delete
    int delete(Breach entity);

    @Query("SELECT COUNT(*) FROM breaches WHERE account=:account")
    Long countUnacknowledged(Long account);

    @Query("SELECT * FROM breaches WHERE _id=:id")
    Breach findById(Long id);

    @Query("SELECT * FROM breaches WHERE account=:accountId ORDER BY is_acknowledged, breach_date DESC")
    List<Breach> findByAccount(Long accountId);

    @Query("SELECT * FROM breaches WHERE account=:accountId ORDER BY is_acknowledged, breach_date DESC")
    LiveData<List<Breach>> findByAccountLD(Long accountId);

    @Query("SELECT * FROM breaches WHERE account=:accountId AND name=:aName ORDER BY name")
    Breach findByAccountAndName(Long accountId, String aName);
}
