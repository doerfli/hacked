package li.doerf.hacked.db.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import li.doerf.hacked.db.entities.BreachedSite;

@Dao
public interface BreachedSiteDao {

    @Insert
    List<Long> insert(BreachedSite... entities);

    @Update
    int update(BreachedSite entity);

    @Delete
    int delete(BreachedSite... entity);

    @Query("SELECT * FROM breached_sites WHERE name=:name LIMIT 1")
    BreachedSite getByName(String name);

    @Query("SELECT * FROM breached_sites ORDER BY name")
    List<BreachedSite> getAll();

    @Query("SELECT * FROM breached_sites ORDER BY name")
    LiveData<List<BreachedSite>> getAllLD();

    @Query("SELECT * FROM breached_sites WHERE name LIKE :name ORDER BY name")
    LiveData<List<BreachedSite>> getAllByName(String name);

    @Query("SELECT * FROM breached_sites ORDER BY pwn_count DESC LIMIT 20")
    LiveData<List<BreachedSite>> listTop20();

    @Query("SELECT * FROM breached_sites ORDER BY added_date DESC LIMIT 3")
    LiveData<List<BreachedSite>> listMostRecent();
}
