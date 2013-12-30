/* 
 * Copyright (c) 2013-2014 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
 
package com.snowplowanalytics.forex

// Java
import java.math.BigDecimal
import java.math.RoundingMode
// Scala
import scala.collection.JavaConversions._
// Specs2
import org.specs2.mutable.Specification
// Joda time
import org.joda.money._

/**
*  testing for unsupported currencies in joda money, e.g. bitcoin(BTC)
*/
class UnsupportedCurrencySpecification extends Specification { 
  val fx  = TestHelper.fx 
  
  // must _== Left(msg)
  
  "a BTC lookup" should {
  	"throw IllegalCurrencyException" in {
 		 def btcException: Either[String, Money] = {
 		 		fx.rate(CurrencyUnit.getInstance("BTC")).to(CurrencyUnit.GBP).now
 		 	}
  	 		btcException must throwA[IllegalCurrencyException]
  	 }
  }

  "a EEK lookup" should {
	"throw IllegalCurrencyException" in {
		 def btcException: Either[String, Money] = {
		 		fx.rate(CurrencyUnit.getInstance("EEK")).to(CurrencyUnit.GBP).now
		 	}
	 		btcException must throwA[IllegalCurrencyException]
	 }
  }

}