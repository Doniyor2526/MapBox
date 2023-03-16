package uz.d_nasimov.mapbox.dao

import androidx.room.Dao
import androidx.room.Insert
import uz.d_nasimov.mapbox.entity.Location

@Dao
interface LocationDao {
    @Insert
    fun addLocation(location: Location)
}