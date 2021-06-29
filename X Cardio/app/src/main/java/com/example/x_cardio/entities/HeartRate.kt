package com.example.x_cardio.entities

    class HeartRate() {


        companion object {
            const val SAMPLE_INTERVAL_S: Double    = 40e-03
            const val RIGHT_RANGE: Double           = 3.1
            const val LEFT_RANGE: Double            = 2.5
            const val AMPLITUDE: Double             = 0.4
        }


        var rate = mutableListOf<Double>()
        var BPM = 0.0
        var IBI = 750e-03                           // 750ms per beat = 80 Beats Per Minute (BPM)
        var Pulse = false
        var sampleCounter = 0.0
        var lastBeatTime = 0.0
        var P = getMeanRange()                  // peak at 1/2 the input range of 2.5....3.1
        var T = getMeanRange()                  // trough at 1/2 the input range.
        var threshSetting = getMeanRange()      // used to seed and reset the thresh variable
        var thresh = 2.9             // threshold a little above the trough
        var amp = AMPLITUDE           // beat amplitude 1/10 of input range.
        var firstBeat = true                    // looking for the first beat
        var secondBeat = false                  // not yet looking for the second beat in a row
        var measurements = mutableListOf<Double>()



        private fun getMeanRange(): Double {

            return (RIGHT_RANGE + LEFT_RANGE)/2

        }

        fun addMeasurement(data: Double){

            processLastSample(data)

        }

        private fun processLastSample(sample: Double){

            sampleCounter += SAMPLE_INTERVAL_S                 // keep track of the time in mS with this variable
            var N = sampleCounter - lastBeatTime                // monitor the time since the last beat to avoid noise

            //  find the peak and trough of the pulse wave
            var signal = sample
            if (signal < thresh && N > (IBI / 5) * 3) {         // avoid dichrotic noise by waiting 3/5 of last IBI
                if (signal < T) {                               // T is the trough
                    T = signal                                 // keep track of lowest point in pulse wave
                }
            }

            if (signal > thresh && signal > P) {                // thresh condition helps avoid noise
                P = signal                                   // P is the peak
            }                                                   // keep track of highest point in pulse wave

            // LOOK FOR THE HEART BEAT
            // signal surges up in value every time there is a pulse
            if (N > 0.25) {                                                 // avoid high frequency noise
                if ((signal > thresh) && !Pulse && (N > (IBI / 5) * 3)) {
                    Pulse           =  true                               // set the Pulse flag when we think there is a pulse
                    IBI             = sampleCounter - lastBeatTime         // measure time between beats in mS
                    lastBeatTime    = sampleCounter;                        // keep track of time for next pulse

                    if (secondBeat) {                                       // if this is the second beat, if secondBeat == TRUE
                        secondBeat  = false                               // clear secondBeat flag
                        addAllRates(IBI)
                    }

                    if (firstBeat) {                                        // if it's the first time we found a beat, if firstBeat == TRUE
                        firstBeat = false                                  // clear firstBeat flag
                        secondBeat = true                                  // set the second beat flag
                        // IBI value is unreliable so discard it
                        return
                    }

                    shiftRate(IBI)

                }}

            if (signal < thresh && Pulse) {                         // when the values are going down, the beat is over
                Pulse = false                                               // reset the Pulse flag so we can do it again
                amp = P - T                                                 // get amplitude of the pulse wave
                thresh = amp / 2 + T                                        // set thresh at 50% of the amplitude
                P = thresh                                                  // reset these for next time
                T = thresh
            }

            if (N > 2.5) {                                                  // if 2.5 seconds go by without a beat
                thresh = threshSetting                                      // set thresh default
                P = getMeanRange()                                                     // set P default
                T = getMeanRange()                                                     // set T default
                lastBeatTime = sampleCounter                                 // bring the lastBeatTime up to date
                firstBeat = true                                            // set these to avoid noise
                secondBeat = false                                          // when we get the heartbeat back
                BPM = 0.0
                IBI = 0.6                                                   // 600ms per beat = 100 Beats Per Minute (BPM)
                Pulse = false
                amp = AMPLITUDE                                                  // beat amplitude 1/10 of input range.

            }



        }

        private fun addAllRates(ibi: Double) {

            // seed the running total to get a realisitic BPM at startup

            for( i in 0..9){

                rate.add(ibi)

            }

        }


        private fun shiftRate(ibi: Double) {


            // keep a running total of the last 10 IBI values
            var runningTotal = 0.0              // clear the runningTotal variable

            rate.removeAt(0)             // shift data in the rate array
            rate.add(ibi)                      // add the latest IBI to the rate array

            for( anIbi in rate ){
                runningTotal += anIbi          // add up the 9 oldest IBI values &  add the latest IBI to runningTotal
            }

            runningTotal /= 10                // average the last 10 IBI values
            BPM = 60 / runningTotal           // how many beats can fit into a minute
            measurements.clear()

        }

         fun calculateBPM(): Double {

             return if(BPM != Double.NaN)
                 BPM
             else
                 0.0


        }





        }


