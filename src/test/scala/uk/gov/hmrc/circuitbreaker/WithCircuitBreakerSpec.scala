/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.circuitbreaker

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.Future


class WithCircuitBreakerSpec extends WordSpecLike with Matchers with Eventually with ScalaFutures {

  private def returnOk = Future.successful(true)

  "WithCircuitBreaker" should {
    "return the function result when no exception is thrown" in new UsingCircuitBreaker {
      lazy override val circuitBreakerName = "someServiceCircuitBreaker"
      lazy override val numberOfCallsToTriggerStateChange: Option[Int] = None
      lazy override val unhealthyServiceUnavailableDuration: Option[Long] = None
      lazy override val turbulencePeriodDuration: Option[Long] = None

      whenReady(withCircuitBreaker[Boolean](returnOk)) {
        actualResult =>
          actualResult shouldBe true
      }
    }

    def throwException = throw new Exception("some exception")

    "return a circuit breaker exception when the function throws an exception" in new UsingCircuitBreaker {
      lazy override val circuitBreakerName = "test_2"
      lazy override val numberOfCallsToTriggerStateChange: Option[Int] = None
      lazy override val unhealthyServiceUnavailableDuration: Option[Long] = None
      lazy override val turbulencePeriodDuration: Option[Long] = None

      intercept[Exception] {
        withCircuitBreaker[Boolean](throwException)
      }

      intercept[Exception] {
        withCircuitBreaker[Boolean](throwException)
      }

      intercept[Exception] {
        withCircuitBreaker[Boolean](throwException)
      }

      Repository.circuitBreaker(circuitBreakerName).currentState.name shouldBe "HEALTHY"

      intercept[Exception] {
        withCircuitBreaker[Boolean](throwException)
      }

      Repository.circuitBreaker(circuitBreakerName).currentState.name shouldBe "UNHEALTHY"

      intercept[UnhealthyServiceException] {
        withCircuitBreaker[Boolean](throwException)
      }.getMessage shouldBe "test_2"

      Repository.circuitBreaker(circuitBreakerName).currentState.name shouldBe "UNHEALTHY"
    }

    "return a false canServiceBeInvoked when in an unhealthy state" in new UsingCircuitBreaker {
      lazy override val circuitBreakerName = "test_3"
      lazy override val numberOfCallsToTriggerStateChange: Option[Int] = Some(1)
      lazy override val unhealthyServiceUnavailableDuration: Option[Long] = Some(20)
      lazy override val turbulencePeriodDuration: Option[Long] = None

      intercept[Exception] {
        withCircuitBreaker[Boolean](throwException)
      }

      Repository.circuitBreaker(circuitBreakerName).currentState.name shouldBe "UNHEALTHY"
      canServiceBeInvoked shouldBe false

    }

    "return a true canServiceBeInvoked when in a healthy state" in new UsingCircuitBreaker {
      lazy override val circuitBreakerName = "test_4"
      lazy override val numberOfCallsToTriggerStateChange: Option[Int] = None
      lazy override val unhealthyServiceUnavailableDuration: Option[Long] = None
      lazy override val turbulencePeriodDuration: Option[Long] = None

      Repository.circuitBreaker(circuitBreakerName).currentState.name shouldBe "HEALTHY"
      canServiceBeInvoked shouldBe true
    }
  }
}
