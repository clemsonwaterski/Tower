package org.droidplanner.android.fragments.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent
import com.o3dr.services.android.lib.drone.attribute.AttributeType
import com.o3dr.services.android.lib.drone.property.Attitude
import com.o3dr.services.android.lib.drone.property.Gps
import com.o3dr.services.android.lib.drone.property.Speed
import org.droidplanner.android.R
import org.droidplanner.android.fragments.helpers.ApiListenerFragment
import org.droidplanner.android.view.AttitudeIndicator
import java.util.*

/**
 * Created by Fredia Huya-Kouadio on 8/27/15.
 */
public class MiniWidgetTelemetryInfo : TowerWidget() {

    companion object {
        private val FLIGHT_TIMER_PERIOD = 1000L // 1 second

        private val filter = initFilter()

        private fun initFilter(): IntentFilter {
            val temp = IntentFilter()
            temp.addAction(AttributeEvent.ATTITUDE_UPDATED)
            temp.addAction(AttributeEvent.SPEED_UPDATED)
            temp.addAction(AttributeEvent.STATE_UPDATED)
		    temp.addAction(AttributeEvent.GPS_POSITION);
            temp.addAction(AttributeEvent.HOME_UPDATED);
            return temp
        }
    }

    private val receiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action){
                AttributeEvent.ATTITUDE_UPDATED -> onOrientationUpdate()
                AttributeEvent.SPEED_UPDATED -> onSpeedUpdate()
                AttributeEvent.STATE_UPDATED -> updateFlightTimer()
                AttributeEvent.GPS_POSITION ->  onPositionUpdate()
                AttributeEvent.HOME_UPDATED ->  onPositionUpdate()
            }
        }
    }

    private val flightTimeUpdater = object : Runnable{
        override fun run(){
            handler.removeCallbacks(this)
            val drone = drone
            if(!drone.isConnected())
                return

            val timeInSecs = drone.flightTime
            val mins = timeInSecs / 60L
            val secs = timeInSecs % 60L
            flightTimer?.text = java.lang.String.format("%02d:%02d", mins, secs)

            handler.postDelayed(this, FLIGHT_TIMER_PERIOD)
        }
    }

    private val handler = Handler()

    private var attitudeIndicator: AttitudeIndicator? = null
    private var roll: TextView? = null
    private var yaw: TextView? = null
    private var pitch: TextView? = null

    private var horizontalSpeed: TextView? = null
    private var verticalSpeed: TextView? = null

    private var latitude: TextView? = null
    private var longitude: TextView? = null

    private var headingModeFPV: Boolean = false

    private var flightTimer : TextView? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View?{
        return inflater?.inflate(R.layout.fragment_mini_widget_telemetry_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)

        attitudeIndicator = view.findViewById(R.id.aiView) as AttitudeIndicator?

        roll = view.findViewById(R.id.rollValueText) as TextView?
        yaw = view.findViewById(R.id.yawValueText) as TextView?
        pitch = view.findViewById(R.id.pitchValueText) as TextView?

        horizontalSpeed = view.findViewById(R.id.horizontal_speed_telem) as TextView?
        verticalSpeed = view.findViewById(R.id.vertical_speed_telem) as TextView?

        flightTimer = view.findViewById(R.id.flight_timer) as TextView?

        latitude = view.findViewById(R.id.latitude_telem) as TextView?
        longitude = view.findViewById(R.id.longitude_telem) as TextView?
    }

    override fun onStart() {
        super.onStart()

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        headingModeFPV = prefs.getBoolean("pref_heading_mode", false)
    }

    override fun getWidgetType() = TowerWidgets.TELEMETRY_INFO

    override fun onApiConnected() {
        updateAllTelem()
        broadcastManager.registerReceiver(receiver, filter)
    }

    override fun onApiDisconnected() {
        broadcastManager.unregisterReceiver(receiver)
    }

    private fun updateAllTelem() {
        onOrientationUpdate()
        onSpeedUpdate()
        onPositionUpdate()
        updateFlightTimer()
    }

    private fun updateFlightTimer(){
        handler.removeCallbacks(flightTimeUpdater)
        if(drone.isConnected)
            flightTimeUpdater.run()
        else
            flightTimer?.text = "00:00"
    }

    private fun onOrientationUpdate() {
        if(!isAdded)
            return

        val drone = drone

        val attitude = drone.getAttribute<Attitude>(AttributeType.ATTITUDE) ?: return

        val r = attitude.roll.toFloat()
        val p = attitude.pitch.toFloat()
        var y = attitude.yaw.toFloat()

        if (!headingModeFPV and (y < 0)) {
            y += 360
        }

        attitudeIndicator?.setAttitude(r, p, y)

        roll?.text = java.lang.String.format(Locale.US,"%3.0f\u00B0", r)
        pitch?.text = java.lang.String.format(Locale.US,"%3.0f\u00B0", p)
        yaw?.text = java.lang.String.format(Locale.US, "%3.0f\u00B0", y)

    }

    private fun onSpeedUpdate() {
        if(!isAdded)
            return

        val drone = drone
        val speed = drone.getAttribute<Speed>(AttributeType.SPEED) ?: return

        val groundSpeedValue =  speed.groundSpeed
        val verticalSpeedValue = speed.verticalSpeed

        val speedUnitProvider = speedUnitProvider

        horizontalSpeed?.text = getString(R.string.horizontal_speed_telem, speedUnitProvider.boxBaseValueToTarget(groundSpeedValue).toString())
        verticalSpeed?.text = getString(R.string.vertical_speed_telem, speedUnitProvider.boxBaseValueToTarget(verticalSpeedValue).toString())
    }

	private fun onPositionUpdate() {
        if (!isAdded)
            return

        val drone = drone
        val droneGps = drone.getAttribute<Gps>(AttributeType.GPS) ?: return

        if (droneGps.isValid) {

            val latitudeValue = droneGps.position.latitude
            val longitudeValue = droneGps.position.longitude

            latitude?.text = getString(R.string.latitude_telem, Location.convert(latitudeValue, Location.FORMAT_DEGREES).toString())
            longitude?.text = getString(R.string.longitude_telem, Location.convert(longitudeValue, Location.FORMAT_DEGREES).toString())

        }
    }
}