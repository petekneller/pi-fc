package fc.device

import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval

package object rc {

  type PpmValue = Int Refined Interval.Closed[W.`0`.T, W.`2000`.T]

}
