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
package oerclient

// Java
import java.io.FileNotFoundException
import java.io.IOException
import java.math.BigDecimal
import java.net.MalformedURLException
import java.net.URL
import java.net.URLConnection
import java.util.Calendar

// Json
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper

// Joda 
import org.joda.money._
import org.joda.time._

// LRUCache
import com.twitter.util.LruMap

/**
 * Implements Json for Open Exchange Rates(http://openexchangerates.org)
 * @param apiKey - The API key to Open Exchange Rates
 */
class OerClient(config: ForexConfig, oerConfig: OerClientConfig) extends ForexClient(config) {

  private val oerUrl = "http://openexchangerates.org/api/"

  /**
   * Sets the base currency in the url
   */
  private val base   = if (oerConfig.configurableBase) "&base=" + config.baseCurrency 
                       else ""
  /**
   * The constant that will hold the URI for
   * a latest-exchange rate lookup from OER
   */
  private val latest = "latest.json?app_id=" + oerConfig.appId + base

  /**
   * The constant will hold the URI for a 
   * historical-exchange rate lookup from OER
   */
  private val historical = "historical/%04d-%02d-%02d.json?app_id=" + oerConfig.appId + base

  /**
   * Mapper for reading JSON objects 
   */
  private val mapper = new ObjectMapper()

  /**
  * The earliest date OER service is availble
  */
  private val oerDataFrom = new DateTime(1999,1,1,0,0)

  /**
   * Gets live currency value for the desired currency, 
   * silently drop the currency types which Joda money does not support.  
   * If cache exists, update nowishCache when an API request has been done,
   * else just return the forex rate
   * @parameter currency - The desired currency we want to look up from the API
   * @returns live exchange rate obtained from API
   */
  def getCurrencyValue(currency: CurrencyUnit): BigDecimal= {
    val key = (config.baseCurrency, currency) 
    val node = getJsonNodeFromAPI(latest)
    nowishCache match {
      case Some(cache) => {
            val currencyNameIterator = node.getFieldNames
            while (currencyNameIterator.hasNext) {  
              val currencyName = currencyNameIterator.next
              try {
                val keyPair   = (config.baseCurrency, CurrencyUnit.getInstance(currencyName))
                val valuePair = (DateTime.now, node.findValue(currencyName).getDecimalValue)                                                                       
                cache.put(keyPair, valuePair)
              } catch {
                case (e: IllegalCurrencyException) => // drop the illegal currencies
              }
            } 
      }
      case None => // do nothing   
    }
    node.findValue(currency.toString).getDecimalValue
  }

  /**
  * build the historical link for the URI according to the date
  * @parameter date - The historical date for the currency look up, 
  * which should be the same as date argument in the getHistoricalCurrencyValue method below
  * @returns the link in string format   
  */
  private def buildHistoricalLink(date: DateTime) : String = {
    val dateCal = date.toGregorianCalendar
    val day     = dateCal.get(Calendar.DAY_OF_MONTH)
    val month   = dateCal.get(Calendar.MONTH) + 1
    val year    = dateCal.get(Calendar.YEAR)
    historical.format(year, month, day)
  }
  /**
   * Gets historical forex rate for the given currency and date
   * return error message if the date is invalid 
   * silently drop the currency types which Joda money does not support
   * if cache exists, update the eodCache when an API request has been done,
   * else just return the look up result
   * @parameter currency - The desired currency we want to look up from the API
   * @parameter date - The specific date we want to look up on
   * @returns live exchange rate obtained from API if available, or error message if else
   */
  def getHistoricalCurrencyValue(currency: CurrencyUnit, date: DateTime): Either[String, BigDecimal] = {
    
    /**
    * return error message if the date given is not supported by OER
    */
    if (date.isBefore(oerDataFrom) || date.isAfter(DateTime.now)) {
      Left("Exchange rate unavailable on the date [%s]".format(date))
    } else {
      val historicalLink = buildHistoricalLink(date)
      val key = (config.baseCurrency, currency, date) 
      val node = getJsonNodeFromAPI(historicalLink)
      eodCache match {
        case Some(cache) => {
          val currencyNameIterator = node.getFieldNames 
          while (currencyNameIterator.hasNext) {  
            val currencyName = currencyNameIterator.next
            try {
              val keySet = (config.baseCurrency, CurrencyUnit.getInstance(currencyName), date)
              cache.put(keySet, node.findValue(currencyName).getDecimalValue)  
            } catch {
              case (e: IllegalCurrencyException) => // drop the illegal currencies
            }                                                         
          }
        }
        case None => // do nothing
      }
      Right(node.findValue(currency.toString).getDecimalValue)
    }
  }

  /**
   * Helper method which returns the node containing
   a list of currency and rate pair.
   * @parameter downloadPath - The URI link for the API request
   * @returns JSON node which contains currency information obtained from API
   */  
  private def getJsonNodeFromAPI(downloadPath: String): JsonNode = {
    val url  = new URL(oerUrl + downloadPath)
    val conn = url.openConnection
    
    val root = mapper.readTree(conn.getInputStream).getElements
    var resNode = root.next
    while (root.hasNext) {
      resNode = root.next
    }
    resNode
  }

}
