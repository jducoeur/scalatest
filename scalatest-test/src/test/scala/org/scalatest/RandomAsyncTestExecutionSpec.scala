/*
 * Copyright 2001-2015 Artima, Inc.
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
package org.scalatest

import scala.collection.mutable.ListBuffer
import org.scalatest.SharedHelpers.SilentReporter
import org.scalatest.SharedHelpers.EventRecordingReporter

class RandomAsyncTestExecutionSpec extends AsyncFunSuite with Safety /* with RandomTestOrder*/ { thisOuterSuite =>
  
  // SKIP-SCALATESTJS-START
  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
  // SKIP-SCALATESTJS-END
  //SCALATESTJS-ONLY implicit val executionContext = scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  private val buf = ListBuffer.empty[Int]

  class ExampleSpec extends AsyncFunSuite with RandomTestOrder {
    // SKIP-SCALATESTJS-START
    implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY implicit val executionContext = scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

    test("test one") { thisOuterSuite.synchronized { buf += 1 } }
    test("test two") { thisOuterSuite.synchronized { buf += 2 } }
    test("test three") { thisOuterSuite.synchronized { buf += 3 } }
    test("test four") { thisOuterSuite.synchronized { buf += 4 } }
    test("test five") { thisOuterSuite.synchronized { buf += 5 } }
    test("test six") { thisOuterSuite.synchronized { buf += 6 } }
    test("test seven") { thisOuterSuite.synchronized { buf += 7 } }

    override def newInstance = new ExampleSpec
  }

  test("Ensure that mixing RandomTestOrder into an AsyncSuite actually randomizes the test order") {
    val a = new ExampleSpec
    val status = a.run(None, Args(SilentReporter, Stopper.default, Filter(), ConfigMap.empty, None, new Tracker, Set.empty))
    status.toFuture.map { _ =>
      val list = thisOuterSuite.synchronized { buf.toList }
      assert(list != list.sorted)
    }
  }
}

