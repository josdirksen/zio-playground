package example.model

/** For now we're only interested in a few details
  *
  * @param timestamp the timestamp the weather was gathered
  * @param temperature the temperature
  */
case class Temperature(timestamp: Long, temperature: Double)
