package uz.d_nasimov.mapbox.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Location {

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var time: Long = 0

    constructor(latitude: Double, longitude: Double, time: Long) {
        this.latitude = latitude
        this.longitude = longitude
        this.time = time
    }

    constructor()
}